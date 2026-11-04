package com.votasegura.api.service;

import com.votasegura.api.dto.request.CandidatoItemRequest;
import com.votasegura.api.dto.request.CrearEleccionRequest;
import com.votasegura.api.dto.request.EmitirVotoRequest;
import com.votasegura.api.dto.response.CandidatoResponse;
import com.votasegura.api.dto.response.EleccionResponse;
import com.votasegura.api.dto.response.ResultadosEleccionResponse;
import com.votasegura.api.dto.response.VotoResponse;
import com.votasegura.api.exception.ApiException;
import com.votasegura.api.model.*;
import com.votasegura.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EleccionService {

    private final EleccionRepository eleccionRepository;
    private final CandidatoRepository candidatoRepository;
    private final GrupoRepository grupoRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final RegistroVotoRepository registroVotoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComprobanteVotoEmailService comprobanteVotoEmailService;

    // Zona Horaria MÃƒÂ©xico (Sur / Centro)
    public static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");

    private static final String CARACTERES_RECIBO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Tarea en segundo plano automÃƒÂ¡tica que se ejecuta cada 10 segundos
     * para cerrar elecciones expiradas o activar elecciones programadas automÃƒÂ¡ticamente en la BD.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void verificarYEyecutarCierreAutomatico() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_MEXICO);

        // 1. Cerrar elecciones que alcanzaron su fecha fin
        List<Eleccion> expiradas = eleccionRepository.findByEstadoAndFechaFinBefore("activa", ahora);
        for (Eleccion e : expiradas) {
            e.setEstado("cerrada");
            eleccionRepository.save(e);
            log.info("Ã¢ÂÂ° [RELOJ AUTOMÃƒÂTICO] ElecciÃƒÂ³n '{}' (id: {}) cerrada automÃƒÂ¡ticamente al llegar fecha fin ({})",
                    e.getTitulo(), e.getId(), e.getFechaFin());
        }

        // 2. Activar elecciones que alcanzaron su fecha inicio
        List<Eleccion> porIniciar = eleccionRepository.findByEstadoAndFechaInicioBefore("programada", ahora);
        for (Eleccion e : porIniciar) {
            if (e.getFechaFin() == null || ahora.isBefore(e.getFechaFin())) {
                e.setEstado("activa");
                eleccionRepository.save(e);
                log.info("Ã¢ÂÂ° [RELOJ AUTOMÃƒÂTICO] ElecciÃƒÂ³n '{}' (id: {}) activada automÃƒÂ¡ticamente al llegar fecha inicio ({})",
                        e.getTitulo(), e.getId(), e.getFechaInicio());
            }
        }
    }

    @Transactional
    public EleccionResponse crearEleccion(String grupoId, String correoUsuario, CrearEleccionRequest req) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Grupo grupo = obtenerGrupo(grupoId);

        validarAdminGrupo(grupo.getId(), usuario);

        LocalDateTime ahora = LocalDateTime.now(ZONA_MEXICO);
        LocalDateTime inicio = parsearFecha(req.getFechaInicio(), ahora);
        LocalDateTime fin = parsearFecha(req.getFechaFin(), ahora.plusDays(1));

        if (fin.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha de fin no puede ser anterior a la fecha de inicio.", "FECHA_INVALIDA");
        }

        String estadoInicial = ahora.isBefore(inicio) ? "programada" : "activa";

        Eleccion eleccion = Eleccion.builder()
                .grupo(grupo)
                .titulo(req.getTitulo().trim())
                .descripcion(req.getDescripcion())
                .fechaInicio(inicio)
                .fechaFin(fin)
                .estado(estadoInicial)
                .build();

        eleccion = eleccionRepository.save(eleccion);

        if (req.getCandidatos() != null && !req.getCandidatos().isEmpty()) {
            final Eleccion eleccionFinal = eleccion;
            List<Candidato> listaCandidatos = req.getCandidatos().stream().map(c ->
                    Candidato.builder()
                            .eleccion(eleccionFinal)
                            .nombre(c.getNombre().trim())
                            .puesto(c.getPuesto() != null ? c.getPuesto().trim() : "Candidato/a")
                            .descripcion(c.getDescripcion())
                            .avatarUrl(c.getAvatarUrl())
                            .votos(0L)
                            .build()
            ).collect(Collectors.toList());

            candidatoRepository.saveAll(listaCandidatos);
            eleccion.setCandidatos(listaCandidatos);
        }

        log.info("ElecciÃƒÂ³n '{}' creada en grupo '{}' [Zona: {}, Inicio: {}, Fin: {}, Estado: {}]",
                eleccion.getTitulo(), grupo.getNombre(), ZONA_MEXICO, inicio, fin, estadoInicial);

        return mapEleccionResponse(eleccion, usuario.getId(), grupo);
    }

    @Transactional
    public List<EleccionResponse> listarEleccionesDeGrupo(String grupoId, String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Grupo grupo = obtenerGrupo(grupoId);

        validarMiembroGrupo(grupo.getId(), usuario.getId());

        List<Eleccion> elecciones = eleccionRepository.findByGrupoIdOrderByCreadoEnDesc(grupoId);
        elecciones.forEach(this::actualizarEstadoTemporal);

        return elecciones.stream()
                .map(e -> mapEleccionResponse(e, usuario.getId(), grupo))
                .collect(Collectors.toList());
    }

    @Transactional
    public EleccionResponse obtenerEleccion(String eleccionId, String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

                validarMiembroGrupo(eleccion.getGrupo().getId(), usuario.getId());

        if ("inactivo".equalsIgnoreCase(usuario.getEstado()) || "desactivado".equalsIgnoreCase(usuario.getEstado()) || "bloqueado".equalsIgnoreCase(usuario.getEstado())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta se encuentra inactiva. No puedes participar en votaciones.", "CUENTA_INACTIVA");
        }
        actualizarEstadoTemporal(eleccion);

        return mapEleccionResponse(eleccion, usuario.getId(), eleccion.getGrupo());
    }

    @Transactional
    public EleccionResponse cambiarEstado(String eleccionId, String correoUsuario, String nuevoEstado) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

        validarAdminGrupo(eleccion.getGrupo().getId(), usuario);

        String estadoNormalizado = nuevoEstado.toLowerCase().trim();
        if ("cerrada".equals(estadoNormalizado)) {
            LocalDateTime ahora = LocalDateTime.now(ZONA_MEXICO);
            if (eleccion.getFechaFin() != null && ahora.isBefore(eleccion.getFechaFin())) {
                long votesCast = registroVotoRepository.countByEleccionId(eleccion.getId());
                long totalMiembros = miembroGrupoRepository.countActivosByGrupoId(eleccion.getGrupo().getId());
                long votantesElegibles = Math.max(0, totalMiembros - 1);
                if (votantesElegibles > 0 && votesCast < votantesElegibles) {
                    long pendientes = votantesElegibles - votesCast;
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "No se puede cerrar la elecciÃ³n antes de tiempo porque aÃºn faltan miembros por votar (" + votesCast + "/" + votantesElegibles + ").",
                            "VOTACION_INCOMPLETA");
                }
            }
        }
        eleccion.setEstado(estadoNormalizado);
        eleccionRepository.save(eleccion);

        log.info("Estado de elecciÃƒÂ³n '{}' cambiado manualmente a {}", eleccion.getTitulo(), estadoNormalizado);

        return mapEleccionResponse(eleccion, usuario.getId(), eleccion.getGrupo());
    }

    @Transactional
    public CandidatoResponse agregarCandidato(String eleccionId, String correoUsuario, CandidatoItemRequest req) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

        validarAdminGrupo(eleccion.getGrupo().getId(), usuario);

        Candidato candidato = Candidato.builder()
                .eleccion(eleccion)
                .nombre(req.getNombre().trim())
                .puesto(req.getPuesto() != null ? req.getPuesto().trim() : "Candidato/a")
                .descripcion(req.getDescripcion())
                .avatarUrl(req.getAvatarUrl())
                .votos(0L)
                .build();

        candidato = candidatoRepository.save(candidato);

        return CandidatoResponse.builder()
                .id(candidato.getId())
                .name(candidato.getNombre())
                .position(candidato.getPuesto())
                .description(candidato.getDescripcion())
                .avatarUrl(candidato.getAvatarUrl())
                .votesCount(0L)
                .percentage(0.0)
                .build();
    }

    @Transactional
    public VotoResponse emitirVoto(String eleccionId, String correoUsuario, EmitirVotoRequest req) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

                validarMiembroGrupo(eleccion.getGrupo().getId(), usuario.getId());

        if ("inactivo".equalsIgnoreCase(usuario.getEstado()) || "desactivado".equalsIgnoreCase(usuario.getEstado()) || "bloqueado".equalsIgnoreCase(usuario.getEstado())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta se encuentra inactiva. No puedes participar en votaciones.", "CUENTA_INACTIVA");
        }
        actualizarEstadoTemporal(eleccion);

        LocalDateTime ahora = LocalDateTime.now(ZONA_MEXICO);

        if (eleccion.getFechaInicio() != null && ahora.isBefore(eleccion.getFechaInicio())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La votaciÃƒÂ³n aÃƒÂºn no ha iniciado. Comienza el: " + formatearFecha(eleccion.getFechaInicio()), "ELECCION_NO_INICIADA");
        }

        if (eleccion.getFechaFin() != null && ahora.isAfter(eleccion.getFechaFin())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El periodo de votaciÃƒÂ³n ha finalizado. La elecciÃƒÂ³n cerrÃƒÂ³ el: " + formatearFecha(eleccion.getFechaFin()), "ELECCION_CERRADA");
        }

        if (!"activa".equalsIgnoreCase(eleccion.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Esta elecciÃƒÂ³n no estÃƒÂ¡ activa para recibir votos.", "ELECCION_NO_ACTIVA");
        }

        if (registroVotoRepository.existsByEleccionIdAndUsuarioId(eleccionId, usuario.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya has emitido tu voto en esta elecciÃƒÂ³n. No se permite el voto doble.", "VOTO_DUPLICADO");
        }

        Candidato candidato = candidatoRepository.findById(req.getCandidatoId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Candidato no encontrado", "CANDIDATO_NO_ENCONTRADO"));

        if (!candidato.getEleccion().getId().equals(eleccionId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El candidato seleccionado no pertenece a esta elecciÃƒÂ³n.", "CANDIDATO_INVALIDO");
        }

        candidato.setVotos(candidato.getVotos() + 1);
        candidatoRepository.save(candidato);

        String recibo = generarCodigoRecibo();

        RegistroVoto registro = RegistroVoto.builder()
                .eleccion(eleccion)
                .usuario(usuario)
                .codigoRecibo(recibo)
                .build();
        registroVotoRepository.save(registro);

        // Enviar comprobante oficial por correo electrÃ³nico de manera asÃ­ncrona
        try {
            comprobanteVotoEmailService.enviarComprobanteVotoAsync(
                    usuario.getCorreo(),
                    usuario.getNombre(),
                    eleccion.getTitulo(),
                    eleccion.getGrupo() != null ? eleccion.getGrupo().getNombre() : "General",
                    candidato.getNombre(),
                    candidato.getPuesto(),
                    recibo,
                    LocalDateTime.now(ZONA_MEXICO)
            );
        } catch (Exception ex) {
            log.warn("No se pudo programar el envio del comprobante de voto: {}", ex.getMessage());
        }

        log.info("Voto emitido en elecciÃƒÂ³n '{}' con recibo {}", eleccion.getTitulo(), recibo);

        return VotoResponse.builder()
                .receiptCode(recibo)
                .status("VOTO_CONFIRMADO")
                .fecha(LocalDateTime.now(ZONA_MEXICO))
                .electionTitle(eleccion.getTitulo())
                .groupName(eleccion.getGrupo().getNombre())
                .mensaje("Tu voto ha sido registrado de manera secreta y segura.")
                .build();
    }

    @Transactional
    public ResultadosEleccionResponse obtenerResultados(String eleccionId, String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

                validarMiembroGrupo(eleccion.getGrupo().getId(), usuario.getId());

        if ("inactivo".equalsIgnoreCase(usuario.getEstado()) || "desactivado".equalsIgnoreCase(usuario.getEstado()) || "bloqueado".equalsIgnoreCase(usuario.getEstado())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta se encuentra inactiva. No puedes participar en votaciones.", "CUENTA_INACTIVA");
        }
        actualizarEstadoTemporal(eleccion);

        List<Candidato> candidatos = candidatoRepository.findByEleccionIdOrderByVotosDesc(eleccionId);
        long totalVotos = candidatos.stream().mapToLong(Candidato::getVotos).sum();

        List<CandidatoResponse> candResponses = candidatos.stream().map(c -> {
            double porcentaje = totalVotos > 0 ? ((double) c.getVotos() / totalVotos) * 100.0 : 0.0;
            return CandidatoResponse.builder()
                    .id(c.getId())
                    .name(c.getNombre())
                    .position(c.getPuesto())
                    .description(c.getDescripcion())
                    .avatarUrl(c.getAvatarUrl())
                    .votesCount(c.getVotos())
                    .percentage(Math.round(porcentaje * 10.0) / 10.0)
                    .build();
        }).collect(Collectors.toList());

        // Determinar ganador ÃƒÂºnico o empate
        CandidatoResponse ganador = null;
        boolean empate = false;
        List<CandidatoResponse> empatados = new ArrayList<>();

        if (!candResponses.isEmpty() && candResponses.get(0).getVotesCount() > 0) {
            long maxVotes = candResponses.get(0).getVotesCount();
            List<CandidatoResponse> topCandidates = candResponses.stream()
                    .filter(c -> c.getVotesCount() == maxVotes)
                    .collect(Collectors.toList());

            if (topCandidates.size() > 1) {
                empate = true;
                empatados = topCandidates;
                ganador = null; // En empate no hay ganador ÃƒÂºnico
            } else {
                ganador = topCandidates.get(0);
            }
        }

        return ResultadosEleccionResponse.builder()
                .eleccionId(eleccion.getId())
                .titulo(eleccion.getTitulo())
                .grupoNombre(eleccion.getGrupo().getNombre())
                .estado(eleccion.getEstado())
                .totalVotos(totalVotos)
                .ganador(ganador)
                .empate(empate)
                .empatados(empatados)
                .candidatos(candResponses)
                .build();
    }

    private void actualizarEstadoTemporal(Eleccion e) {
        LocalDateTime ahora = LocalDateTime.now(ZONA_MEXICO);
        if (e.getFechaFin() != null && ahora.isAfter(e.getFechaFin()) && !"cerrada".equalsIgnoreCase(e.getEstado())) {
            e.setEstado("cerrada");
            eleccionRepository.save(e);
            log.info("Ã¢ÂÂ° ElecciÃƒÂ³n '{}' cerrada automÃƒÂ¡ticamente por alcanzar fecha fin ({})", e.getTitulo(), e.getFechaFin());
        } else if (e.getFechaInicio() != null && ahora.isBefore(e.getFechaInicio()) && !"programada".equalsIgnoreCase(e.getEstado())) {
            e.setEstado("programada");
            eleccionRepository.save(e);
        } else if (e.getFechaInicio() != null && e.getFechaFin() != null 
                && !ahora.isBefore(e.getFechaInicio()) && !ahora.isAfter(e.getFechaFin()) 
                && "programada".equalsIgnoreCase(e.getEstado())) {
            e.setEstado("activa");
            eleccionRepository.save(e);
            log.info("Ã¢ÂÂ° ElecciÃƒÂ³n '{}' iniciada automÃƒÂ¡ticamente al llegar su fecha inicio ({})", e.getTitulo(), e.getFechaInicio());
        }
    }

    private EleccionResponse mapEleccionResponse(Eleccion e, String usuarioId, Grupo grupo) {
        boolean hasVoted = registroVotoRepository.existsByEleccionIdAndUsuarioId(e.getId(), usuarioId);
        long votesCast = registroVotoRepository.countByEleccionId(e.getId());
        long totalVoters = miembroGrupoRepository.countActivosByGrupoId(grupo.getId());

        List<Candidato> candidatos = (e.getCandidatos() != null && !e.getCandidatos().isEmpty())
                ? e.getCandidatos()
                : candidatoRepository.findByEleccionIdOrderByVotosDesc(e.getId());

        List<CandidatoResponse> candidatosResp = candidatos.stream().map(c ->
                CandidatoResponse.builder()
                        .id(c.getId())
                        .name(c.getNombre())
                        .position(c.getPuesto())
                        .description(c.getDescripcion())
                        .avatarUrl(c.getAvatarUrl())
                        .votesCount(c.getVotos())
                        .build()
        ).collect(Collectors.toList());

        return EleccionResponse.builder()
                .id(e.getId())
                .groupId(grupo.getId())
                .groupName(grupo.getNombre())
                .title(e.getTitulo())
                .description(e.getDescripcion())
                .status(e.getEstado())
                .hasVoted(hasVoted)
                .totalVoters(totalVoters)
                .votesCast(votesCast)
                .startDate(e.getFechaInicio() != null ? e.getFechaInicio().toString() : null)
                .endDate(e.getFechaFin() != null ? e.getFechaFin().toString() : null)
                .candidates(candidatosResp)
                .build();
    }

    private LocalDateTime parsearFecha(String fechaStr, LocalDateTime fallback) {
        if (fechaStr == null || fechaStr.isBlank()) {
            return fallback;
        }
        String limpia = fechaStr.trim();
        try {
            return LocalDateTime.parse(limpia);
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.parse(limpia, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.parse(limpia, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.parse(limpia, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.parse(limpia, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.parse(limpia, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (Exception ignored) {}

        return fallback;
    }

    private String formatearFecha(LocalDateTime ldt) {
        if (ldt == null) return "";
        return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    private String generarCodigoRecibo() {
        StringBuilder sb = new StringBuilder("VS-");
        for (int i = 0; i < 4; i++) {
            sb.append(CARACTERES_RECIBO.charAt(RANDOM.nextInt(CARACTERES_RECIBO.length())));
        }
        sb.append("-");
        sb.append(10 + RANDOM.nextInt(90));
        return sb.toString();
    }

    private void validarMiembroGrupo(String grupoId, String usuarioId) {
        if (!miembroGrupoRepository.existsByGrupoIdAndUsuarioId(grupoId, usuarioId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No perteneces a este grupo para acceder a sus elecciones.", "ACCESO_DENEGADO");
        }
    }

    private void validarAdminGrupo(String grupoId, Usuario usuario) {
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            return;
        }
        MiembroGrupo miembro = miembroGrupoRepository.findByGrupoIdAndUsuarioId(grupoId, usuario.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No eres miembro de este grupo", "ACCESO_DENEGADO"));

        if (!"administrador".equalsIgnoreCase(miembro.getRolEnGrupo())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el administrador de este grupo puede realizar esta acciÃƒÂ³n.", "ACCESO_DENEGADO");
        }
    }

    private Grupo obtenerGrupo(String grupoId) {
        return grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grupo no encontrado", "GRUPO_NO_ENCONTRADO"));
    }

    private Usuario obtenerUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado o no encontrado", "USUARIO_NO_ENCONTRADO"));
    }

    @Transactional
    public EleccionResponse actualizarEleccion(String eleccionId, String correoUsuario, CrearEleccionRequest req) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

        validarAdminGrupo(eleccion.getGrupo().getId(), usuario);

        eleccion.setTitulo(req.getTitulo().trim());
        eleccion.setDescripcion(req.getDescripcion());
        if (req.getFechaFin() != null && !req.getFechaFin().isBlank()) {
            eleccion.setFechaFin(parsearFecha(req.getFechaFin(), eleccion.getFechaFin()));
        }
        eleccion = eleccionRepository.save(eleccion);

        log.info("ElecciÃƒÂ³n {} actualizada por {}", eleccionId, usuario.getCorreo());
        return mapEleccionResponse(eleccion, usuario.getId(), eleccion.getGrupo());
    }

    @Transactional
    public void eliminarEleccion(String eleccionId, String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

        validarAdminGrupo(eleccion.getGrupo().getId(), usuario);

        candidatoRepository.deleteByEleccionId(eleccionId);
        registroVotoRepository.deleteByEleccionId(eleccionId);
        eleccionRepository.delete(eleccion);

        log.info("ElecciÃƒÂ³n {} eliminada por {}", eleccionId, usuario.getCorreo());
    }

    @Transactional
    public void eliminarCandidato(String eleccionId, String candidatoId, String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Eleccion eleccion = eleccionRepository.findById(eleccionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ElecciÃƒÂ³n no encontrada", "ELECCION_NO_ENCONTRADA"));

        validarAdminGrupo(eleccion.getGrupo().getId(), usuario);

        Candidato candidato = candidatoRepository.findById(candidatoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Candidato no encontrado", "CANDIDATO_NO_ENCONTRADO"));

        if (!candidato.getEleccion().getId().equals(eleccionId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El candidato no pertenece a esta elecciÃƒÂ³n", "CANDIDATO_INVALIDO");
        }

        candidatoRepository.delete(candidato);
        log.info("Candidato {} eliminado de elecciÃƒÂ³n {} por {}", candidatoId, eleccionId, usuario.getCorreo());
    }
}


