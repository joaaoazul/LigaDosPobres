"use strict";

const estado = { eu: null, dividas: null };

const $ = (s) => document.querySelector(s);

function texto(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, (c) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
}

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

let temporizador = null;

function mostrarAlerta(mensagem, tipo = "erro") {
    const alerta = $("#alerta");
    alerta.textContent = mensagem;
    alerta.className = `alerta ${tipo === "sucesso" ? "sucesso" : ""}`;
    clearTimeout(temporizador);
    temporizador = setTimeout(() => alerta.classList.add("oculto"), 4500);
}

async function executar(acao) {
    try {
        await acao();
    } catch (erro) {
        mostrarAlerta(erro.message);
    }
}

function formatoMoeda(valor) {
    return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(valor ?? 0);
}

function badge(estadoTexto) {
    const cores = { PENDENTE: "amarelo", RESOLVIDA: "verde" };
    const nomes = { SEM_DIVIDA: "Sem dívida" };
    return `<span class="badge ${cores[estadoTexto] || ""}">${texto(nomes[estadoTexto] || estadoTexto)}</span>`;
}

function badgeTipoBloco(tipo) {
    return tipo === "INSCRICAO"
        ? `<span class="badge azul">Inscrição</span>`
        : `<span class="badge">Período</span>`;
}

function desenharDividas() {
    const equipas = estado.dividas.equipas;
    $("#total-geral").textContent = formatoMoeda(estado.dividas.totalGeral);

    if (!equipas.length) {
        $("#lista-minhas-equipas").innerHTML = `<p class="ajuda">Ainda não treinas nenhuma equipa.</p>`;
        return;
    }

    $("#lista-minhas-equipas").classList.add("pilha");
    $("#lista-minhas-equipas").innerHTML = equipas.map((divida) => `
        <div class="detalhe-jornada">
            <h3>${texto(divida.equipaNome)} <span class="ajuda">&middot; ${texto(divida.ligaNome)}</span></h3>
            <p class="ajuda">Total pendente: <strong>${formatoMoeda(divida.totalPendente)}</strong> ${badge(divida.estado)}</p>
            ${divida.blocos.length ? `
                <div class="tabela-rolavel">
                    <table>
                        <thead>
                            <tr><th class="numero">Nº</th><th>Tipo</th><th class="numero">Valor</th><th>Estado</th></tr>
                        </thead>
                        <tbody>
                            ${divida.blocos.map((bloco) => `
                                <tr>
                                    <td class="numero">${bloco.numeroBloco}</td>
                                    <td>${badgeTipoBloco(bloco.tipo)}</td>
                                    <td class="numero">${formatoMoeda(bloco.valor)}</td>
                                    <td>${badge(bloco.estado)}</td>
                                </tr>`).join("")}
                        </tbody>
                    </table>
                </div>` : `<p class="ajuda">Ainda não há blocos.</p>`}
        </div>`).join("");
}

$("#btn-sair").addEventListener("click", () => {
    executar(async () => {
        await api("/api/auth/logout", { method: "POST" });
        window.location.href = "/login.html";
    });
});

executar(async () => {
    const resposta = await fetch("/api/auth/estado");
    if (!resposta.ok) {
        window.location.href = "/login.html";
        return;
    }
    estado.eu = await resposta.json();
    $("#gestor-nome").textContent = estado.eu.nome;
    if (estado.eu.admin) {
        $("#link-admin").classList.remove("oculto");
    }

    estado.dividas = await api("/api/minhas-dividas");
    desenharDividas();
});
