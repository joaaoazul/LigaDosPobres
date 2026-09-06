# Manual do Quota

Guia de utilização da aplicação, para quem gere ligas e para quem treina
equipas. Não é preciso saber nada de informática para seguir este manual.

Para documentação técnica, ver [DESENVOLVIMENTO.md](DESENVOLVIMENTO.md) e
[API.md](API.md).

---

## 1. O que é o Quota

O Quota gere ligas amadoras: as equipas, as jornadas, a classificação e o
dinheiro que cada equipa deve à liga.

A ideia central é simples. Cada jornada dá uma classificação, e quem fica pior
classificado paga mais. A aplicação faz essas contas sozinha e mostra-te, a
qualquer momento, quanto já entrou no pote e quanto ainda falta receber.

### Os dois tipos de conta

**Gestor.** Cria a liga, inscreve as equipas, abre e fecha jornadas, define
quanto se paga e marca os pagamentos que recebeu. Vê apenas as suas próprias
ligas.

**Treinador.** Vê as ligas onde treina alguma equipa e o que a sua equipa deve.
Não altera nada. Um treinador só precisa de conta se quiser ver isto sozinho.
Se não tiver email, ou simplesmente não quiser conta, continua tudo a funcionar
com o gestor a tratar dele.

**A mesma pessoa pode ser as duas coisas com um só login.** Se geres uma liga e
ao mesmo tempo treinas uma equipa (na tua liga ou na de outra pessoa), não
precisas de duas contas. Ver a secção 2.3.

---

## 2. Entrar na aplicação

Não há registo aberto. Toda a gente entra por convite. Isto é de propósito:
evita que apareçam contas que ninguém sabe de onde vieram.

### 2.1 Criar conta de gestor

Precisas de um código de convite dado por um administrador.

1. Abre a aplicação e carrega em **Criar conta**.
2. Preenche o código do convite, o nome, o email e uma password com pelo menos
   10 caracteres.
3. Carrega em **Criar conta**. Ficas logo com sessão aberta.

Cada código serve uma vez só. Se te enganares a escrever, pede outro.

### 2.2 Criar conta de treinador

O convite de treinador é dado pelo gestor da liga onde treinas, não por um
administrador, e é sempre para uma equipa em concreto: "treinas o Bairro FC, na
Liga do Café".

1. **Abre o link** que o gestor te enviou. A página diz-te quem te convidou e
   para que equipa.
2. Preenche o nome, o email e a password.

Não tens de escrever código nenhum: ele vai dentro do link. Se o link já não
funcionar, o convite expirou ou foi revogado — pede outro ao gestor.

Uma conta criada assim **não pode criar ligas próprias**. Se precisares disso,
um administrador tem de te dar essa permissão.

### 2.3 Já tens conta e recebeste um convite de treinador

Não crias uma segunda conta. Abre o link do convite já com sessão iniciada: a
página oferece **Aceitar com a minha conta** e a equipa passa a aparecer na que
já tens. Se ainda não tinhas sessão, entra primeiro e volta a abrir o link.

A partir daí, a mesma conta gere as tuas ligas e mostra as equipas que treinas.

(O caminho antigo continua a existir: em **Password**, na secção **Ligar convite
de treinador**, colando o código à mão.)

### 2.4 Mudar a password

Em **Password**. Tens de saber a password actual. Depois de mudares, a sessão
termina e voltas a entrar com a nova, em todos os sítios onde estavas.

### 2.5 Se te esqueceste da password

Na página de entrada, carrega em **Recupera-a aqui**. Escreves o email da conta
e recebes uma mensagem com um link.

O link serve **uma vez** e **expira ao fim de uma hora**. Se demorares, ou se o
usares e precisares de outro, é só pedir de novo.

Alguns pormenores que valem a pena saber:

- A página diz sempre a mesma coisa, quer o email tenha conta quer não. É de
  propósito: assim ninguém pode usar este ecrã para descobrir quem está
  registado na aplicação.
- Se não receber nada, confirma a pasta de spam e confirma que escreveste o
  email com que te registaste, e não outro qualquer.
- Ao redefinires a password, **todas as sessões dessa conta são terminadas**,
  incluindo as de outros telemóveis ou computadores. Se estás a recuperar
  precisamente porque desconfias que alguém entrou na tua conta, é isto que
  põe essa pessoa fora.

> **Se o email da tua conta estiver mal escrito**, a mensagem nunca chega, e
> nesse caso só um administrador pode corrigir o email (secção 9.2).

---

## 3. Criar e gerir uma liga

### 3.1 Criar

Na coluna da esquerda, escreve o nome e o número máximo de equipas (entre 1 e
45) e carrega em **Criar liga**.

### 3.2 O cabeçalho da liga

Depois de escolheres uma liga, o cabeçalho mostra o nome, o logo e um conjunto
de etiquetas: se está activa, quantas equipas tem, quantas estão activas e
quantas jornadas já existem.

Podes pôr o **logo** da liga carregando no quadrado à esquerda do nome. Aceita
PNG, JPEG e WEBP até 1 MB. Para o tirar, usa o × que aparece no canto.

### 3.3 Terminar uma liga

O botão **Terminar liga** fecha-a definitivamente. Depois disso não se
acrescentam equipas nem se abrem jornadas, e o logo também deixa de poder ser
alterado. A liga continua visível, com tudo o que lá está.

Não há forma de voltar atrás.

---

## 4. Equipas

No separador **Equipas**.

### 4.1 Inscrever

Escreve o nome da equipa e o nome do treinador, e carrega em **Adicionar
equipa**.

Duas coisas acontecem ao mesmo tempo:

- Se a liga tiver uma regra de dívida com valor de inscrição, esse valor é
  cobrado à equipa nesse momento.
- Não podes ter duas equipas com o mesmo nome na mesma liga. Se tentares, a
  aplicação recusa. Nomes repetidos tornavam a classificação impossível de ler.

### 4.2 Convidar o treinador

A coluna **Conta** diz-te em que pé está cada equipa:

| O que lá está | O que quer dizer |
| --- | --- |
| Sem convite | ninguém foi convidado ainda |
| Convite pendente | há um convite por usar (passa o rato por cima para veres até quando) |
| Ligada | o treinador já tem conta e vê a equipa |

O botão **Convidar treinador** cria o convite e **copia o link** para a área de
transferência — é esse link que envias, por WhatsApp ou como quiseres. O
treinador abre-o e cria conta (secção 2.2) ou liga-o à que já tem (secção 2.3).

Carregar outra vez no mesmo botão — agora **Copiar link** — dá-te o mesmo link,
não um segundo convite. Isso é de propósito: dois convites válidos para a mesma
equipa eram duas chaves da mesma porta, e a primeira ficava por aí sem ninguém
saber onde.

**Os convites duram 30 dias.** Passado esse prazo o link deixa de funcionar e
basta convidar outra vez.

**Revogar** invalida o link que já deste — usa-o se o mandaste para o sítio
errado. A seguir podes emitir um novo.

**Editar**, ao lado do nome do treinador, corrige o nome que escreveste ao
inscrever a equipa. É o nome que a liga mostra, mesmo depois de o treinador ter
conta: o nome da conta é dele, este é o teu rótulo.

**Desligar conta** corta o acesso de quem lá estava — para quando um treinador
sai da equipa. A dívida da equipa não vai atrás: é da equipa, não de quem a
treina. Depois disso podes convidar o treinador novo.

### 4.3 Desistência

O botão **Desistência** marca a equipa como desistente. Ela deixa de contar
para as jornadas seguintes e aparece riscada na classificação.

**O que ela já devia continua a dever.** A dívida de uma equipa desistente
continua a contar no pote da liga. Isso é de propósito: o dinheiro continua a
fazer falta.

---

## 5. Jornadas

No separador **Jornadas**.

### 5.1 Treino e oficial

As primeiras cinco jornadas são de **treino**, e as seguintes são **oficiais**.
Cada tipo tem a sua própria numeração, por isso vais ver uma "Jornada 1" de
treino e, mais tarde, uma "Jornada 1" oficial. É normal.

Para efeitos de dinheiro e de classificação, as duas contam igual.

### 5.2 O ciclo de uma jornada

**Só pode haver uma jornada por fechar de cada vez.** Enquanto não fechares a
que está aberta, não abres a seguinte.

1. **Abrir jornada.**
2. **Inserir as pontuações** de cada equipa activa e carregar em **Guardar** em
   cada linha.
3. **Fechar jornada.** As posições são atribuídas por ordem decrescente de
   pontuação.

Enquanto a jornada está aberta, a tabela mostra-te uma previsão: como ficaria a
classificação e quanto pagaria cada equipa se fechasses agora.

### 5.3 Empates

Se, ao fechar, ficarem equipas com a mesma pontuação, **a jornada não fecha**.
Fica em estado *Desempate*, a amarelo, e aparece um painel por baixo da tabela.

Nesse painel, as equipas empatadas aparecem agrupadas por pontuação. Usa as
setas ↑ e ↓ para as ordenares, da melhor para a pior, e carrega em **Confirmar
desempate**. Só então a jornada fecha.

Isto não é uma chatice desnecessária: a posição decide quanto cada equipa paga.
Se duas equipas ficassem empatadas em quinto, ambas pagariam o valor do quinto
lugar e a liga perdia a diferença para o sexto.

Enquanto o empate não estiver resolvido:

- as pontuações ficam trancadas,
- não se abre a jornada seguinte,
- **não é cobrado nada**.

> **Atenção:** depois de uma jornada fechar, as pontuações não se alteram. Se
> te enganaste numa pontuação, confirma antes de fechar. Não existe forma de
> reabrir uma jornada.

### 5.4 A coluna "Valor"

Mostra quanto aquela jornada em concreto pesa para cada equipa, segundo a regra
da liga. Não é o total da equipa: quando o bloco fechar, este valor soma-se ao
das outras jornadas do mesmo bloco.

---

## 6. Classificação

No separador **Classificação**. Soma os pontos de todas as jornadas e ordena as
equipas. O primeiro lugar aparece destacado, e as equipas desistentes ficam no
fim, riscadas.

Se duas equipas tiverem os mesmos pontos no total da época, a tabela ordena-as
por nome. Ao contrário do que acontece nas jornadas, aqui não há desempate
manual, porque esta tabela não decide dinheiro nenhum.

---

## 7. O dinheiro

No separador **Dívidas**.

### 7.1 A regra da liga

É aqui que dizes como a tua liga cobra. Enquanto não definires uma regra, a
aplicação não cobra nada sozinha e tu lanças tudo à mão.

| Campo | O que significa |
|---|---|
| **Inscrição** | Valor cobrado uma vez, quando a equipa entra na liga. |
| **Valor inicial** | O que paga quem fica no melhor escalão. |
| **Incremento** | Quanto sobe de escalão para escalão. |
| **Equipas por escalão** | Quantas posições cabem em cada escalão. |
| **Valor máximo** | Tecto: nenhuma equipa paga mais do que isto por jornada. |
| **Jornadas por bloco** | De quantas em quantas jornadas se fecha uma conta. |

**Exemplo.** Valor inicial 0,00, incremento 0,50 e 5 equipas por escalão:

- 1º ao 5º lugar: 0,00 €
- 6º ao 10º: 0,50 €
- 11º ao 15º: 1,00 €

e assim por diante, até ao valor máximo.

> Se puseres 5 equipas por escalão, as cinco primeiras pagam todas o mesmo. Não
> é um erro. Se quiseres que cada posição pague um valor diferente, põe 1 equipa
> por escalão.

Podes alterar a regra a meio da época. As jornadas já cobradas não são
recalculadas: a alteração vale daí para a frente.

### 7.2 Blocos

Um **bloco** é uma conta fechada. Há dois tipos:

- **Inscrição:** a entrada na liga, cobrada uma vez.
- **Período:** o conjunto de jornadas definido em "jornadas por bloco".

Quando fecham jornadas suficientes, a aplicação fecha o bloco sozinha e cobra a
cada equipa **a soma do que ela deveu em cada uma dessas jornadas**. Uma equipa
que correu mal em duas jornadas do bloco paga as duas.

Se a liga não tiver regra definida, usa o campo **Novo bloco (valor)** para
lançares um valor à mão.

Um bloco de 0,00 € aparece já como pago. Não há nada para receber, por isso a
aplicação não te chateia com isso.

### 7.3 Marcar pagamentos

Escolhe a equipa na lista à esquerda e depois:

- **Marcar pago** numa linha, para um bloco de cada vez.
- **Marcar tudo pago**, para liquidar tudo o que aquela equipa deve.

> **Atenção:** "pago" quer dizer que **tu** confirmaste que recebeste. A
> aplicação não movimenta dinheiro nenhum nem se liga a bancos ou a MB Way. É um
> registo, não um sistema de pagamentos.

### 7.4 O pote da liga

No cabeçalho da liga, por baixo das etiquetas, aparecem três números:

- **Total:** tudo o que já foi lançado nesta liga.
- **Pago:** o que já deste por recebido.
- **Dívidas:** o que falta receber.

Total é sempre igual a Pago mais Dívidas. Inclui as inscrições, os blocos de
período, e o que as equipas desistentes deixaram por pagar.

Se a liga ainda não cobrou nada, o mostrador não aparece.

---

## 8. Se treinas uma equipa

Duas páginas no topo da aplicação:

**As minhas ligas.** A classificação e as jornadas das ligas onde treinas.
Só de leitura, não alteras nada. Vês também o pote da liga, que é um valor do
conjunto e não diz quanto cada equipa deve.

**As minhas dívidas.** O que cada equipa que treinas ainda deve, em todas as
ligas, com o detalhe bloco a bloco e um total geral no topo.

Quem marca os pagamentos é sempre o gestor da liga.

---

## 9. Administração

Só para contas de administrador, no botão **Administração**.

### 9.1 Convites de gestor

Cria códigos para novas contas de gestor. Podes deixar uma nota (para te
lembrares a quem o deste) e um prazo de validade em dias. O código só é
visível enquanto não for usado: copia-o e entrega-o.

**Revogar** invalida um convite que ainda não tenha sido usado.

### 9.2 Contas

A tabela lista todas as contas. Em cada linha podes:

- **Tornar admin** ou **Despromover**.
- **Desativar** ou **Reativar**. Uma conta desactivada perde o acesso de
  imediato, mesmo que a pessoa esteja com a sessão aberta. As ligas dela não são
  apagadas.
- **Bloquear** ou **Permitir criar ligas**. Serve para contas que entraram por
  convite de treinador e que queiras promover, ou o contrário.
- **Corrigir email**. Existe para desenrascar quem escreveu o email mal no
  registo: sem isto essa conta não recebe o link de recuperação e fica presa,
  porque só o próprio muda o email e o próprio já não consegue entrar. Ao
  corrigires, as sessões dessa conta terminam.

Não podes alterar a tua própria conta. É deliberado, para ninguém se fechar
fora da aplicação por engano.

> **Nota sobre contas antigas:** contas criadas antes de a permissão "cria
> ligas" existir ficaram todas com ela ligada, incluindo contas de treinador.
> Vale a pena olhar para essa coluna uma vez e corrigir o que não fizer sentido.

---

## 10. O que a aplicação não faz

Para não haver surpresas:

- **Não movimenta dinheiro.** Marcar como pago é um registo do que recebeste.
- **Não confirma o email no registo.** O registo é por convite, e ninguém se
  regista sem um. Mas também ninguém verifica se o email que escreveste está
  certo, e só dás por isso quando precisares de recuperar a password.
- **Não reabre jornadas.** Depois de fechada, uma jornada não se altera.
- **Não desfaz uma liga terminada.**
- **Não apaga equipas.** Quem sai marca-se como desistente, e o histórico fica.
- **Não desempata a classificação geral.** Só a de cada jornada, que é a que
  decide dinheiro.

---

## 11. Problemas comuns

**"Já existe uma equipa com este nome nesta liga."**
Escolhe outro nome. Duas equipas com o mesmo nome tornavam as tabelas
impossíveis de ler.

**"Já existe uma jornada aberta nesta liga."**
Fecha a que está aberta primeiro. Se ela estiver em desempate, resolve o
empate.

**"Esta jornada está à espera de desempate."**
Vai ao separador Jornadas, escolhe essa jornada e ordena as equipas empatadas.

**"Não tens permissão para esta operação."**
A tua conta não pode criar ligas. Pede a um administrador.

**"A tua conta foi desativada."**
Fala com um administrador.

**"A password desta conta foi alterada. Entra outra vez."**
A password foi mudada noutro sítio, ou recuperada por email, e as sessões
antigas deixaram de valer. Entra com a password nova. Se não foste tu a
mudá-la, recupera-a já (secção 2.5) e escolhe outra.

**Pedi o link de recuperação e não recebi nada.**
Confirma a pasta de spam. Confirma que escreveste o email com que te
registaste. Se pediste várias vezes seguidas, só as três primeiras de cada
hora são enviadas. Se mesmo assim nada, o email da conta pode estar mal
escrito, e aí só um administrador o corrige.

**"O link de recuperação não é válido ou já expirou."**
Os links servem uma vez e duram uma hora. Pede outro.

**"Isto foi alterado por outro pedido ao mesmo tempo. Tenta outra vez."**
Duas alterações à mesma coisa ao mesmo tempo, provavelmente em dois
separadores. Repete a operação.

**Fui abaixo a meio e perdi a sessão.**
As sessões não sobrevivem a uma actualização da aplicação. Volta a entrar, não
se perdeu nada do que já tinhas gravado.
