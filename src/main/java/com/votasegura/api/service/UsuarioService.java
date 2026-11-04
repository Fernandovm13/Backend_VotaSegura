package com.votasegura.api.service;

import com.votasegura.api.dto.request.ActualizarPerfilRequest;
import com.votasegura.api.dto.response.HistorialVotoResponse;
import com.votasegura.api.dto.response.PerfilResponse;
import com.votasegura.api.exception.ApiException;
import com.votasegura.api.model.RegistroVoto;
import com.votasegura.api.model.Usuario;
import com.votasegura.api.repository.RegistroVotoRepository;
import com.votasegura.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RegistroVotoRepository registroVotoRepository;

    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "El usuario asociado al token ya no existe.", "USER_NOT_FOUND"));

        long votadas = registroVotoRepository.countByUsuarioId(usuario.getId());
        return PerfilResponse.from(usuario, (int) votadas);
    }

    @Transactional(readOnly = true)
    public List<HistorialVotoResponse> obtenerHistorialVotos(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado.", "USER_NOT_FOUND"));

        List<RegistroVoto> votos = registroVotoRepository.findByUsuarioIdWithDetails(usuario.getId());
        return votos.stream()
                .map(HistorialVotoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PerfilResponse actualizarPerfil(String correo, ActualizarPerfilRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado", "USER_NOT_FOUND"));

        usuario.setNombre(req.getNombreCompleto().trim());
        usuario = usuarioRepository.save(usuario);

        long votadas = registroVotoRepository.countByUsuarioId(usuario.getId());
        return PerfilResponse.from(usuario, (int) votadas);
    }

    @Transactional
    public void desactivarCuenta(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado", "USER_NOT_FOUND"));

        usuario.setEstado("inactivo");
        usuarioRepository.save(usuario);
    }
}
