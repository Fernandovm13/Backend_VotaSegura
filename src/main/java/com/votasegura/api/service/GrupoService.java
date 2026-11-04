package com.votasegura.api.service;

import com.votasegura.api.dto.request.CrearGrupoRequest;
import com.votasegura.api.dto.response.GrupoResponse;
import com.votasegura.api.exception.ApiException;
import com.votasegura.api.model.Grupo;
import com.votasegura.api.model.MiembroGrupo;
import com.votasegura.api.model.Usuario;
import com.votasegura.api.repository.EleccionRepository;
import com.votasegura.api.repository.GrupoRepository;
import com.votasegura.api.repository.MiembroGrupoRepository;
import com.votasegura.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final EleccionRepository eleccionRepository;
    private final UsuarioRepository usuarioRepository;

    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public GrupoResponse crearGrupo(String correoUsuario, CrearGrupoRequest req) {
        Usuario usuario = obtenerUsuario(correoUsuario);

        String codigo = generarCodigoUnico();

        Grupo grupo = Grupo.builder()
                .nombre(req.getNombre().trim())
                .codigo(codigo)
                .descripcion(req.getDescripcion())
                .creador(usuario)
                .build();

        grupo = grupoRepository.save(grupo);

        // El creador es miembro con rol administrador
        MiembroGrupo miembroAdmin = MiembroGrupo.builder()
                .grupo(grupo)
                .usuario(usuario)
                .rolEnGrupo("administrador")
                .build();
        miembroGrupoRepository.save(miembroAdmin);

        log.info("Grupo creado: '{}' con código {} por el usuario {}", grupo.getNombre(), grupo.getCodigo(), usuario.getCorreo());

        return GrupoResponse.builder()
                .id(grupo.getId())
                .name(grupo.getNombre())
                .code(grupo.getCodigo())
                .description(grupo.getDescripcion())
                .role("administrador")
                .electionsCount(0)
                .membersCount(1)
                .creadoEn(grupo.getCreadoEn())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GrupoResponse> listarMisGrupos(String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        List<MiembroGrupo> membresias = miembroGrupoRepository.findByUsuarioId(usuario.getId());

        return membresias.stream().map(m -> {
            Grupo g = m.getGrupo();
            long eleccionesCount = eleccionRepository.countByGrupoId(g.getId());
            long miembrosCount = miembroGrupoRepository.countActivosByGrupoId(g.getId());

            return GrupoResponse.builder()
                    .id(g.getId())
                    .name(g.getNombre())
                    .code(g.getCodigo())
                    .description(g.getDescripcion())
                    .role(m.getRolEnGrupo())
                    .electionsCount(eleccionesCount)
                    .membersCount(miembrosCount)
                    .creadoEn(g.getCreadoEn())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public GrupoResponse unirseAGrupo(String correoUsuario, String codigo) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        String codigoLimpio = codigo.trim().toUpperCase();

        Grupo grupo = grupoRepository.findByCodigo(codigoLimpio)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe ningún grupo con el código: " + codigoLimpio, "GRUPO_NO_ENCONTRADO"));

        var miembroExistente = miembroGrupoRepository.findByGrupoIdAndUsuarioId(grupo.getId(), usuario.getId());
        if (miembroExistente.isPresent()) {
            MiembroGrupo m = miembroExistente.get();
            return mapToResponse(grupo, m.getRolEnGrupo());
        }

        MiembroGrupo nuevoMiembro = MiembroGrupo.builder()
                .grupo(grupo)
                .usuario(usuario)
                .rolEnGrupo("votante")
                .build();
        miembroGrupoRepository.save(nuevoMiembro);

        log.info("Usuario {} se unió al grupo '{}' ({})", usuario.getCorreo(), grupo.getNombre(), grupo.getCodigo());

        return mapToResponse(grupo, "votante");
    }

    @Transactional(readOnly = true)
    public GrupoResponse obtenerGrupo(String grupoId, String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grupo no encontrado", "GRUPO_NO_ENCONTRADO"));

        MiembroGrupo membresia = miembroGrupoRepository.findByGrupoIdAndUsuarioId(grupo.getId(), usuario.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No eres miembro de este grupo", "ACCESO_DENEGADO"));

        return mapToResponse(grupo, membresia.getRolEnGrupo());
    }

    private GrupoResponse mapToResponse(Grupo g, String role) {
        long eleccionesCount = eleccionRepository.countByGrupoId(g.getId());
        long miembrosCount = miembroGrupoRepository.countActivosByGrupoId(g.getId());

        return GrupoResponse.builder()
                .id(g.getId())
                .name(g.getNombre())
                .code(g.getCodigo())
                .description(g.getDescripcion())
                .role(role)
                .electionsCount(eleccionesCount)
                .membersCount(miembrosCount)
                .creadoEn(g.getCreadoEn())
                .build();
    }

    private String generarCodigoUnico() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder();
            // Formato ej: K7P-2QX
            for (int j = 0; j < 3; j++) {
                sb.append(CARACTERES_CODIGO.charAt(RANDOM.nextInt(CARACTERES_CODIGO.length())));
            }
            sb.append("-");
            for (int j = 0; j < 3; j++) {
                sb.append(CARACTERES_CODIGO.charAt(RANDOM.nextInt(CARACTERES_CODIGO.length())));
            }
            String code = sb.toString();
            if (!grupoRepository.existsByCodigo(code)) {
                return code;
            }
        }
        return "VS-" + System.currentTimeMillis() % 100000;
    }

    private Usuario obtenerUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado o no encontrado", "USUARIO_NO_ENCONTRADO"));
    }

    @Transactional
    public GrupoResponse actualizarGrupo(String grupoId, String correoUsuario, CrearGrupoRequest req) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grupo no encontrado", "GRUPO_NO_ENCONTRADO"));

        MiembroGrupo miembro = miembroGrupoRepository.findByGrupoIdAndUsuarioId(grupo.getId(), usuario.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No eres miembro de este grupo", "ACCESO_DENEGADO"));

        if (!"administrador".equalsIgnoreCase(miembro.getRolEnGrupo()) && usuario.getRol() != com.votasegura.api.model.Rol.ADMINISTRADOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el administrador del grupo puede actualizar sus datos", "ACCESO_DENEGADO");
        }

        grupo.setNombre(req.getNombre().trim());
        grupo.setDescripcion(req.getDescripcion());
        grupo = grupoRepository.save(grupo);

        log.info("Grupo {} actualizado por usuario {}", grupo.getId(), usuario.getCorreo());
        return mapToResponse(grupo, miembro.getRolEnGrupo());
    }

    @Transactional
    public void eliminarGrupo(String grupoId, String correoUsuario) {
        Usuario usuario = obtenerUsuario(correoUsuario);
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grupo no encontrado", "GRUPO_NO_ENCONTRADO"));

        MiembroGrupo miembro = miembroGrupoRepository.findByGrupoIdAndUsuarioId(grupo.getId(), usuario.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No eres miembro de este grupo", "ACCESO_DENEGADO"));

        if (!"administrador".equalsIgnoreCase(miembro.getRolEnGrupo()) && usuario.getRol() != com.votasegura.api.model.Rol.ADMINISTRADOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el administrador del grupo puede eliminarlo", "ACCESO_DENEGADO");
        }

        miembroGrupoRepository.deleteByGrupoId(grupo.getId());
        eleccionRepository.deleteByGrupoId(grupo.getId());
        grupoRepository.delete(grupo);
        log.info("Grupo {} eliminado por usuario {}", grupo.getId(), usuario.getCorreo());
    }

    @Transactional
    public void eliminarMiembro(String grupoId, String usuarioIdAEliminar, String correoUsuarioAutenticado) {
        Usuario solicitante = obtenerUsuario(correoUsuarioAutenticado);
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grupo no encontrado", "GRUPO_NO_ENCONTRADO"));

        MiembroGrupo miembroSolicitante = miembroGrupoRepository.findByGrupoIdAndUsuarioId(grupo.getId(), solicitante.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No eres miembro de este grupo", "ACCESO_DENEGADO"));

        boolean esAdmin = "administrador".equalsIgnoreCase(miembroSolicitante.getRolEnGrupo()) || solicitante.getRol() == com.votasegura.api.model.Rol.ADMINISTRADOR;

        if (!esAdmin && !solicitante.getId().equals(usuarioIdAEliminar)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un administrador puede expulsar a otros miembros", "ACCESO_DENEGADO");
        }

        MiembroGrupo miembroAEliminar = miembroGrupoRepository.findByGrupoIdAndUsuarioId(grupo.getId(), usuarioIdAEliminar)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El miembro no pertenece a este grupo", "MIEMBRO_NO_ENCONTRADO"));

        miembroGrupoRepository.delete(miembroAEliminar);
        log.info("Miembro {} eliminado del grupo {} por {}", usuarioIdAEliminar, grupoId, solicitante.getCorreo());
    }
}