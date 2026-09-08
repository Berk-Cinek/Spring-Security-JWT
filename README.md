# Spring Security JWT

A stateless authentication API built with Spring Boot and Spring Security. Users sign up, receive a six-digit verification code by email, confirm it, then log in to receive a signed JWT used to authenticate every request after that — no server-side sessions.

## Features

- Email + password signup with BCrypt password hashing
- Email verification via a time-limited 6-digit code (SMTP / Gmail)
- Stateless JWT authentication (HS256) issued on login
- Custom `OncePerRequestFilter` that validates the bearer token on every protected request
- PostgreSQL persistence via Spring Data JPA / Hibernate

## Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 26 |
| Framework | Spring Boot 4.1.1, Spring Security 7.1 |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL |
| Auth tokens | [jjwt](https://github.com/jwtk/jjwt) 0.11.5 (HS256) |
| Email | Spring Mail (SMTP) |
| Build | Gradle (Kotlin DSL) |


## Prerequisites

- JDK 26
- A PostgreSQL database (this project was built and tested against [Supabase](https://supabase.com))
- An SMTP account for sending verification emails (a Gmail [app password](https://myaccount.google.com/apppasswords) works)

## Configuration

Configuration values are read from environment variables via `application.properties`, loaded from a local `.env` file at startup:

```properties
spring.config.import=optional:file:.env[.properties]
```

Create a `.env` file in the project root (already git-ignored) with:

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME=<db-username>
SPRING_DATASOURCE_PASSWORD=<db-password>

JWT_SECRET_KEY=<base64-encoded-secret>

SUPPORT_EMAIL=<smtp-account-email>
APP_PASSWORD=<smtp-app-password>
```

**`JWT_SECRET_KEY` must be valid Base64**

The API starts on `http://localhost:8080`.

## API reference

All request/response bodies are JSON.

| Method | Endpoint | Auth required | Body |
|---|---|---|---|
| `POST` | `/auth/signup` | No | `{ "username": "...", "email": "...", "password": "..." }` |
| `POST` | `/auth/login` | No | `{ "email": "...", "password": "..." }` |
| `POST` | `/auth/verify` | No | `{ "email": "...", "verificationCode": "..." }` |
| `POST` | `/auth/resend?email=...` | No | — |
| `GET` | `/users/me` | Yes | — |
| `GET` | `/users/` | Yes | — |

Authenticated requests send the token from `/auth/login` as a bearer header:

```
Authorization: Bearer <token>
```

### Example: sign up

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"berk","email":"berk@example.com","password":"correct horse battery staple"}'
```

### Example: verify the emailed code

```bash
curl -X POST http://localhost:8080/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"berk@example.com","verificationCode":"123456"}'
```

### Example: log in

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"berk@example.com","password":"correct horse battery staple"}'
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600000
}
```

### Example: call a protected endpoint

```bash
curl http://localhost:8080/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## How authentication works

1. **Signup** hashes the password with BCrypt, generates a 6-digit verification code with a 25-minute expiry, emails it, and saves the account with `enabled=false`.
2. **Verify** checks the code and expiry, then flips `enabled=true` and clears the code.
3. **Login** rejects unverified accounts before checking credentials, otherwise delegates to Spring Security's `AuthenticationManager` (BCrypt comparison via `DaoAuthenticationProvider`), then issues a signed JWT.
4. **Every request after that** carries the JWT in an `Authorization: Bearer` header. `JwtAuthenticationFilter` — registered *before* Spring's own login filter — validates the signature and expiry, reloads the user, and populates the security context. No session is ever created (`SessionCreationPolicy.STATELESS`).
