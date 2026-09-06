"use strict";

const estado = {
    gestor: null,
    ligas: [],
    ligaId: null,
    detalhe: null,
    jornadaId: null,
    tab: "classificacao",
    // Muda a cada gravação/remoção de logo, para rebentar a cache da <img>.
    logoV: 0,
    // Dívidas: regra da liga (null se ainda não definida) e a dívida de cada
    // equipa, por id — carregadas só quando o separador é aberto.
    regraDivida: null,
    dividas: new Map(),
    equipaDividaId: null,
    // Ordem que o gestor está a montar para desfazer um empate, antes de a
    // confirmar. Só existe enquanto a jornada escolhida estiver em desempate.
    desempate: null
};

/* Emblema neutro para uma liga ainda sem logo — o mesmo anel+arco da marca,
   com as mesmas classes: a cor do acento é do CSS (--sinal), não repetida aqui,
   senão este era o único sinal da aplicação que não seguia o tema. */
const EMBLEMA_LIGA = `<svg class="emblema" viewBox="0 0 32 32" aria-hidden="true" focusable="false"><circle cx="16" cy="16" r="12" fill="none" stroke="currentColor" stroke-width="2.5" opacity="0.28"/><path class="arco" d="M16 4a12 12 0 0 1 10.392 6" fill="none" stroke-width="2.5" stroke-linecap="round"/></svg>`;

/* ---------------------------------------------------------------- API ---- */

function tokenCsrf() {
    const par = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
    return par ? decodeURIComponent(par.split("=").slice(1).join("=")) : null;
}

async function api(caminho, opcoes = {}) {
    // Num envio multipart é o browser que tem de definir o Content-Type, porque
    // só ele sabe o boundary; defini-lo aqui partia o upload.
    const cabecalhos = opcoes.body instanceof FormData ? {} : { "Content-Type": "application/json" };
    const token = tokenCsrf();
    if (token) {
        cabecalhos["X-XSRF-TOKEN"] = token;
    }

    const resposta = await fetch(caminho, { headers: cabecalhos, ...opcoes });

    // Sessão expirada ou inexistente: volta ao login em vez de falhar em silêncio.
    if (resposta.status === 401) {
        window.location.href = "/login.html";
        throw new Error("Sessão terminada.");
    }

    if (resposta.status === 204) {
        return null;
    }

    const corpo = await resposta.json().catch(() => null);

    if (!resposta.ok) {
        throw new Error((corpo && corpo.mensagem) || `Erro ${resposta.status}`);
    }
    return corpo;
}

/* ------------------------------------------------------------ auxiliares -- */

const $ = (seletor) => document.querySelector(seletor);

function texto(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, (c) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
}

let temporizadorAlerta = null;

function mostrarAlerta(mensagem, tipo = "erro") {
    const alerta = $("#alerta");
    alerta.textContent = mensagem;
    // "erro" é o estilo base da caixa; os outros tipos são variantes.
    alerta.className = `alerta ${tipo === "erro" ? "" : tipo}`;
    clearTimeout(temporizadorAlerta);
    temporizadorAlerta = setTimeout(() => alerta.classList.add("oculto"), 4500);
}

async function executar(acao) {
    try {
        await acao();
    } catch (erro) {
        mostrarAlerta(erro.message);
    }
}

/* A área de transferência não existe fora de https (nem no localhost de alguns
   browsers), e quando existe pode recusar se a janela não estiver em foco.
   Devolve se conseguiu, para a mensagem não prometer uma cópia que não houve. */
async function copiar(valor) {
    if (!navigator.clipboard) {
        return false;
    }
    try {
        await navigator.clipboard.writeText(valor);
        return true;
    } catch {
        return false;
    }
}

function plural(n, singular, pluralForma) {
    return `${n} ${n === 1 ? singular : pluralForma}`;
}

function badgeEstado(estadoTexto) {
    const cores = {
        ATIVA: "verde",
        DESATIVADA: "vermelho",
        DESISTENTE: "vermelho",
        ABERTA: "verde",
        FECHADA: "azul",
        DESEMPATE: "amarelo",
        TREINO: "amarelo",
        OFICIAL: "azul",
        PENDENTE: "amarelo",
        RESOLVIDA: "verde"
    };
    const nomes = { SEM_DIVIDA: "Sem dívida" };
    return `<span class="badge ${cores[estadoTexto] || ""}">${texto(nomes[estadoTexto] || estadoTexto)}</span>`;
}

function badgeTipoBloco(tipo) {
    return tipo === "INSCRICAO"
        ? `<span class="badge azul">Inscrição</span>`
        : `<span class="badge">Período</span>`;
}

function formatoMoeda(valor) {
    return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(valor ?? 0);
}

/* Espelha ClassificacaoService.calcularValor no backend: o valor do escalão
   para esta posição, segundo a regra da liga. Usado só para mostrar uma
   prévia (a cobrança a sério continua a ser feita no servidor). */
function calcularValorEscalao(regra, posicao) {
    const escalao = Math.floor((posicao - 1) / regra.equipasPorEscalao);
    const valor = regra.valorInicial + regra.incremento * escalao;
    return Math.min(valor, regra.valorMaximo);
}

/* Espelha o critério de JornadaService.fecharJornada: ordena por pontuação
   desta jornada e dá a mesma posição a quem empata. Usado para prever, numa
   jornada ainda aberta, a posição que cada equipa vai ficar quando fechar. */
function calcularPosicoesPreview(resultados) {
    const ordenados = resultados.slice().sort((a, b) => b.pontuacao - a.pontuacao);
    const posicoes = new Map();
    let posicaoAtual = 0;
    let pontuacaoAnterior = null;
    ordenados.forEach((resultado, indice) => {
        if (pontuacaoAnterior === null || resultado.pontuacao !== pontuacaoAnterior) {
            posicaoAtual = indice + 1;
            pontuacaoAnterior = resultado.pontuacao;
        }
        posicoes.set(resultado.equipaId, posicaoAtual);
    });
    return posicoes;
}

/* ------------------------------------------------------------- carregar --- */

async function carregarLigas() {
    estado.ligas = await api("/api/ligas");
    desenharLigas();
}

async function carregarDetalhe() {
    if (!estado.ligaId) {
        return;
    }
    estado.detalhe = await api(`/api/ligas/${estado.ligaId}`);

    const jornadas = estado.detalhe.jornadas;
    if (!jornadas.some((j) => j.id === estado.jornadaId)) {
        const aberta = jornadas.find((j) => j.estado !== "FECHADA");
        estado.jornadaId = aberta ? aberta.id : (jornadas.length ? jornadas[jornadas.length - 1].id : null);
    }

    // A regra de dívida é usada tanto na tab Jornadas (coluna de valor) como
    // na tab Dívidas, por isso carrega-se aqui, uma vez só. Sem regra
    // definida o pedido dá 404 — não é um erro, é o estado normal de uma
    // liga que ainda cobra tudo à mão.
    estado.regraDivida = await api(`/api/ligas/${estado.ligaId}/regra-divida`).catch(() => null);

    desenharDetalhe();
}

/**
 * Um sítio só para "os dados mudaram": volta a ler o que o servidor sabe e
 * redesenha tudo.
 *
 * Toda a acção que muda alguma coisa chama isto, em vez de cada uma decidir
 * por si o que refrescar. Enquanto essa decisão era de cada handler, faltava
 * sempre um pedaço: o painel do desempate ficou com a ordem velha, e o
 * mostrador do pote não mexia depois de marcar um pagamento. Custa um ou dois
 * pedidos a mais por acção, e à escala desta aplicação isso não se nota.
 *
 * As dívidas só são lidas se a tab estiver aberta — são um pedido por equipa.
 */
async function recarregar() {
    await carregarLigas();
    await carregarDetalhe();
    await atualizarDividasSeAbertas();
}

/* -------------------------------------------------------------- desenho --- */

function desenharLigas() {
    const lista = $("#lista-ligas");

    if (!estado.ligas.length) {
        lista.innerHTML = `<li class="ajuda">Ainda não existem ligas.</li>`;
        return;
    }

    lista.innerHTML = estado.ligas.map((liga) => `
        <li>
            <button class="cartao-liga ${liga.id === estado.ligaId ? "selecionado" : ""}" data-liga="${liga.id}">
                <strong>${texto(liga.nome)}</strong>
                <small>${liga.equipasAtivas}/${liga.maxEquipas} equipas &middot; ${plural(liga.totalJornadas, "jornada", "jornadas")}</small>
                ${badgeEstado(liga.estado)}
            </button>
        </li>
    `).join("");
}

function desenharDetalhe() {
    const detalhe = estado.detalhe;

    $("#sem-liga").classList.toggle("oculto", Boolean(detalhe));
    $("#detalhe-liga").classList.toggle("oculto", !detalhe);

    if (!detalhe) {
        return;
    }

    const liga = detalhe.liga;
    const desativada = liga.estado !== "ATIVA";

    desenharLogo(liga, desativada);

    $("#liga-titulo").textContent = liga.nome;
    $("#liga-badges").innerHTML = [
        badgeEstado(liga.estado),
        `<span class="badge">${liga.totalEquipas}/${liga.maxEquipas} equipas</span>`,
        `<span class="badge">${liga.equipasAtivas} ativas</span>`,
        `<span class="badge">${plural(liga.totalJornadas, "jornada", "jornadas")}</span>`
    ].join("");

    $("#btn-terminar").disabled = desativada;
    $("#btn-abrir-jornada").disabled = desativada;

    desenharPote(detalhe.pote);
    desenharClassificacao(detalhe.classificacao);
    desenharEquipas(detalhe.equipas, desativada);
    desenharJornadas(detalhe.jornadas);
    desenharJornadaSelecionada();
}

/* O dinheiro da liga inteira: o que já foi lançado, o que o gestor deu por
   pago, e o que falta receber. Some-se numa liga que ainda não cobra nada —
   três zeros no cabeçalho não dizem nada a ninguém. */
function desenharPote(pote) {
    const mostrador = $("#mostrador-pote");
    const temDinheiro = Boolean(pote) && Number(pote.total) > 0;

    mostrador.classList.toggle("oculto", !temDinheiro);
    if (!temDinheiro) {
        mostrador.innerHTML = "";
        return;
    }

    mostrador.innerHTML = `
        <div class="valor-pote">
            <span class="rotulo-pote">Total</span>
            <strong class="numero">${formatoMoeda(pote.total)}</strong>
        </div>
        <div class="valor-pote pago">
            <span class="rotulo-pote">Pago</span>
            <strong class="numero">${formatoMoeda(pote.pago)}</strong>
        </div>
        <div class="valor-pote por-pagar">
            <span class="rotulo-pote">Dívidas</span>
            <strong class="numero">${formatoMoeda(pote.porPagar)}</strong>
        </div>`;
}

/* O logo é a identidade da liga (do gestor), mostrada aqui, no seu espaço.
   Numa liga desativada não se mexe — como em tudo o resto. A imagem leva
   alt="" porque quem a nomeia é o aria-label do botão que a contém. */
function desenharLogo(liga, desativada) {
    const alvo = $("#liga-logo");
    const remover = $("#btn-logo-remover");

    alvo.disabled = desativada;
    if (liga.temLogo) {
        alvo.innerHTML = `<img src="/api/ligas/${liga.id}/logo?v=${estado.logoV}" alt="">`;
        remover.classList.toggle("oculto", desativada);
    } else {
        alvo.innerHTML = EMBLEMA_LIGA;
        remover.classList.add("oculto");
    }
}

function desenharClassificacao(classificacao) {
    if (!classificacao.length) {
        $("#tabela-classificacao").innerHTML = `<p class="ajuda">Sem equipas para classificar.</p>`;
        return;
    }

    // O treinador vive dentro da célula da equipa: é um atributo dela, não um
    // eixo independente. Assim a tabela passa de cinco colunas a quatro e
    // sobra largura para o que interessa — a posição e os pontos.
    $("#tabela-classificacao").innerHTML = `
        <table class="marcador">
            <thead>
                <tr>
                    <th class="col-pos">Pos</th>
                    <th>Equipa</th>
                    <th class="col-estado">Estado</th>
                    <th class="col-pts">Pts</th>
                </tr>
            </thead>
            <tbody>
                ${classificacao.map((linha) => `
                    <tr class="${linha.posicao === 1 && linha.estado !== "DESISTENTE" ? "lider" : ""}${linha.estado === "DESISTENTE" ? " linha-desistente" : ""}">
                        <td class="col-pos"><span class="pos">${linha.posicao}</span></td>
                        <td class="col-equipa">
                            <span class="equipa">${texto(linha.equipa)}</span>
                            <span class="treinador">${texto(linha.treinador)}</span>
                        </td>
                        <td class="col-estado">${badgeEstado(linha.estado)}</td>
                        <td class="col-pts"><span class="pts">${linha.pontos}</span></td>
                    </tr>
                `).join("")}
            </tbody>
        </table>`;
}

function desenharEquipas(equipas, desativada) {
    $("#form-equipa").querySelectorAll("input, button")
        .forEach((elemento) => { elemento.disabled = desativada; });

    if (!equipas.length) {
        $("#tabela-equipas").innerHTML = `<p class="ajuda">Ainda não há equipas nesta liga.</p>`;
        return;
    }

    $("#tabela-equipas").innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Equipa</th>
                    <th>Treinador</th>
                    <th>Estado</th>
                    <th>Conta</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                ${equipas.map((equipa) => `
                    <tr class="${equipa.estado === "DESISTENTE" ? "linha-desistente" : ""}">
                        <td><strong>${texto(equipa.nome)}</strong></td>
                        <td>
                            ${texto(equipa.treinador)}
                            <button class="botao pequeno" data-editar-treinador="${equipa.id}"
                                data-nome-treinador="${texto(equipa.treinador)}"
                                ${desativada ? "disabled" : ""}>
                                Editar
                            </button>
                        </td>
                        <td>${badgeEstado(equipa.estado)}</td>
                        <td>${badgeConta(equipa)}</td>
                        <td class="numero">
                            ${acoesDaConta(equipa, desativada)}
                            <button class="botao pequeno perigo" data-desistencia="${equipa.id}"
                                ${equipa.estado !== "ATIVA" || desativada ? "disabled" : ""}>
                                Desistência
                            </button>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>`;
}

/* O estado da conta do treinador de uma equipa. Vem do servidor já decidido
   (EquipaDto.deParaGestor) para o botão não ter de o adivinhar a partir de
   meia dúzia de campos soltos. */
function badgeConta(equipa) {
    if (equipa.conviteEstado === "LIGADA") {
        return `<span class="badge verde">Ligada</span>`;
    }
    if (equipa.conviteEstado === "PENDENTE") {
        const ate = equipa.conviteExpiraEm
            ? ` title="Válido até ${new Date(equipa.conviteExpiraEm).toLocaleDateString("pt-PT")}"`
            : "";
        return `<span class="badge amarelo"${ate}>Convite pendente</span>`;
    }
    return `<span class="ajuda">Sem convite</span>`;
}

/* Convidar e copiar o link são o mesmo botão porque são o mesmo pedido: o
   servidor devolve o convite que já exista em vez de emitir um segundo. */
function acoesDaConta(equipa, desativada) {
    if (equipa.conviteEstado === "LIGADA") {
        return `<button class="botao pequeno" data-desligar-conta="${equipa.id}"
                    ${desativada ? "disabled" : ""}>Desligar conta</button>`;
    }

    const convidar = `<button class="botao pequeno" data-convidar-treinador="${equipa.id}"
            ${desativada ? "disabled" : ""}>
            ${equipa.conviteEstado === "PENDENTE" ? "Copiar link" : "Convidar treinador"}
        </button>`;

    if (equipa.conviteEstado !== "PENDENTE") {
        return convidar;
    }
    return `${convidar}
        <button class="botao pequeno perigo" data-revogar-convite="${equipa.conviteId}"
            data-equipa-convite="${equipa.id}" ${desativada ? "disabled" : ""}>
            Revogar
        </button>`;
}

function desenharJornadas(jornadas) {
    const lista = $("#lista-jornadas");

    if (!jornadas.length) {
        lista.innerHTML = `<p class="ajuda">Sem jornadas abertas.</p>`;
        return;
    }

    lista.innerHTML = jornadas.map((jornada) => `
        <button class="cartao-jornada ${jornada.id === estado.jornadaId ? "selecionado" : ""}" data-jornada="${jornada.id}">
            <strong>Jornada ${jornada.numero}</strong>
            <small>${jornada.treino ? "Treino" : "Oficial"} &middot; ${plural(jornada.resultados.length, "resultado", "resultados")}</small>
            ${badgeEstado(jornada.estado)}
        </button>
    `).join("");
}

function desenharJornadaSelecionada() {
    const painel = $("#detalhe-jornada");
    const jornada = estado.detalhe.jornadas.find((j) => j.id === estado.jornadaId);

    if (!jornada) {
        painel.innerHTML = `<p class="ajuda">Escolhe uma jornada para gerir os resultados.</p>`;
        return;
    }

    const fechada = jornada.estado === "FECHADA";
    const emDesempate = jornada.estado === "DESEMPATE";
    // Em desempate as posições já estão atribuídas (partilhadas pelos
    // empatados) e as pontuações estão trancadas, tal como numa jornada
    // fechada — o servidor recusa alterá-las até o empate ficar desfeito.
    const bloqueada = fechada || emDesempate;
    const pontosPorEquipa = new Map(jornada.resultados.map((r) => [r.equipaId, r]));
    // A coluna de valor usa a posição desta jornada em concreto (a que o
    // backend soma às restantes do bloco quando este fechar), não a
    // classificação geral acumulada da liga. Fechada, usa a posição já
    // atribuída; aberta, prevê a posição a partir das pontuações inseridas.
    const posicaoPreviaPorEquipa = bloqueada ? null : calcularPosicoesPreview(jornada.resultados);
    const regra = estado.regraDivida;

    const linhas = estado.detalhe.equipas
        .filter((equipa) => equipa.estado === "ATIVA" || pontosPorEquipa.has(equipa.id))
        .slice()
        .sort((a, b) => {
            // Com posições atribuídas, ordena por elas. Aberta, pelas pontuações
            // já inseridas, como prévia de como fecharia se fechasse agora.
            const ra = pontosPorEquipa.get(a.id);
            const rb = pontosPorEquipa.get(b.id);
            const va = bloqueada ? (ra && ra.posicao ? ra.posicao : Infinity) : (ra ? -ra.pontuacao : Infinity);
            const vb = bloqueada ? (rb && rb.posicao ? rb.posicao : Infinity) : (rb ? -rb.pontuacao : Infinity);
            // Entre duas equipas ainda sem resultado, va - vb dá Infinity menos
            // Infinity, que é NaN. Um comparador que devolve NaN deixa a ordem
            // por conta do motor, e era isso que baralhava a lista numa jornada
            // acabada de abrir. NaN é falso, portanto o nome decide.
            return (va - vb) || a.nome.localeCompare(b.nome, "pt");
        })
        .map((equipa) => {
            const resultado = pontosPorEquipa.get(equipa.id);
            const podeEditar = !bloqueada && equipa.estado === "ATIVA";
            const posicaoDaJornada = bloqueada
                ? (resultado ? resultado.posicao : null)
                : posicaoPreviaPorEquipa.get(equipa.id);
            const valor = (regra && equipa.estado === "ATIVA" && posicaoDaJornada)
                ? formatoMoeda(calcularValorEscalao(regra, posicaoDaJornada))
                : "-";
            return `
                <tr>
                    <td class="posicao">${resultado && resultado.posicao ? resultado.posicao : "&ndash;"}</td>
                    <td><strong>${texto(equipa.nome)}</strong></td>
                    <td class="numero">
                        ${podeEditar
                            ? `<input type="number" min="0" step="1" value="${resultado ? resultado.pontuacao : ""}"
                                   data-pontuacao="${equipa.id}" placeholder="0">`
                            : (resultado ? resultado.pontuacao : "&ndash;")}
                    </td>
                    <td class="numero">${valor}</td>
                    <td class="numero">
                        ${podeEditar
                            ? `<button class="botao pequeno" data-guardar="${equipa.id}">Guardar</button>`
                            : ""}
                    </td>
                </tr>`;
        }).join("");

    let estadoDaJornada;
    if (emDesempate) {
        estadoDaJornada = "Jornada por fechar: há equipas empatadas. Ordena-as abaixo para desfazer o empate.";
    } else if (fechada) {
        estadoDaJornada = "Jornada fechada — posições atribuídas por pontuação.";
    } else {
        estadoDaJornada = "Insere a pontuação de cada equipa ativa e fecha a jornada no fim.";
    }

    painel.innerHTML = `
        <h3>Jornada ${jornada.numero} ${badgeEstado(jornada.estado)} ${badgeEstado(jornada.tipo)}</h3>
        <p class="ajuda">${estadoDaJornada}</p>
        <p class="ajuda">${regra
            ? "A coluna Valor é o que esta jornada pesa no bloco, segundo a regra da liga. Quando o bloco fechar, soma-se ao valor das outras jornadas que o compõem."
            : "Sem regra de dívida definida nesta liga: os blocos são cobrados à mão."}</p>
        <div class="tabela-rolavel">
            <table>
                <thead>
                    <tr>
                        <th class="posicao">Pos</th>
                        <th>Equipa</th>
                        <th class="numero">Pontos</th>
                        <th class="numero">Valor</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>${linhas || `<tr><td colspan="5" class="ajuda">Sem equipas ativas.</td></tr>`}</tbody>
            </table>
        </div>
        ${emDesempate ? desenharDesempate(jornada) : ""}
        ${bloqueada ? "" : `
            <div class="barra-acoes depois">
                <button class="botao primario" data-fechar="${jornada.id}"
                    ${jornada.resultados.length ? "" : "disabled"}>Fechar jornada</button>
                <span class="ajuda">Se ficarem equipas empatadas, a jornada espera pelo desempate antes de fechar.</span>
            </div>`}`;
}

/* ------------------------------------------------------------ desempate --- */

/* Os grupos de equipas que ficaram com a mesma pontuação, do melhor para o
   pior. A ordem dentro de cada grupo é a que o gestor está a montar; só é
   enviada ao servidor quando ele confirma. */
function prepararDesempate(jornada) {
    if (estado.desempate && estado.desempate.jornadaId === jornada.id) {
        return estado.desempate;
    }

    const porPontuacao = new Map();
    jornada.resultados.forEach((resultado) => {
        if (!porPontuacao.has(resultado.pontuacao)) {
            porPontuacao.set(resultado.pontuacao, []);
        }
        porPontuacao.get(resultado.pontuacao).push(resultado);
    });

    const grupos = [...porPontuacao.entries()]
        .filter(([, resultados]) => resultados.length > 1)
        .sort((a, b) => b[0] - a[0])
        .map(([pontuacao, resultados]) => ({
            pontuacao,
            equipas: resultados.map((r) => ({ id: r.equipaId, nome: r.equipa }))
        }));

    estado.desempate = { jornadaId: jornada.id, grupos };
    return estado.desempate;
}

function desenharDesempate(jornada) {
    const desempate = prepararDesempate(jornada);

    const grupos = desempate.grupos.map((grupo, indiceGrupo) => `
        <div class="grupo-empate">
            <h4>${plural(grupo.equipas.length, "equipa", "equipas")} com ${grupo.pontuacao} ${grupo.pontuacao === 1 ? "ponto" : "pontos"}</h4>
            <ol class="ordem-empate">
                ${grupo.equipas.map((equipa, indice) => `
                    <li>
                        <span>${texto(equipa.nome)}</span>
                        <span class="mover-empate">
                            <button class="botao pequeno" title="Subir" aria-label="Subir ${texto(equipa.nome)}"
                                data-desempate-mover="cima" data-desempate-grupo="${indiceGrupo}"
                                data-desempate-indice="${indice}" ${indice === 0 ? "disabled" : ""}>&uarr;</button>
                            <button class="botao pequeno" title="Descer" aria-label="Descer ${texto(equipa.nome)}"
                                data-desempate-mover="baixo" data-desempate-grupo="${indiceGrupo}"
                                data-desempate-indice="${indice}"
                                ${indice === grupo.equipas.length - 1 ? "disabled" : ""}>&darr;</button>
                        </span>
                    </li>
                `).join("")}
            </ol>
        </div>
    `).join("");

    return `
        <div class="painel-desempate">
            <h3 class="titulo-seccao">Desempate</h3>
            <p class="ajuda">
                A ordem que deixares aqui é a que fica na tabela, de cima para baixo. Só depois
                de confirmares é que a jornada fecha e conta para o bloco de dívida.
            </p>
            ${grupos}
            <div class="barra-acoes depois">
                <button class="botao primario" data-confirmar-desempate="${jornada.id}">Confirmar desempate</button>
            </div>
        </div>`;
}

/* -------------------------------------------------------------- dívidas --- */

/* Carregada só quando o separador abre: a dívida de cada equipa vive no seu
   próprio recurso (DividaController), não no detalhe da liga. A regra em si
   (estado.regraDivida) já vem de carregarDetalhe(), partilhada com a tab
   Jornadas. */
async function carregarDividas() {
    if (!estado.ligaId || !estado.detalhe) {
        return;
    }

    const equipas = estado.detalhe.equipas;
    const dividas = await Promise.all(
        equipas.map((equipa) => api(`/api/ligas/${estado.ligaId}/equipas/${equipa.id}/divida`))
    );
    estado.dividas = new Map(equipas.map((equipa, i) => [equipa.id, dividas[i]]));

    if (!equipas.some((equipa) => equipa.id === estado.equipaDividaId)) {
        estado.equipaDividaId = equipas.length ? equipas[0].id : null;
    }

    desenharRegraDivida();
    desenharListaEquipasDivida();
    desenharDetalheDivida();
}

/* Recarrega as dívidas só se o separador estiver aberto — para não gastar
   pedidos a mais sempre que uma equipa é criada ou uma jornada fecha. */
async function atualizarDividasSeAbertas() {
    if (estado.tab === "dividas") {
        await carregarDividas();
    }
}

function desenharRegraDivida() {
    const r = estado.regraDivida;
    $("#regra-divida-ajuda").textContent = r
        ? "Os blocos de período fecham sozinhos quando a jornada certa fecha; a inscrição é cobrada ao adicionar a equipa."
        : "Sem regra definida: os blocos e a inscrição são cobrados à mão.";
    $("#regra-inscricao").value = r ? r.valorInscricao : "";
    $("#regra-inicial").value = r ? r.valorInicial : "";
    $("#regra-incremento").value = r ? r.incremento : "";
    $("#regra-escalao").value = r ? r.equipasPorEscalao : "";
    $("#regra-maximo").value = r ? r.valorMaximo : "";
    $("#regra-jornadas").value = r ? r.jornadasPorBloco : "";
}

function desenharListaEquipasDivida() {
    const lista = $("#lista-equipas-divida");
    const equipas = estado.detalhe.equipas;

    if (!equipas.length) {
        lista.innerHTML = `<p class="ajuda">Ainda não há equipas nesta liga.</p>`;
        return;
    }

    lista.innerHTML = equipas.map((equipa) => {
        const divida = estado.dividas.get(equipa.id);
        return `
        <button class="cartao-jornada ${equipa.id === estado.equipaDividaId ? "selecionado" : ""}" data-equipa-divida="${equipa.id}">
            <strong>${texto(equipa.nome)}</strong>
            <small>${divida ? formatoMoeda(divida.totalPendente) : ""}</small>
            ${divida ? badgeEstado(divida.estado) : ""}
        </button>`;
    }).join("");
}

function desenharDetalheDivida() {
    const painel = $("#detalhe-divida");
    const equipa = estado.detalhe.equipas.find((e) => e.id === estado.equipaDividaId);
    const divida = estado.dividas.get(estado.equipaDividaId);

    if (!equipa || !divida) {
        painel.innerHTML = `<p class="ajuda">Escolhe uma equipa para gerir a dívida.</p>`;
        return;
    }

    const temPendente = divida.blocos.some((bloco) => bloco.estado === "PENDENTE");

    const linhasBlocos = divida.blocos.map((bloco) => `
        <tr>
            <td class="numero">${bloco.numeroBloco}</td>
            <td>${badgeTipoBloco(bloco.tipo)}</td>
            <td class="numero">${formatoMoeda(bloco.valor)}</td>
            <td>${badgeEstado(bloco.estado)}</td>
            <td class="numero">${bloco.estado === "PENDENTE"
                ? `<button class="botao pequeno" data-pagar-bloco="${bloco.id}">Marcar pago</button>`
                : ""}</td>
        </tr>`).join("");

    painel.innerHTML = `
        <h3>${texto(equipa.nome)} ${badgeEstado(divida.estado)}</h3>
        <p class="ajuda">Total pendente: <strong>${formatoMoeda(divida.totalPendente)}</strong></p>

        <form class="formulario em-linha" data-form-bloco="${equipa.id}">
            <div>
                <label for="bloco-valor">Novo bloco (valor)</label>
                <input id="bloco-valor" type="number" min="0" step="0.01" placeholder="0.00" required>
            </div>
            <button type="submit" class="botao">Fechar bloco</button>
        </form>

        ${divida.blocos.length ? `
            <div class="tabela-rolavel">
                <table>
                    <thead>
                        <tr><th class="numero">Nº</th><th>Tipo</th><th class="numero">Valor</th><th>Estado</th><th></th></tr>
                    </thead>
                    <tbody>${linhasBlocos}</tbody>
                </table>
            </div>
            <div class="barra-acoes depois">
                <button class="botao primario" data-pagar-tudo="${equipa.id}" ${temPendente ? "" : "disabled"}>
                    Marcar tudo pago
                </button>
            </div>` : `<p class="ajuda">Ainda não há blocos registados.</p>`}`;
}

/* --------------------------------------------------------------- ações ---- */

/* Lançar uma jornada é escrever vinte números seguidos, e é a coisa que mais
   se faz nesta aplicação. Por isso o Enter grava, e o cursor salta sozinho
   para a equipa seguinte.
 *
 * A equipa seguinte é decidida ANTES de gravar, pela ordem que o gestor tem à
 * frente. Depois de gravar a tabela reordena-se pelas pontuações, por isso o
 * campo é procurado outra vez pelo id da equipa e não pela posição na lista.
 */
function guardarPontuacao(equipaId, saltarParaSeguinte) {
    const campo = document.querySelector(`[data-pontuacao="${equipaId}"]`);
    if (!campo || campo.value === "") {
        mostrarAlerta("Indica uma pontuação.");
        return;
    }

    const campos = [...document.querySelectorAll("[data-pontuacao]")];
    const seguinte = campos[campos.indexOf(campo) + 1];

    executar(async () => {
        await api(`/api/ligas/${estado.ligaId}/jornadas/${estado.jornadaId}/resultados`, {
            method: "PUT",
            body: JSON.stringify({ equipaId, pontuacao: Number(campo.value) })
        });
        await recarregar();
        mostrarAlerta("Resultado guardado.", "sucesso");

        if (saltarParaSeguinte && seguinte) {
            const redesenhado = document.querySelector(`[data-pontuacao="${seguinte.dataset.pontuacao}"]`);
            if (redesenhado) {
                redesenhado.focus();
                redesenhado.select();
            }
        }
    });
}

// O Enter num campo de pontuação vale por carregar em Guardar. São campos
// soltos numa tabela, não um formulário, por isso o browser não faz isto
// sozinho: sem esta linha, o Enter não fazia rigorosamente nada.
document.addEventListener("keydown", (evento) => {
    if (evento.key !== "Enter") {
        return;
    }
    const campo = evento.target.closest("[data-pontuacao]");
    if (!campo) {
        return;
    }
    evento.preventDefault();
    guardarPontuacao(campo.dataset.pontuacao, true);
});

function selecionarTab(tab) {
    estado.tab = tab;
    document.querySelectorAll(".separador")
        .forEach((botao) => botao.classList.toggle("ativo", botao.dataset.tab === tab));
    document.querySelectorAll(".tab")
        .forEach((seccao) => seccao.classList.toggle("oculto", seccao.id !== `tab-${tab}`));
    if (tab === "dividas" && estado.ligaId) {
        executar(carregarDividas);
    }
}

$("#form-regra-divida").addEventListener("submit", (evento) => {
    evento.preventDefault();
    executar(async () => {
        await api(`/api/ligas/${estado.ligaId}/regra-divida`, {
            method: "PUT",
            body: JSON.stringify({
                valorInscricao: Number($("#regra-inscricao").value || 0),
                valorInicial: Number($("#regra-inicial").value || 0),
                incremento: Number($("#regra-incremento").value || 0),
                equipasPorEscalao: Number($("#regra-escalao").value || 1),
                valorMaximo: Number($("#regra-maximo").value || 0),
                jornadasPorBloco: Number($("#regra-jornadas").value || 1)
            })
        });
        await recarregar();
        mostrarAlerta("Regra de dívida guardada.", "sucesso");
    });
});

// O formulário de "novo bloco" nasce dentro do detalhe da dívida, criado de
// novo a cada equipa escolhida — por isso o envio é delegado, como os
// cliques mais abaixo, em vez de um listener preso a um elemento fixo.
document.addEventListener("submit", (evento) => {
    const form = evento.target.closest("[data-form-bloco]");
    if (!form) {
        return;
    }
    evento.preventDefault();
    executar(async () => {
        const equipaId = form.dataset.formBloco;
        const valor = form.querySelector("#bloco-valor").value;
        await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/divida/blocos`, {
            method: "POST",
            body: JSON.stringify({ valor: Number(valor) })
        });
        await recarregar();
        mostrarAlerta("Bloco registado.", "sucesso");
    });
});

$("#form-liga").addEventListener("submit", (evento) => {
    evento.preventDefault();
    executar(async () => {
        const liga = await api("/api/ligas", {
            method: "POST",
            body: JSON.stringify({
                nome: $("#liga-nome").value.trim(),
                maxEquipas: Number($("#liga-max").value)
            })
        });
        $("#liga-nome").value = "";
        estado.ligaId = liga.id;
        estado.jornadaId = null;
        await recarregar();
        mostrarAlerta(`Liga "${liga.nome}" criada.`, "sucesso");
    });
});

$("#form-equipa").addEventListener("submit", (evento) => {
    evento.preventDefault();
    executar(async () => {
        await api(`/api/ligas/${estado.ligaId}/equipas`, {
            method: "POST",
            body: JSON.stringify({
                nome: $("#equipa-nome").value.trim(),
                treinador: $("#equipa-treinador").value.trim()
            })
        });
        $("#equipa-nome").value = "";
        $("#equipa-treinador").value = "";
        // A equipa pode ter sido logo cobrada a inscrição, se a liga tiver regra.
        await recarregar();
        mostrarAlerta("Equipa adicionada.", "sucesso");
    });
});

$("#btn-terminar").addEventListener("click", () => {
    if (!confirm("Terminar esta liga? Depois disso não é possível adicionar equipas nem abrir jornadas.")) {
        return;
    }
    executar(async () => {
        await api(`/api/ligas/${estado.ligaId}/terminar`, { method: "POST" });
        await recarregar();
        mostrarAlerta("Liga terminada.", "sucesso");
    });
});

$("#liga-logo").addEventListener("click", () => $("#input-logo").click());

$("#input-logo").addEventListener("change", (evento) => {
    const ficheiro = evento.target.files[0];
    evento.target.value = "";  // permite reescolher o mesmo ficheiro a seguir
    if (!ficheiro) {
        return;
    }
    executar(async () => {
        const dados = new FormData();
        dados.append("ficheiro", ficheiro);
        await api(`/api/ligas/${estado.ligaId}/logo`, { method: "POST", body: dados });
        estado.logoV = Date.now();
        await recarregar();
        mostrarAlerta("Logo da liga atualizado.", "sucesso");
    });
});

$("#btn-logo-remover").addEventListener("click", () => {
    if (!confirm("Remover o logo desta liga?")) {
        return;
    }
    executar(async () => {
        await api(`/api/ligas/${estado.ligaId}/logo`, { method: "DELETE" });
        estado.logoV = Date.now();
        await recarregar();
        mostrarAlerta("Logo removido.", "sucesso");
    });
});

$("#btn-abrir-jornada").addEventListener("click", () => {
    executar(async () => {
        const jornada = await api(`/api/ligas/${estado.ligaId}/jornadas`, { method: "POST" });
        estado.jornadaId = jornada.id;
        await recarregar();
        mostrarAlerta(`Jornada ${jornada.numero} (${jornada.tipo.toLowerCase()}) aberta.`, "sucesso");
    });
});

document.querySelectorAll(".separador")
    .forEach((botao) => botao.addEventListener("click", () => selecionarTab(botao.dataset.tab)));

document.addEventListener("click", (evento) => {
    const alvo = evento.target.closest(
        "[data-liga], [data-jornada], [data-desistencia], [data-guardar], [data-fechar], " +
        "[data-equipa-divida], [data-pagar-bloco], [data-pagar-tudo], [data-convidar-treinador], " +
        "[data-revogar-convite], [data-editar-treinador], [data-desligar-conta], " +
        "[data-desempate-mover], [data-confirmar-desempate]"
    );
    if (!alvo) {
        return;
    }

    if (alvo.dataset.liga) {
        estado.ligaId = alvo.dataset.liga;
        estado.jornadaId = null;
        estado.equipaDividaId = null;
        executar(async () => {
            await recarregar();
        });
        return;
    }

    if (alvo.dataset.jornada) {
        estado.jornadaId = alvo.dataset.jornada;
        desenharJornadas(estado.detalhe.jornadas);
        desenharJornadaSelecionada();
        return;
    }

    if (alvo.dataset.desistencia) {
        if (!confirm("Registar a desistência desta equipa?")) {
            return;
        }
        executar(async () => {
            await api(`/api/ligas/${estado.ligaId}/equipas/${alvo.dataset.desistencia}/desistencia`, { method: "POST" });
            await recarregar();
            mostrarAlerta("Desistência registada.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.guardar) {
        guardarPontuacao(alvo.dataset.guardar, false);
        return;
    }

    if (alvo.dataset.fechar) {
        if (!confirm("Fechar esta jornada?")) {
            return;
        }
        executar(async () => {
            const jornada = await api(
                `/api/ligas/${estado.ligaId}/jornadas/${alvo.dataset.fechar}/fechar`, { method: "POST" });
            estado.desempate = null;
            // Pode ter fechado um bloco de dívida sozinha, se a liga tiver regra.
            await recarregar();
            // Não é um erro: a jornada não fechou porque falta uma decisão tua.
            mostrarAlerta(...(jornada.estado === "DESEMPATE"
                ? ["Há equipas empatadas: desfaz o empate para a jornada fechar.", "aviso"]
                : ["Jornada fechada.", "sucesso"]));
        });
        return;
    }

    if (alvo.dataset.desempateMover) {
        // O painel ainda está no ecrã enquanto um pedido anterior corre, e
        // esse pedido limpa o estado a meio — sem isto, carregar numa seta
        // nessa janela rebentava com um erro na consola e não fazia nada.
        if (!estado.desempate) {
            return;
        }
        const grupo = estado.desempate.grupos[Number(alvo.dataset.desempateGrupo)];
        const indice = Number(alvo.dataset.desempateIndice);
        const destino = alvo.dataset.desempateMover === "cima" ? indice - 1 : indice + 1;
        if (destino < 0 || destino >= grupo.equipas.length) {
            return;
        }
        [grupo.equipas[indice], grupo.equipas[destino]] = [grupo.equipas[destino], grupo.equipas[indice]];
        desenharJornadaSelecionada();
        return;
    }

    if (alvo.dataset.confirmarDesempate) {
        if (!estado.desempate) {
            return;
        }
        // Uma lista só, com todos os grupos por ordem: o servidor volta a
        // ordenar por pontuação, portanto isto nunca troca equipas entre
        // pontuações diferentes.
        const ordem = estado.desempate.grupos.flatMap((grupo) => grupo.equipas.map((equipa) => equipa.id));
        executar(async () => {
            await api(`/api/ligas/${estado.ligaId}/jornadas/${alvo.dataset.confirmarDesempate}/desempate`, {
                method: "POST",
                body: JSON.stringify({ ordem })
            });
            estado.desempate = null;
            await recarregar();
            mostrarAlerta("Desempate resolvido e jornada fechada.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.equipaDivida) {
        estado.equipaDividaId = alvo.dataset.equipaDivida;
        desenharListaEquipasDivida();
        desenharDetalheDivida();
        return;
    }

    if (alvo.dataset.pagarBloco) {
        if (!confirm("Marcar este bloco como pago?")) {
            return;
        }
        executar(async () => {
            const equipaId = estado.equipaDividaId;
            await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/divida/blocos/${alvo.dataset.pagarBloco}/pagar`,
                { method: "POST" });
            await recarregar();
            mostrarAlerta("Bloco marcado como pago.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.pagarTudo) {
        if (!confirm("Marcar toda a dívida desta equipa como paga?")) {
            return;
        }
        executar(async () => {
            const equipaId = alvo.dataset.pagarTudo;
            await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/divida/pagar`, { method: "POST" });
            await recarregar();
            mostrarAlerta("Dívida paga.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.convidarTreinador) {
        executar(async () => {
            const equipaId = alvo.dataset.convidarTreinador;
            // Sem diasValidade: o servidor usa a validade por omissão. Pedir um
            // convite eterno daqui era espalhar uma credencial sem prazo.
            const convite = await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/convites-treinador`, {
                method: "POST",
                body: JSON.stringify({ diasValidade: null })
            });
            const copiado = await copiar(convite.link);
            await recarregar();
            mostrarAlerta(copiado
                ? `Link do convite copiado. Envia-o ao treinador: ${convite.link}`
                : `Link do convite: ${convite.link}`, "sucesso");
        });
        return;
    }

    if (alvo.dataset.revogarConvite) {
        if (!confirm("Revogar este convite? O link deixa de funcionar.")) {
            return;
        }
        executar(async () => {
            const equipaId = alvo.dataset.equipaConvite;
            await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/convites-treinador/${alvo.dataset.revogarConvite}`,
                { method: "DELETE" });
            await recarregar();
            mostrarAlerta("Convite revogado.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.editarTreinador) {
        const nome = prompt("Nome do treinador desta equipa:", alvo.dataset.nomeTreinador || "");
        if (nome === null || !nome.trim()) {
            return;
        }
        executar(async () => {
            await api(`/api/ligas/${estado.ligaId}/equipas/${alvo.dataset.editarTreinador}/treinador`, {
                method: "PATCH",
                body: JSON.stringify({ nome: nome.trim() })
            });
            await recarregar();
            mostrarAlerta("Treinador alterado.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.desligarConta) {
        if (!confirm("Desligar a conta deste treinador? Deixa de ver a equipa e as dívidas dela.")) {
            return;
        }
        executar(async () => {
            await api(`/api/ligas/${estado.ligaId}/equipas/${alvo.dataset.desligarConta}/treinador/conta`,
                { method: "DELETE" });
            await recarregar();
            mostrarAlerta("Conta desligada.", "sucesso");
        });
    }
});

/* --------------------------------------------------------------- início --- */

async function iniciar() {
    const resposta = await fetch("/api/auth/estado");
    if (!resposta.ok) {
        window.location.href = "/login.html";
        return;
    }
    estado.gestor = await resposta.json();
    $("#gestor-nome").textContent = estado.gestor.nome;
    $("#barra-sessao").classList.remove("oculto");
    if (estado.gestor.admin) {
        $("#link-admin").classList.remove("oculto");
    }
    if (!estado.gestor.podeCriarLigas) {
        // Conta sem permissão para criar ligas (normalmente uma conta que só
        // aceitou um convite de treinador). O servidor já recusa o pedido de
        // qualquer forma; isto só evita mostrar um formulário que ia falhar.
        $("#form-liga").classList.add("oculto");
        $("#aviso-sem-permissao").classList.remove("oculto");
    }
    await carregarLigas();
}

$("#btn-sair").addEventListener("click", () => {
    executar(async () => {
        await api("/api/auth/logout", { method: "POST" });
        window.location.href = "/login.html";
    });
});

selecionarTab("classificacao");
executar(iniciar);
