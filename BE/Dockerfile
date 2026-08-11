# ────────────────────────────────────────────────────────────────
# Stage 1: Build
# ────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

COPY src ./src
ARG MAVEN_SKIP_TESTS=false
RUN if [ "$MAVEN_SKIP_TESTS" = "true" ]; then \
    mvn package -DskipTests -B --no-transfer-progress; \
    else \
    mvn package -B --no-transfer-progress; \
    fi

# ────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21.0.11_10-jre-alpine-3.22 AS runtime

ENV XDG_CACHE_HOME=/opt/tectonic-cache \
    TECTONIC_UNTRUSTED_MODE=1 \
    TEX_COMPILE_ONLY_CACHED=true

RUN apk add --no-cache tectonic=0.15.0-r4 \
    && mkdir -p "$XDG_CACHE_HOME" /tmp/tectonic-output

# Fetch every package used by the supported templates at image-build time.
# Runtime compilation then uses --only-cached and performs no package download.
COPY --from=builder /build/src/main/resources/paper-templates /tmp/paper-templates
RUN for template in /tmp/paper-templates/*.tex; do \
        tectonic -X compile --untrusted --outdir /tmp/tectonic-output "$template"; \
    done \
    && rm -rf /tmp/paper-templates /tmp/tectonic-output \
    && chmod -R a=rX "$XDG_CACHE_HOME"

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Safely copy the exact jar, explicitly changing ownership to the non-root user
COPY --chown=appuser:appgroup --from=builder /build/target/evidence-pilot.jar app.jar

# Drop root privileges immediately
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
