# ==========================================
# Stage 1: Build stage
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Copy Maven wrapper and dependencies specification
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make wrapper executable and download dependencies for caching
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy project source code
COPY src/ src/

# Compile and package application (skipping tests since CI runs them)
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal Non-Root Runtime stage
# ==========================================
FROM eclipse-temurin:17-jre-jammy AS runner
WORKDIR /app

# Security principle of Least Privilege: run as dedicated non-root user
RUN addgroup --system --gid 1001 metermind && \
    adduser --system --uid 1001 --ingroup metermind --shell /bin/false metermind

# Copy executable jar from builder stage
COPY --from=builder --chown=metermind:metermind /workspace/target/*.jar app.jar

# Set non-root user ownership
USER metermind:metermind

# Expose standard application HTTP port
EXPOSE 8080

# Production environment defaults
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Health check verifies the HTTP server responds
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/login || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
