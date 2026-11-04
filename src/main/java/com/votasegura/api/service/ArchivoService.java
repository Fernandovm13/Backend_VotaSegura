package com.votasegura.api.service;

import com.votasegura.api.dto.response.ArchivoResponse;
import com.votasegura.api.exception.ApiException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ArchivoService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    private static final List<String> TIPOS_PERMITIDOS = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/jpg"
    );

    private static final long TAMANIO_MAXIMO = 10 * 1024 * 1024;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Directorio de almacenamiento de archivos inicializado en: {}", this.rootLocation);
        } catch (IOException e) {
            log.error("No se pudo inicializar el directorio de almacenamiento", e);
            throw new RuntimeException("No se pudo crear el directorio de subida de archivos", e);
        }
    }

    public ArchivoResponse guardarImagen(MultipartFile archivo, HttpServletRequest request) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo de imagen no puede estar vacío.", "ARCHIVO_VACIO");
        }

        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo excede el tamaño máximo permitido de 10 MB.", "ARCHIVO_DEMASIADO_GRANDE");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Formato no permitido. Solo se permiten imágenes JPG, PNG, WEBP o GIF.", "FORMATO_NO_PERMITIDO");
        }

        String originalFilename = archivo.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        } else {
            extension = ".png";
        }

        String nombreGuardado = UUID.randomUUID().toString() + extension;

        try {
            Path destino = this.rootLocation.resolve(nombreGuardado).normalize();
            
            if (!destino.startsWith(this.rootLocation)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ruta de archivo inválida.", "PATH_TRAVERSAL_DETECTED");
            }

            try (InputStream inputStream = archivo.getInputStream()) {
                Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Archivo guardado exitosamente: {} (original: {})", nombreGuardado, originalFilename);

            String esquema = request.getScheme();
            String host = request.getServerName();
            int puerto = request.getServerPort();
            String baseUrl = (puerto == 80 || puerto == 443)
                    ? String.format("%s://%s", esquema, host)
                    : String.format("%s://%s:%d", esquema, host, puerto);

            String urlPublica = String.format("%s/api/v1/archivos/%s", baseUrl, nombreGuardado);

            return ArchivoResponse.builder()
                    .nombreOriginal(originalFilename)
                    .nombreArchivo(nombreGuardado)
                    .url(urlPublica)
                    .tipo(contentType)
                    .tamanioBytes(archivo.getSize())
                    .build();

        } catch (IOException e) {
            log.error("Error al almacenar el archivo", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al guardar la imagen en el servidor.", "ERROR_ALMACENAMIENTO");
        }
    }

    public Resource cargarArchivo(String nombreArchivo) {
        try {
            Path archivoPath = this.rootLocation.resolve(nombreArchivo).normalize();

            if (!archivoPath.startsWith(this.rootLocation)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Acceso no permitido al archivo solicitado.", "PATH_TRAVERSAL_DETECTED");
            }

            Resource resource = new UrlResource(archivoPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException(HttpStatus.NOT_FOUND, "La imagen solicitada no existe o no se puede leer.", "ARCHIVO_NO_ENCONTRADO");
            }
        } catch (MalformedURLException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "URL de archivo malformada.", "URL_INVALIDA");
        }
    }

    public void eliminarArchivo(String nombreArchivo) {
        try {
            Path archivoPath = this.rootLocation.resolve(nombreArchivo).normalize();
            if (!archivoPath.startsWith(this.rootLocation)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Acceso no permitido.", "PATH_TRAVERSAL_DETECTED");
            }
            if (Files.exists(archivoPath)) {
                Files.delete(archivoPath);
                log.info("Archivo eliminado correctamente: {}", nombreArchivo);
            }
        } catch (IOException e) {
            log.error("Error al eliminar archivo: {}", nombreArchivo, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar archivo.", "ERROR_ELIMINACION");
        }
    }
}