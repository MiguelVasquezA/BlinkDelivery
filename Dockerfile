# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar ----------
# Maven descarga el driver JDBC de PostgreSQL definido en pom.xml; javac compila
# el código contra él.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -q -B dependency:copy-dependencies -DoutputDirectory=/build/lib -DincludeScope=runtime
COPY src ./src
RUN find src/main/java -name "*.java" > sources.txt \
 && javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Clases compiladas y datos. Las rutas "data/*.txt" son relativas al directorio
# de trabajo (/app), por eso WORKDIR debe ser la raíz del proyecto.
COPY --from=build /build/out ./classes
COPY --from=build /build/lib ./lib
COPY data ./data

# Usuario sin privilegios; necesita escribir en data/ porque los DAO reescriben los .txt.
RUN useradd --system --no-create-home appuser \
 && chown -R appuser /app/data
USER appuser

# Render inyecta PORT; este valor solo aplica si no se define.
ENV PORT=8080
EXPOSE 8080

# Al arrancar, copia el Secret File de Render a data/ (la app lee data/credenciales.txt).
# Se hace en tiempo de ejecución porque los Secret Files no existen durante el build.
# La conexión a la base de datos se configura en Render con DATABASE_URL, DATABASE_USER y
# DATABASE_PASSWORD (no se definen aquí).
CMD ["sh", "-c", "mkdir -p /app/data && cp -f /etc/secrets/credenciales.txt /app/data/credenciales.txt && exec java -cp \"/app/classes:/app/lib/*\" com.blinkdelivery.api.ApiServer"]
