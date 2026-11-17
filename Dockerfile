# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar ----------
# Compila con javac directamente (sin Maven): el backend no tiene dependencias
# de runtime, así que no hace falta descargar nada desde el repositorio Maven.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY src ./src
RUN find src/main/java -name "*.java" > sources.txt \
 && javac -encoding UTF-8 -d out @sources.txt

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Clases compiladas y datos. Las rutas "data/*.txt" son relativas al directorio
# de trabajo (/app), por eso WORKDIR debe ser la raíz del proyecto.
COPY --from=build /build/out ./classes
COPY data ./data

# Usuario sin privilegios; necesita escribir en data/ porque los DAO reescriben los .txt.
RUN useradd --system --no-create-home appuser \
 && chown -R appuser /app/data
USER appuser

# Render inyecta PORT; este valor solo aplica si no se define.
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "/app/classes", "com.blinkdelivery.api.ApiServer"]
