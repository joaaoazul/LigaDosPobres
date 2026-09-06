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
    equipaDividaId: null
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
    alerta.className = `alerta ${tipo === "sucesso" ? "sucesso" : ""}`;
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

    desenharClassificacao(detalhe.classificacao);
    desenharEquipas(detalhe.equipas, desativada);
    desenharJornadas(detalhe.jornadas);
    desenharJornadaSelecionada();
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
                    <th>Estado</th>
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
                        <td>${badgeEstado(linha.estado)}</td>
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
                    <th></th>
                </tr>
            </thead>
            <tbody>
                ${equipas.map((equipa) => `
                    <tr class="${equipa.estado === "DESISTENTE" ? "linha-desistente" : ""}">
                        <td><strong>${texto(equipa.nome)}</strong></td>
                        <td>${texto(equipa.treinador)}</td>
                        <td>${badgeEstado(equipa.estado)}</td>
                        <td class="numero">
                            <button class="botao pequeno" data-convidar-treinador="${equipa.id}"
                                ${desativada ? "disabled" : ""}>
                                Convidar treinador
                            </button>
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
    const pontosPorEquipa = new Map(jornada.resultados.map((r) => [r.equipaId, r]));
    // A coluna de valor usa a posição desta jornada em concreto (a que o
    // backend soma às restantes do bloco quando este fechar), não a
    // classificação geral acumulada da liga. Fechada, usa a posição já
    // atribuída; aberta, prevê a posição a partir das pontuações inseridas.
    const posicaoPreviaPorEquipa = fechada ? null : calcularPosicoesPreview(jornada.resultados);
    const regra = estado.regraDivida;

    const linhas = estado.detalhe.equipas
        .filter((equipa) => equipa.estado === "ATIVA" || pontosPorEquipa.has(equipa.id))
        .slice()
        .sort((a, b) => {
            // Fechada: ordena pela posição atribuída. Aberta: pelas pontuações
            // já inseridas, como prévia de como fecharia se fechasse agora.
            const ra = pontosPorEquipa.get(a.id);
            const rb = pontosPorEquipa.get(b.id);
            const va = fechada ? (ra && ra.posicao ? ra.posicao : Infinity) : (ra ? -ra.pontuacao : Infinity);
            const vb = fechada ? (rb && rb.posicao ? rb.posicao : Infinity) : (rb ? -rb.pontuacao : Infinity);
            return va - vb;
        })
        .map((equipa) => {
            const resultado = pontosPorEquipa.get(equipa.id);
            const podeEditar = !fechada && equipa.estado === "ATIVA";
            const posicaoDaJornada = fechada
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

    painel.innerHTML = `
        <h3>Jornada ${jornada.numero} ${badgeEstado(jornada.estado)} ${badgeEstado(jornada.tipo)}</h3>
        <p class="ajuda">${fechada
            ? "Jornada fechada — posições atribuídas por pontuação."
            : "Insere a pontuação de cada equipa ativa e fecha a jornada no fim."}</p>
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
        ${fechada ? "" : `
            <div class="barra-acoes depois">
                <button class="botao primario" data-fechar="${jornada.id}"
                    ${jornada.resultados.length ? "" : "disabled"}>Fechar jornada</button>
                <span class="ajuda">Empates ficam com a mesma posição (desempate manual ainda por implementar).</span>
            </div>`}`;
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
        estado.regraDivida = await api(`/api/ligas/${estado.ligaId}/regra-divida`, {
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
        desenharRegraDivida();
        // A coluna de valor na tab Jornadas também depende da regra.
        desenharJornadaSelecionada();
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
        estado.dividas.set(equipaId, await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/divida`));
        desenharListaEquipasDivida();
        desenharDetalheDivida();
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
        await carregarLigas();
        await carregarDetalhe();
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
        await carregarDetalhe();
        await carregarLigas();
        // A equipa pode ter sido logo cobrada a inscrição, se a liga tiver regra.
        await atualizarDividasSeAbertas();
        mostrarAlerta("Equipa adicionada.", "sucesso");
    });
});

$("#btn-terminar").addEventListener("click", () => {
    if (!confirm("Terminar esta liga? Depois disso não é possível adicionar equipas nem abrir jornadas.")) {
        return;
    }
    executar(async () => {
        await api(`/api/ligas/${estado.ligaId}/terminar`, { method: "POST" });
        await carregarDetalhe();
        await carregarLigas();
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
        await carregarDetalhe();
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
        await carregarDetalhe();
        mostrarAlerta("Logo removido.", "sucesso");
    });
});

$("#btn-abrir-jornada").addEventListener("click", () => {
    executar(async () => {
        const jornada = await api(`/api/ligas/${estado.ligaId}/jornadas`, { method: "POST" });
        estado.jornadaId = jornada.id;
        await carregarDetalhe();
        await carregarLigas();
        mostrarAlerta(`Jornada ${jornada.numero} (${jornada.tipo.toLowerCase()}) aberta.`, "sucesso");
    });
});

document.querySelectorAll(".separador")
    .forEach((botao) => botao.addEventListener("click", () => selecionarTab(botao.dataset.tab)));

document.addEventListener("click", (evento) => {
    const alvo = evento.target.closest(
        "[data-liga], [data-jornada], [data-desistencia], [data-guardar], [data-fechar], " +
        "[data-equipa-divida], [data-pagar-bloco], [data-pagar-tudo], [data-convidar-treinador]"
    );
    if (!alvo) {
        return;
    }

    if (alvo.dataset.liga) {
        estado.ligaId = alvo.dataset.liga;
        estado.jornadaId = null;
        estado.equipaDividaId = null;
        executar(async () => {
            await carregarDetalhe();
            desenharLigas();
            await atualizarDividasSeAbertas();
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
            await carregarDetalhe();
            await carregarLigas();
            mostrarAlerta("Desistência registada.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.guardar) {
        const campo = document.querySelector(`[data-pontuacao="${alvo.dataset.guardar}"]`);
        if (campo.value === "") {
            mostrarAlerta("Indica uma pontuação.");
            return;
        }
        executar(async () => {
            await api(`/api/ligas/${estado.ligaId}/jornadas/${estado.jornadaId}/resultados`, {
                method: "PUT",
                body: JSON.stringify({
                    equipaId: alvo.dataset.guardar,
                    pontuacao: Number(campo.value)
                })
            });
            await carregarDetalhe();
            mostrarAlerta("Resultado guardado.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.fechar) {
        if (!confirm("Fechar esta jornada?")) {
            return;
        }
        executar(async () => {
            await api(`/api/ligas/${estado.ligaId}/jornadas/${alvo.dataset.fechar}/fechar`, { method: "POST" });
            await carregarDetalhe();
            await carregarLigas();
            // Pode ter fechado um bloco de dívida sozinha, se a liga tiver regra.
            await atualizarDividasSeAbertas();
            mostrarAlerta("Jornada fechada.", "sucesso");
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
            estado.dividas.set(equipaId, await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/divida`));
            desenharListaEquipasDivida();
            desenharDetalheDivida();
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
            estado.dividas.set(equipaId, await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/divida`));
            desenharListaEquipasDivida();
            desenharDetalheDivida();
            mostrarAlerta("Dívida paga.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.convidarTreinador) {
        executar(async () => {
            const equipaId = alvo.dataset.convidarTreinador;
            const convite = await api(`/api/ligas/${estado.ligaId}/equipas/${equipaId}/convites-treinador`, {
                method: "POST",
                body: JSON.stringify({ diasValidade: null })
            });
            navigator.clipboard?.writeText(convite.codigo).catch(() => {});
            mostrarAlerta(`Convite criado e copiado: ${convite.codigo}`, "sucesso");
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
