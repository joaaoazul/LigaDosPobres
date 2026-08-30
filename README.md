# Liga dos Pobres

Aplicação de gestão de ligas amadoras: ligas, equipas, jornadas e classificação.
Cada gestor tem conta própria e vê apenas as suas ligas.

Java 21 · Spring Boot · PostgreSQL · Flyway · interface em HTML e JavaScript,
sem passo de build no frontend.

## Correr localmente

```bash
cp .env.example .env          # preenche as passwords e o REGISTO_CODIGO
set -a; source .env; set +a   # exporta as variáveis para esta shell
docker compose up -d          # PostgreSQL na porta 5432
mvn spring-boot:run
```

O `.env` está no `.gitignore` e nunca deve ser versionado. O `.env.example` é o
modelo, e é esse que vai para o repositório — sem valores reais lá dentro.

Abre <http://localhost:8080>. Sem sessão és reencaminhado para o registo/login.

Requisitos: JDK 21+, Maven e Docker (ou um PostgreSQL já instalado).

## Configuração

Nenhuma credencial está no repositório, nem sequer as de desenvolvimento local. A aplicação lê estas variáveis de
ambiente e, quando não existem, usa os valores de desenvolvimento local:

| Variável | Omissão | Para que serve |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ligadospobres` | Ligação à base de dados |
| `DB_USER` | `liga` | Utilizador da base de dados |
| `DB_PASSWORD` | vazio | Password da base de dados |
| `DB_POOL` | `5` | Máximo de ligações simultâneas |
| `PORT` | `8080` | Porta HTTP |
| `COOKIE_SECURE` | `false` | **Põe `true` em produção**: restringe o cookie de sessão a HTTPS |
| `ADMIN_EMAIL` | vazio | Primeiro administrador, criado só se não existir nenhum |
| `ADMIN_PASSWORD` | vazio | Password desse administrador. Mínimo 10 caracteres |

## Publicar no teu servidor

Pressupõe um VPS com IP público e o domínio na Cloudflare.

**1. Certificado de origem.** No painel da Cloudflare, *SSL/TLS → Origin Server →
Create Certificate*. Guarda os dois ficheiros em `./certs/origin.pem` e
`./certs/origin.key`. A pasta `certs/` está no `.gitignore`.

**2. Modo de TLS: `Full (strict)`.** Não `Flexible`. Em `Flexible` o troço entre
a Cloudflare e o teu servidor vai em HTTP simples pela internet, e o cookie de
sessão marcado como `Secure` deixa de funcionar de maneira difícil de diagnosticar.

**3. DNS.** Um registo `A` para o IP do servidor, com a nuvem laranja ligada
(proxied). Assim o IP do servidor não fica público.

**4. Domínio no Caddyfile.** Troca `liga.exemplo.pt` pelo teu.

**5. Variáveis.** `cp .env.example .env` e preenche `POSTGRES_PASSWORD`,
`ADMIN_EMAIL` e `ADMIN_PASSWORD`.

**6. Arrancar.**

```bash
docker compose -f compose.prod.yml -f compose.caddy.yml up -d --build
```

**7. Firewall.** Só 22, 80 e 443. A aplicação escuta em `127.0.0.1:8080` e a
base de dados não é exposta.

**8. Backups no cron.** Sem isto não há plano de recuperação nenhum:

```
0 4 * * * cd /caminho/LigaDosPobres && ./scripts/backup.sh >> backups/backup.log 2>&1
```

Copia os backups para fora do servidor. Um backup que só existe na máquina que
pode arder não é um backup.

### Alojar numa plataforma em vez do teu servidor

Nada aqui é específico do teu servidor: é um contentor Docker, uma base de dados
PostgreSQL e variáveis de ambiente. A plataforma trata do TLS, dispensando o
Caddy e o certificado.

**No Railway**, cria o serviço a partir do `Dockerfile`, junta um PostgreSQL e
define as variáveis. A base de dados do Railway anuncia-se em formato
`postgres://`, que o Java não entende — tens de montar o URL JDBC a partir das
peças:

```
DB_URL          jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USER         ${{Postgres.PGUSER}}
DB_PASSWORD     ${{Postgres.PGPASSWORD}}
COOKIE_SECURE   true
FORWARD_HEADERS framework
ADMIN_EMAIL     o-teu-email
ADMIN_PASSWORD  uma-password-longa
```

Aponta o *health check* do serviço a `/actuator/health`. A porta vem da variável
`PORT`, que a aplicação já respeita.

O `compose.prod.yml`, o `Caddyfile` e o script de backup ficam por usar — só
servem quando fores para o teu servidor.

### Mudar de plataforma para servidor próprio

```bash
# na plataforma
pg_dump "$DATABASE_URL" --clean --if-exists | gzip > mudanca.sql.gz

# no servidor, com o compose.prod.yml já a correr
gunzip -c mudanca.sql.gz | docker compose -f compose.prod.yml exec -T db psql -U liga -d ligadospobres
```

Depois muda o registo DNS e acabou. Nada no código muda.

## Contas de gestor

Há dois papéis. Um **gestor** cria e gere as suas ligas. Um **administrador**
faz o mesmo e ainda administra contas e convites, em `/admin.html`.

### Primeiro arranque

Numa base de dados vazia não há forma de entrar: não existem convites porque não
existe quem os crie. Define `ADMIN_EMAIL` e `ADMIN_PASSWORD` e o primeiro
administrador é criado no arranque. Depois de entrares, muda a password e remove
`ADMIN_PASSWORD` do ambiente.

Estas variáveis só têm efeito enquanto não existir nenhum administrador ativo:
numa instalação já povoada não fazem nada, portanto não servem de porta das
traseiras.

### Convites

O registo é sempre por convite. Cada convite serve **uma vez**, pode ter prazo
e pode ser revogado enquanto não for usado. O código só é mostrado enquanto
estiver por usar.

Os convites nunca são apagados: fica registado quem entrou com qual, que é o que
permite perceber mais tarde como é que uma conta apareceu.

### Desativar contas

Uma conta desativada é bloqueada de imediato, mesmo que a pessoa já tenha sessão
aberta &mdash; cada pedido confirma que a conta continua ativa. As ligas dessa
pessoa não são apagadas, para não se perder o histórico das provas.

Um administrador não pode alterar a própria conta. É isso que garante que nunca
ficas sem ninguém com acesso à administração.

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
- Limite de tentativas de login: hoje nada impede tentativas repetidas de
  adivinhar uma password, além da lentidão própria do BCrypt
