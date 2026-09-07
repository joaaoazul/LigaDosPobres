# Guiões de fluxo, ponta a ponta

Testes que falam com a aplicação a correr, por HTTP, como um browser fala. O
`mvn test` cobre os serviços; isto cobre o que só aparece com a aplicação de pé:
sessões, autorização entre contas, concorrência e o que sobrevive a um reinício.

Foi assim que apareceram os dois problemas que a V14 e o `styles.css` corrigem.

## Como correr

Com uma base descartável e a aplicação de pé:

```bash
DB_URL=jdbc:postgresql://localhost:5432/qa DB_USER=qa DB_PASSWORD=qa \
  ADMIN_EMAIL=admin@teste.pt ADMIN_PASSWORD=passwordsegura1 mvn spring-boot:run

python3 testes-e2e/e2e.py 5           # o fluxo todo, cinco vezes
python3 testes-e2e/arestas.py 3       # limites, erros e autorização
python3 testes-e2e/dinheiro.py 3      # concorrência sobre dinheiro
python3 testes-e2e/corrida.py 8080 10 # fecho simultâneo da mesma jornada
python3 testes-e2e/varrimento.py 3    # segurança, validação, upload e administração
python3 testes-e2e/recuperar.py       # recuperação de password
```

Todos assumem a aplicação em `localhost:8081` (o `corrida.py` recebe a porta) e
um administrador `admin@teste.pt` / `passwordsegura1`, que é o que o
`ADMIN_EMAIL`/`ADMIN_PASSWORD` cria no primeiro arranque.

**Correm contra uma base descartável, nunca contra produção.** Criam contas,
ligas e dívidas, e não limpam nada — é de propósito: cada corrida usa um sufixo
aleatório e repetir contra a mesma base é precisamente o que apanha os erros de
"segunda vez".

O `recuperar.py` lê o link do log da aplicação (`/tmp/app.log`), que é onde o
`EnviadorParaLog` o escreve quando não há chave de email configurada.

## O que cada um cobre

| Guião | O que exercita |
| --- | --- |
| `e2e.py` | convite de gestor, registo, liga, equipas, convites de treinador (individual, em massa, ligar a conta existente), regra por tabela, jornadas de treino e oficiais, desempate, cobrança de época, pagamentos, desistência, vistas do treinador, autorização entre contas, mudança de password, terminar liga |
| `arestas.py` | liga cheia, pontuações inválidas, convites revogados e expirados, emitir em paralelo, registos recusados, empate na cobrança de época, posição para lá do fim da tabela, administração, recuperação |
| `dinheiro.py` | fechar a mesma jornada em paralelo, pagar o mesmo bloco em paralelo, lançar blocos em paralelo, blocos de várias jornadas |
| `corrida.py` | só a corrida do fecho, muitas vezes, a contar quantas cobranças saíram a dobrar |
| `varrimento.py` | CSRF, JSON mal formado, tipos trocados, limites de texto, unicode e HTML nos nomes, upload de logo (PNG, SVG, ficheiro grande), regra alterada a meio da época, precisão dos cêntimos, administração |
| `recuperar.py` | pedido, link, redefinição, corte das sessões abertas, código de uso único |
