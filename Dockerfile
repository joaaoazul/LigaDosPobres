# Construção
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Camada separada para as dependências: só é refeita quando o pom muda, o que
# torna as reconstruções seguintes muito mais rápidas.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Execução
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl para os health checks. Sem ele, o healthcheck do compose falha sempre e
# o contentor fica eternamente "unhealthy" sem que nada esteja mal.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Não correr como root: se a aplicação for comprometida, o atacante fica com uma
# conta sem privilégios em vez do controlo do contentor.
RUN useradd --system --uid 10001 liga
USER liga

# Sem o nome da versão: mudar a versão no pom deixa de partir a imagem.
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage em vez de -Xmx: a JVM respeita o limite de memória que a
# plataforma impuser ao contentor, seja ele qual for.
#
# preferIPv6Addresses: a rede privada do Railway só existe em IPv6. O nome
# postgres.railway.internal resolve apenas para um endereço IPv6 e, sem esta
# opção, a JVM não o usa — a ligação à base de dados fica a tentar até estourar
# e a aplicação morre no arranque. Não faz mal onde há IPv4: só muda a ordem
# de preferência quando ambos existem.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.net.preferIPv6Addresses=true", "-jar", "app.jar"]
