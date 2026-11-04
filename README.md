# VotaSegura — Backend API

> **© 2026 Fernando Vélez Méndez. Todos los derechos reservados.**  
> Este proyecto es de autoría propia. Queda prohibida su copia, distribución o uso comercial sin autorización expresa del autor.

---

## ¿Qué es VotaSegura?

VotaSegura es una API REST segura que alimenta la plataforma de votaciones internas anónimas del mismo nombre. Gestiona la autenticación, biometría, grupos, elecciones, candidatos y el sistema de votación anónima mediante una arquitectura que garantiza matemática y estructuralmente que **nadie, ni el administrador con acceso root a la base de datos, puede conocer el voto de un usuario**.

## Tecnologías Utilizadas

| Tecnología | Versión | Uso |
|:---|:---|:---|
| **Java** | 21 | Lenguaje principal |
| **Spring Boot** | 3.2.4 | Framework REST API |
| **Spring Security** | — | Autenticación, autorización, CORS |
| **Spring Data JPA / Hibernate** | — | ORM y manejo de base de datos |
| **MySQL** | 8.x | Base de datos relacional |
| **JWT (JSON Web Tokens)** | — | Autenticación sin estado (stateless) |
| **WebAuthn / FIDO2** | — | Registro y validación biométrica |
| **JavaMail (SMTP Gmail)** | — | Envío de OTP y comprobantes de voto |
| **Lombok** | — | Reducción de código boilerplate |
| **Maven** | 3.x | Gestión de dependencias y build |

## Características Principales

- 🔐 **JWT + Refresh Tokens**: autenticación segura sin estado con rotación de tokens
- 🧬 **WebAuthn / Biometría FIDO2**: registro y validación de huella digital o Face ID con chip de seguridad del dispositivo
- 🗳️ **Voto 100% anónimo**: separación estructural entre padrón electoral (`registro_votos`) y urna (`candidatos.votos`). Sin ninguna relación en base de datos entre usuario y candidato
- ⏰ **Cierre automático de elecciones**: tarea programada (`@Scheduled`) que cierra elecciones vencidas cada 60 segundos
- 🛡️ **Rate Limiting**: límite de 25 peticiones/min en autenticación y 150 req/min general
- 📧 **Correo asíncrono**: envío de OTPs y comprobantes de voto con `@Async`
- 👥 **Roles y permisos**: administrador de grupo y votante con validaciones en cada endpoint
- 🧹 **Limpieza automática**: eliminación periódica de tokens expirados y OTPs vencidos
- 🖼️ **Subida de imágenes**: gestión de avatares de candidatos vía multipart

## Arquitectura del Anonimato del Voto

```
registro_votos          candidatos
┌─────────────┐         ┌──────────────┐
│ usuario_id  │         │ nombre       │
│ eleccion_id │         │ eleccion_id  │
│ codigo_recibo│        │ votos (int)  │
│ emitido_en  │         └──────────────┘
└─────────────┘
       │                      │
       └──── elecciones ──────┘
              (único nexo)

❌ NO existe relación directa entre
   registro_votos ↔ candidatos
```

## Estructura del Proyecto

```
src/main/java/com/votasegura/api/
├── config/           # Beans de configuración (CORS, tasks, seguridad)
├── controller/       # Controladores REST
├── dto/              # DTOs de Request y Response
├── exception/        # Manejo global de excepciones (RFC 7807)
├── filter/           # Filtros de JWT y Rate Limiting
├── model/            # Entidades JPA (tablas de la BD)
├── repository/       # Interfaces JPA Repository
├── security/         # SecurityConfig, JwtUtil
└── service/          # Lógica de negocio
```

## Endpoints Principales

| Método | Ruta | Descripción |
|:---|:---|:---|
| `POST` | `/api/v1/auth/register` | Registro de usuario |
| `POST` | `/api/v1/auth/login` | Inicio de sesión |
| `POST` | `/api/v1/auth/otp/verify` | Verificación de OTP |
| `GET` | `/api/v1/grupos/mis-grupos` | Listar mis grupos |
| `POST` | `/api/v1/grupos` | Crear grupo |
| `GET` | `/api/v1/grupos/{id}/elecciones` | Listar elecciones del grupo |
| `POST` | `/api/v1/elecciones/{id}/votar` | Emitir voto |
| `GET` | `/api/v1/elecciones/{id}/resultados` | Ver resultados |
| `POST` | `/api/v1/biometria/registrar` | Registrar biométrico |
| `POST` | `/api/v1/biometria/autenticar` | Autenticar con biométrico |

## Configuración Local

1. Clona el repositorio
2. Crea la base de datos en MySQL:
   ```sql
   CREATE DATABASE vota_segura_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Copia el archivo de configuración de ejemplo:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
4. Edita `application.properties` con tus credenciales locales (MySQL, correo, JWT secret)
5. Inicia el servidor:
   ```bash
   mvn spring-boot:run
   ```
6. La API estará disponible en `http://localhost:8080`

## Exposición a Internet (Cloudflare Tunnel)

```bash
npx cloudflared tunnel --protocol http2 --url http://localhost:8080
```

---

## Autor

**Fernando Vélez Méndez**  
Desarrollador Full Stack  
© 2026 — Todos los derechos reservados.

> *"El reto principal de VotaSegura fue lograr que el voto sea completamente anónimo incluso para el propio creador del sistema. Esto se logró mediante separación estructural en la base de datos: el padrón y la urna nunca se tocan."*

