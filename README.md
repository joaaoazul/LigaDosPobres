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

## Publicar

1. Cria uma base de dados PostgreSQL gerida (Neon, Supabase, Railway).
2. Faz deploy do `Dockerfile` num serviço que corra contentores (Railway, Render, Fly).
3. Define as variáveis acima no painel do serviço. Acrescenta `?sslmode=require`
   ao `DB_URL` e põe `COOKIE_SECURE=true`.
4. Aponta o *health check* do serviço para `/actuator/health`.

As migrações do Flyway correm sozinhas no arranque. **Um ficheiro de migração já
aplicado nunca se edita**: o Flyway guarda uma assinatura de cada um e recusa
arrancar se ela mudar. Correções fazem-se com um `V2__…` novo.

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
