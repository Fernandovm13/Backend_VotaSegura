# ==========================================
# VotaSegura API - Multi-stage Dockerfile
# Base: Eclipse Temurin Java 17
# ==========================================

# Etapa 1: Compilacion y empaquetado con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copiar pom.xml para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar codigo fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final ligera de produccion
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Crear directorio para subida de archivos/imagenes
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copiar el ejecutable .jar generado en la etapa 1
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=America/Mexico_City", "-jar", "app.jar"]
