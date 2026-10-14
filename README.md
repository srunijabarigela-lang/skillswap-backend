# SkillSwap Backend

Spring Boot 3 + Java 17 + MySQL REST API. See the root `README.md` for full setup, testing, and deployment steps.

Local start: open this folder (the one containing `pom.xml`) in VS Code and run `mvn spring-boot:run`.

Health check: `http://localhost:8080/api/health`

Configure `spring.datasource.password` in `src/main/resources/application.properties` or provide `DB_PASSWORD` as an environment variable. Never commit a real database password.
