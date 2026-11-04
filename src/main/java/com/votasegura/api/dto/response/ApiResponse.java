package com.votasegura.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
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
    public static class Meta {
        private String version;
        private LocalDateTime timestamp;
    }
}
