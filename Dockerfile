# Construção
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Camada separada para as dependências: só é refeita quando o pom muda.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Execução
FROM eclipse-temurin:21-jre
WORKDIR /app

# Não correr como root: se a aplicação for comprometida, o atacante fica com
# uma conta sem privilégios em vez do controlo do contentor.
RUN useradd --system --uid 10001 liga
USER liga

COPY --from=build /app/target/LigaDosPobres-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
