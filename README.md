# Liga dos Pobres

Aplicação de gestão de ligas amadoras: ligas, equipas, jornadas e classificação.
Cada gestor tem conta própria e vê apenas as suas ligas.

Java 21 · Spring Boot · PostgreSQL · Flyway · interface em HTML e JavaScript,
sem passo de build no frontend.

## Correr localmente

```bash
docker compose up -d          # PostgreSQL na porta 5432
mvn spring-boot:run
```

Abre <http://localhost:8080>. Sem sessão és reencaminhado para o registo/login.

Requisitos: JDK 21+, Maven e Docker (ou um PostgreSQL já instalado).

## Configuração

Nenhuma credencial está no repositório. A aplicação lê estas variáveis de
ambiente e, quando não existem, usa os valores de desenvolvimento local:

| Variável | Omissão | Para que serve |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ligadospobres` | Ligação à base de dados |
| `DB_USER` | `liga` | Utilizador da base de dados |
| `DB_PASSWORD` | vazio | Password da base de dados |
| `DB_POOL` | `5` | Máximo de ligações simultâneas |
| `PORT` | `8080` | Porta HTTP |
| `COOKIE_SECURE` | `false` | **Põe `true` em produção**: restringe o cookie de sessão a HTTPS |

## Publicar

1. Cria uma base de dados PostgreSQL gerida (Neon, Supabase, Railway).
2. Faz deploy do `Dockerfile` num serviço que corra contentores (Railway, Render, Fly).
3. Define as variáveis acima no painel do serviço. Acrescenta `?sslmode=require`
   ao `DB_URL` e põe `COOKIE_SECURE=true`.
4. Aponta o *health check* do serviço para `/actuator/health`.

As migrações do Flyway correm sozinhas no arranque. **Um ficheiro de migração já
aplicado nunca se edita**: o Flyway guarda uma assinatura de cada um e recusa
arrancar se ela mudar. Correções fazem-se com um `V2__…` novo.

## Segurança

- Passwords guardadas com BCrypt. A password em claro nunca é escrita em disco nem em log.
- Sessão por cookie `HttpOnly`, com `Secure` em produção e proteção CSRF por token.
- **Toda a autorização é feita na consulta**, não numa verificação à parte: os
  repositórios só devolvem registos do gestor autenticado
  (`buscarPorIdEGestor`). Um esquecimento devolve 404, nunca dados de outro gestor.
- As rotas de jornada estão aninhadas na liga para que a liga seja sempre
  resolvida — e a sua posse verificada — antes de qualquer operação.

## Testes

```bash
mvn test
```

Os testes de serviço usam as implementações em memória dos repositórios, por isso
correm em segundos e não precisam de base de dados.

## Por implementar

- Dívidas e blocos de dívida (`DividaService` é ainda um esboço)
- Resolução manual de empates (`DesempateService`); equipas empatadas ficam com
  a mesma posição na jornada
- Recuperação de password e verificação de email no registo
