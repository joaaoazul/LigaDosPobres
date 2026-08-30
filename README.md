# Liga dos Pobres

Aplicação de gestão da Liga dos Pobres: ligas, equipas, jornadas e classificação.

Backend em Java (Spring Boot) com os serviços de domínio existentes e uma interface
web servida pela própria aplicação (HTML/CSS/JavaScript, sem dependências de frontend).

## Como correr

```bash
mvn spring-boot:run
```

Depois abre <http://localhost:8080>.

Requisitos: JDK 21+ e Maven.

> Os dados são guardados **em memória** — ao parar a aplicação, perde-se tudo.

## O que a interface já faz

- Criar e listar ligas, terminar uma liga
- Adicionar equipas (equipa + treinador) e registar desistências
- Abrir jornadas (as 5 primeiras são de treino, as seguintes oficiais)
- Inserir pontuações por equipa e fechar a jornada (posições por pontuação)
- Ver a classificação geral (equipas desistentes ficam nas últimas posições)

## Por implementar

- Dívidas e blocos de dívida (`DividaService` ainda é um esboço)
- Resolução manual de empates (`DesempateService`); por agora, equipas empatadas
  ficam com a mesma posição na jornada
- Persistência em base de dados

## API REST

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/api/ligas` | Lista as ligas |
| POST | `/api/ligas` | Cria uma liga (`nome`, `maxEquipas`) |
| GET | `/api/ligas/{ligaId}` | Detalhe da liga (equipas, jornadas, classificação) |
| POST | `/api/ligas/{ligaId}/terminar` | Termina a liga |
| GET | `/api/ligas/{ligaId}/classificacao` | Classificação geral |
| POST | `/api/ligas/{ligaId}/equipas` | Adiciona uma equipa (`nome`, `treinador`) |
| POST | `/api/ligas/{ligaId}/equipas/{equipaId}/desistencia` | Regista uma desistência |
| GET | `/api/ligas/{ligaId}/jornadas` | Lista as jornadas da liga |
| POST | `/api/ligas/{ligaId}/jornadas` | Abre a próxima jornada |
| GET | `/api/jornadas/{jornadaId}` | Detalhe de uma jornada |
| PUT | `/api/jornadas/{jornadaId}/resultados` | Insere/atualiza uma pontuação (`equipaId`, `pontuacao`) |
| POST | `/api/jornadas/{jornadaId}/fechar` | Fecha a jornada e atribui posições |

Erros do domínio são traduzidos em `400` (validação), `409` (regra de negócio)
e `404` (recurso inexistente), sempre com `{"status": ..., "mensagem": ...}`.

## Testes

```bash
mvn test
```
