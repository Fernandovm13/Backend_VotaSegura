package com.votasegura.api.controller;

import com.votasegura.api.dto.response.ApiResponse;
import com.votasegura.api.dto.response.ArchivoResponse;
import com.votasegura.api.exception.ApiException;
import com.votasegura.api.service.ArchivoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final ArchivoService archivoService;

    /**
     * Endpoint para subir una imagen de candidato o usuario desde el dispositivo.
     * Acepta multipart/form-data con el parametro 'archivo' o 'file'.
     */
    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ArchivoResponse>> subirArchivo(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {

        MultipartFile archivoFinal = (archivo != null && !archivo.isEmpty()) ? archivo : file;

        if (archivoFinal == null || archivoFinal.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debes adjuntar un archivo en el campo 'archivo' o 'file'.", "ARCHIVO_REQUERIDO");
        }

        ArchivoResponse respuesta = archivoService.guardarImagen(archivoFinal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(respuesta));
    }

    /**
     * Endpoint publico para visualizar o descargar la imagen almacenada.
     */
    @GetMapping("/{nombreArchivo:.+}")
    public ResponseEntity<Resource> verArchivo(@PathVariable String nombreArchivo) {
        Resource recurso = archivoService.cargarArchivo(nombreArchivo);

        String contentType = null;
        try {
            contentType = Files.probeContentType(recurso.getFile().toPath());
        } catch (IOException ignored) {
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                .body(recurso);
    }

    /**
     * Endpoint para eliminar un archivo almacenado previamente.
     */
    @DeleteMapping("/{nombreArchivo:.+}")
    public ResponseEntity<ApiResponse<String>> eliminarArchivo(@PathVariable String nombreArchivo) {
        archivoService.eliminarArchivo(nombreArchivo);
        return ResponseEntity.ok(ApiResponse.success("Archivo eliminado correctamente."));
    }
}