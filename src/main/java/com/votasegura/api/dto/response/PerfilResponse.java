package com.votasegura.api.dto.response;

import com.votasegura.api.model.Rol;
import com.votasegura.api.model.Usuario;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PerfilResponse {
    private String id;
    private String nombre;
    private String correo;
    private Rol rol;
    private String estado;
    private boolean correoVerificado;
    private boolean biometriaRegistrada;
    private int eleccionesDisponibles;
    private int eleccionesVotadas;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    /** Factory method sin conteo de votadas (conteo = 0) */
    public static PerfilResponse from(Usuario u) {
        return from(u, 0);
    }

    /** Factory method con conteo real de elecciones votadas */
    public static PerfilResponse from(Usuario u, int eleccionesVotadas) {
        PerfilResponse r = new PerfilResponse();
        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setCorreo(u.getCorreo());
        r.setRol(u.getRol());
        r.setEstado(u.getEstado());
        r.setCorreoVerificado(u.isCorreoVerificado());
        r.setBiometriaRegistrada(u.isBiometriaRegistrada());
        r.setEleccionesDisponibles(0);
        r.setEleccionesVotadas(eleccionesVotadas);
        r.setCreadoEn(u.getCreadoEn());
        r.setActualizadoEn(u.getActualizadoEn());
        return r;
    }
}
