package com.votasegura.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprobante de voto emitido (anónimo, sin referencia al candidato elegido)")
public class VotoResponse {

    @Schema(description = "Código de recibo único del voto (para auditoría anónima)", example = "REC-8A2F3C1D-7B6E")
    private String receiptCode;

    @Schema(description = "Estado del voto registrado", example = "REGISTRADO", allowableValues = {"REGISTRADO", "PENDIENTE"})
    private String status;

    @Schema(description = "Fecha y hora en que se emitió el voto", example = "2024-12-01T14:30:00")
    private LocalDateTime fecha;

    @Schema(description = "Título de la elección en la que se votó", example = "Elección de Presidente 2024")
    private String electionTitle;

    @Schema(description = "Nombre del grupo donde se realizó la elección", example = "Consejo Estudiantil 2024")
    private String groupName;

    @Schema(description = "Mensaje informativo para el usuario sobre su voto", example = "Tu voto ha sido registrado exitosamente. Guarda tu código de recibo.")
    private String mensaje;
}