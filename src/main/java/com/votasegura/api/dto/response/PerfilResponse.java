package com.votasegura.api.dto.response;

import com.votasegura.api.model.Rol;
import com.votasegura.api.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Perfil completo del usuario autenticado")
public class PerfilResponse {

    @Schema(description = "Identificador único del usuario (UUID)", example = "usr-3d8f1a2b-4c7e")
    private String id;

    @Schema(description = "Nombre completo del usuario", example = "Ana García López")
    private String nombre;

    @Schema(description = "Correo electrónico del usuario", example = "ana.garcia@correo.com")
    private String correo;

    @Schema(description = "Rol del usuario en el sistema", example = "VOTANTE", allowableValues = {"VOTANTE", "ADMIN"})
    private Rol rol;

    @Schema(description = "Estado de la cuenta del usuario", example = "ACTIVO", allowableValues = {"ACTIVO", "INACTIVO"})
    private String estado;

    @Schema(description = "Indica si el correo electrónico fue verificado", example = "true")
    private boolean correoVerificado;

    @Schema(description = "Indica si el usuario tiene biometría (WebAuthn/FIDO2) registrada", example = "false")
    private boolean biometriaRegistrada;

    @Schema(description = "Número de elecciones disponibles para votar actualmente", example = "2")
    private int eleccionesDisponibles;

    @Schema(description = "Número total de elecciones en las que ya votó", example = "5")
    private int eleccionesVotadas;

    @Schema(description = "Fecha y hora de creación de la cuenta", example = "2024-10-01T09:00:00")
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de la última actualización del perfil", example = "2024-11-15T18:30:00")
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
