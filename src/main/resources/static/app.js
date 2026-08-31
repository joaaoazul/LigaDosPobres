"use strict";

const estado = {
    gestor: null,
    ligas: [],
    ligaId: null,
    detalhe: null,
    jornadaId: null,
    tab: "classificacao"
};

/* ---------------------------------------------------------------- API ---- */

function tokenCsrf() {
    const par = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
    return par ? decodeURIComponent(par.split("=").slice(1).join("=")) : null;
}

async function api(caminho, opcoes = {}) {
    const cabecalhos = { "Content-Type": "application/json" };
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
        OFICIAL: "azul"
    };
    return `<span class="badge ${cores[estadoTexto] || ""}">${texto(estadoTexto)}</span>`;
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

    const linhas = estado.detalhe.equipas
        .filter((equipa) => equipa.estado === "ATIVA" || pontosPorEquipa.has(equipa.id))
        .map((equipa) => {
            const resultado = pontosPorEquipa.get(equipa.id);
            const podeEditar = !fechada && equipa.estado === "ATIVA";
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
        <table>
            <thead>
                <tr>
                    <th class="posicao">Pos</th>
                    <th>Equipa</th>
                    <th class="numero">Pontos</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>${linhas || `<tr><td colspan="4" class="ajuda">Sem equipas ativas.</td></tr>`}</tbody>
        </table>
        ${fechada ? "" : `
            <div class="barra-acoes" style="margin-top:16px">
                <button class="botao primario" data-fechar="${jornada.id}"
                    ${jornada.resultados.length ? "" : "disabled"}>Fechar jornada</button>
                <span class="ajuda">Empates ficam com a mesma posição (desempate manual ainda por implementar).</span>
            </div>`}`;
}

/* --------------------------------------------------------------- ações ---- */

function selecionarTab(tab) {
    estado.tab = tab;
    document.querySelectorAll(".separador")
        .forEach((botao) => botao.classList.toggle("ativo", botao.dataset.tab === tab));
    document.querySelectorAll(".tab")
        .forEach((seccao) => seccao.classList.toggle("oculto", seccao.id !== `tab-${tab}`));
}

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
    const alvo = evento.target.closest("[data-liga], [data-jornada], [data-desistencia], [data-guardar], [data-fechar]");
    if (!alvo) {
        return;
    }

    if (alvo.dataset.liga) {
        estado.ligaId = alvo.dataset.liga;
        estado.jornadaId = null;
        executar(async () => {
            await carregarDetalhe();
            desenharLigas();
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
            mostrarAlerta("Jornada fechada.", "sucesso");
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
