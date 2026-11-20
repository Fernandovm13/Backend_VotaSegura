package com.votasegura.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envoltorio estándar de respuesta de la API VotaSegura")
public class ApiResponse<T> {

    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private boolean success;

    @Schema(description = "Datos de respuesta (tipo varía según el endpoint)")
    private T data;

    @Schema(description = "Metadatos de la respuesta")
    private Meta meta;

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.meta = new Meta("1.0.0", LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Metadatos de la respuesta de la API")
    public static class Meta {

        @Schema(description = "Versión de la API", example = "1.0.0")
        private String version;

        @Schema(description = "Fecha y hora de la respuesta en ISO 8601", example = "2024-11-17T12:00:00")
        private LocalDateTime timestamp;
    }
}
