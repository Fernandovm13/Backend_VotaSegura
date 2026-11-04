package com.votasegura.api.dto.response;

import com.votasegura.api.model.Rol;
import com.votasegura.api.model.Usuario;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UsuarioResponse {
    private String id;
    private String nombre;
    private String correo;
    private Rol rol;
    private String estado;
    private boolean correoVerificado;
    private boolean biometriaRegistrada;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static UsuarioResponse from(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setCorreo(u.getCorreo());
        r.setRol(u.getRol());
        r.setEstado(u.getEstado());
        r.setCorreoVerificado(u.isCorreoVerificado());
        r.setBiometriaRegistrada(u.isBiometriaRegistrada());
        r.setCreadoEn(u.getCreadoEn());
        r.setActualizadoEn(u.getActualizadoEn());
        return r;
    }
}
