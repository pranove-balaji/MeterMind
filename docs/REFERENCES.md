# Official Technical References

Use these sources for version-sensitive questions. Check the documentation again before upgrading dependency versions or changing workflow action versions.

## Spring and Maven

- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) — Java and Maven compatibility.
- [Spring Boot reference documentation](https://docs.spring.io/spring-boot/3.5/reference/index.html) — application configuration, testing, Actuator and deployment.
- [Spring Initializr](https://start.spring.io/) — initial project generation. This project has already been generated; do not regenerate it.
- [Spring Data JPA reference](https://docs.spring.io/spring-data/jpa/reference/) — repositories and persistence.
- [Spring Security reference](https://docs.spring.io/spring-security/reference/) — authentication, password encoding and authorization.
- [Maven Wrapper documentation](https://maven.apache.org/wrapper/) — reproducible project builds.

## MySQL and Flyway

- [MySQL 8.4 Reference Manual](https://dev.mysql.com/doc/refman/8.4/en/) — SQL syntax, transactions and constraints.
- [Flyway MySQL database driver](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mysql) — current requirement for the separate MySQL Flyway module. Open-source coordinates are `org.flywaydb:flyway-mysql`; keep Flyway versions aligned with Spring Boot dependency management unless there is a deliberate compatibility reason.
- [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations) — migration naming and versioning.

## GitHub Actions and containers

- [GitHub Actions: Java with Maven](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven) — CI workflow concepts and Maven caching.
- [actions/checkout](https://github.com/actions/checkout) — official checkout action. The template uses `actions/checkout@v7` based on the current official release surfaced while preparing this kit; review current official releases when updating it.
- [actions/setup-java](https://github.com/actions/setup-java) — Java installation and Maven caching. The template uses `actions/setup-java@v6` based on current official documentation.
- [Docker build-push-action](https://github.com/docker/build-push-action) — build and push an image.
- [Docker metadata-action](https://github.com/docker/metadata-action) — Docker image tags and labels.
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry) — publishing and pulling `ghcr.io` images.

## Research hygiene

- Prefer official vendor documentation for syntax, compatibility and security behavior.
- Record the source/date when a significant version-dependent choice is made.
- Do not blindly upgrade every dependency; preserve Spring Boot's dependency management where possible.
- Do not make claims that a feature is production-ready just because a basic local test passes.
