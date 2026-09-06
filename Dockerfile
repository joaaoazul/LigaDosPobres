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
# Aqui esteve -Djava.net.preferIPv6Addresses=true, para a rede privada do
# Railway. Estava a mais, e fazia mal: a opção só decide a ORDEM quando um nome
# tem endereços das duas famílias. Um nome que só tenha IPv6, como o
# postgres.railway.internal, resolve para IPv6 na mesma sem ela — medido, com
# e sem a opção.
#
# O que ela fazia era mandar para IPv6 tudo o que tem as duas, incluindo a
# api.resend.com. O Railway não tem saída IPv6 para a internet pública, por
# isso os emails de recuperação de password morriam todos em "Network is
# unreachable", e o erro não tinha nada a ver com o email nem com o Resend.
#
# Sem a opção, a JVM prefere IPv4 quando existem os dois e usa IPv6 quando é o
# único que há. É o que serve os dois casos.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
