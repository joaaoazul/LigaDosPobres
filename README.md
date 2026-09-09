# Quota

Aplicação de gestão de ligas amadoras: ligas, equipas, jornadas e classificação.
Cada gestor tem conta própria e vê apenas as suas ligas.

Java 21 · Spring Boot · PostgreSQL · Flyway · interface em HTML e JavaScript,
sem passo de build no frontend.

## Documentação

| Documento | Para quem | PDF |
| --- | --- | --- |
| [Manual de utilização](docs/MANUAL.md) | gestores | [PDF](docs/pdf/MANUAL.pdf) |
| [Manual do treinador](docs/MANUAL_TREINADOR.md) | treinadores | [PDF](docs/pdf/MANUAL_TREINADOR.pdf) |
| [Manual técnico](docs/DESENVOLVIMENTO.md) | quem mexe no código | [PDF](docs/pdf/DESENVOLVIMENTO.pdf) |
| [Referência da API](docs/API.md) | integrações e frontend | [PDF](docs/pdf/API.pdf) |

Os PDF são gerados a partir dos markdown com `docs/gerar-pdf.py`; as instruções
estão no cabeçalho do script.

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
| `EMAIL_CHAVE` | vazio | Chave de API do Resend, para os emails de recuperação de password e de convite |
| `EMAIL_REMETENTE` | vazio | Remetente, num domínio verificado no Resend |
| `APP_URL` | `http://localhost:8080` | Endereço público, usado nos links dos emails (recuperação e convite) |

Sem `EMAIL_CHAVE` e `EMAIL_REMETENTE` a aplicação arranca à mesma, mas as
mensagens vão para o log em vez de serem enviadas: ninguém consegue recuperar a
password, e os convites de treinador têm de ser entregues à mão pelo link. O arranque avisa quando é esse o caso. Em desenvolvimento é o que se
quer: o link sai na consola e o fluxo experimenta-se sem servidor de email.

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

**No Railway**, por esta ordem:

1. **Cria primeiro a base de dados.** No projeto: *New → Database → Add PostgreSQL*.
   Sem ela a aplicação arranca, não encontra o PostgreSQL e morre em ciclo.
2. Cria o serviço da aplicação a partir do repositório; o `railway.json` trata
   do resto.
3. Define as variáveis no serviço **da aplicação** (não no do PostgreSQL).

A base de dados do Railway anuncia-se em formato `postgres://`, que o Java não
entende — tens de montar o URL JDBC a partir das peças:

```
DB_URL          jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USER         ${{Postgres.PGUSER}}
DB_PASSWORD     ${{Postgres.PGPASSWORD}}
COOKIE_SECURE   true
FORWARD_HEADERS framework
ADMIN_EMAIL     o-teu-email
ADMIN_PASSWORD  uma-password-longa
```

`Postgres` nas referências acima é o **nome do serviço** no Railway. Se lhe
mudaste o nome, muda também as referências.

Não é preciso correr SQL nenhum: o Flyway cria o esquema no primeiro arranque.

A porta vem da variável `PORT`, que a aplicação já respeita.

**Se o arranque falhar com um erro de ligação ao PostgreSQL** (`PGStream`,
`ConnectionFactoryImpl`, `Socket.connect`), é quase sempre a rede privada do
Railway, que só existe em IPv6. Um nome que só tem endereços IPv6, como o
`postgres.railway.internal`, é usado pela JVM sem ser preciso opção nenhuma.

> **Não acrescentes `-Djava.net.preferIPv6Addresses=true`.** Já lá esteve e
> teve de sair. Essa opção só decide a ordem quando um nome tem endereços das
> duas famílias, não ajuda em nada um nome que só tem IPv6, e manda para IPv6
> tudo o resto, incluindo a API do Resend. Como o Railway não tem saída IPv6
> para a internet pública, os emails de recuperação de password falhavam todos
> com `Network is unreachable`.

Se o problema persistir, usa o endereço público da base de dados para
desbloquear:

```
DB_URL  jdbc:postgresql://${{Postgres.RAILWAY_TCP_PROXY_DOMAIN}}:${{Postgres.RAILWAY_TCP_PROXY_PORT}}/${{Postgres.PGDATABASE}}?sslmode=require
```

O endereço público sai da rede do Railway e volta a entrar, por isso é mais
lento e conta para o tráfego. Serve para confirmar que o resto está bem; depois
volta ao `PGHOST` interno.

O `compose.prod.yml` e o `Caddyfile` ficam por usar — só servem quando fores
para o teu servidor.

**Backups.** O `scripts/backup.sh` fala com o contentor do docker compose e por
isso não serve aqui; para uma plataforma usa-se o `scripts/backup-railway.sh`,
que só precisa de um URL de ligação:

```bash
# o DATABASE_PUBLIC_URL do serviço Postgres, no separador Variables
export DATABASE_URL='postgresql://utilizador:senha@host:porta/base'
./scripts/backup-railway.sh
```

Corre-o do teu computador e não da plataforma: um backup que só existe na
plataforma que pode desaparecer não é um backup. O ficheiro sai
`--no-owner --no-privileges`, por isso restaura tanto lá como no teu servidor,
onde o dono das tabelas tem outro nome.

Os backups automáticos do Railway (se o teu plano os tiver) não dispensam isto:
vivem na mesma conta que estás a tentar sobreviver.

**Verifica o primeiro, e depois um de vez em quando:**

```bash
./scripts/backup-railway.sh --verificar backups/ligadospobres-AAAAMMDD-HHMMSS.sql.gz
```

Restaura o ficheiro para uma base descartável, conta as linhas das tabelas que
interessam e diz em que migração o esquema ficou; no fim apaga a base, mesmo
que tenha rebentado a meio. Falha se o ficheiro não restaurar e falha se
restaurar vazio — é essa a diferença entre um backup e uma suposição. Precisa
de um PostgreSQL onde criar a base descartável; por omissão procura-o em
`localhost:5432`, e muda-se com `URL_VERIFICACAO`. **Nunca lhe apontes a base de
produção:** o restauro apaga o que estiver à frente.

Compara as contagens com o que a aplicação mostra. Se baterem certo, tens um
plano de recuperação; se nunca as comparaste, tens ficheiros.

### Mudar de plataforma para servidor próprio

```bash
# de onde alcances a plataforma
export DATABASE_URL='postgresql://...'
./scripts/backup-railway.sh

# no servidor, com o compose.prod.yml já a correr
gunzip -c backups/ligadospobres-AAAAMMDD-HHMMSS.sql.gz \
  | docker compose -f compose.prod.yml exec -T db psql -U liga -d ligadospobres
```

O script serve aqui melhor do que um `pg_dump` à mão porque leva
`--no-owner --no-privileges`: sem isso, o ficheiro traz o dono das tabelas da
plataforma agarrado e o restauro no teu servidor, onde o utilizador é o `liga`,
enche-se de erros por causa de um papel que ali não existe.

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

- Verificação de email no registo: o convite já trava o registo aberto, mas
  ninguém confirma que o email escrito está certo, e só se dá por isso quando
  faz falta recuperar a password
