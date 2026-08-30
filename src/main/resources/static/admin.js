"use strict";

const estado = { eu: null, convites: [], gestores: [] };

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
    if (resposta.status === 403) {
        window.location.href = "/";
        throw new Error("Sem permissões.");
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
    temporizador = setTimeout(() => alerta.classList.add("oculto"), 6000);
}

async function executar(acao) {
    try {
        await acao();
    } catch (erro) {
        mostrarAlerta(erro.message);
    }
}

function data(valor) {
    return valor ? new Date(valor).toLocaleDateString("pt-PT") : "&ndash;";
}

function badge(estadoTexto) {
    const cores = {
        DISPONIVEL: "verde", USADO: "azul", REVOGADO: "vermelho",
        EXPIRADO: "amarelo", ADMIN: "amarelo", GESTOR: ""
    };
    return `<span class="badge ${cores[estadoTexto] ?? ""}">${texto(estadoTexto)}</span>`;
}

/* ------------------------------------------------------------- convites -- */

function desenharConvites() {
    if (!estado.convites.length) {
        $("#tabela-convites").innerHTML = `<p class="ajuda">Ainda não criaste convites.</p>`;
        return;
    }

    $("#tabela-convites").innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Código</th><th>Para quem</th><th>Estado</th>
                    <th>Criado</th><th>Expira</th><th>Usado por</th><th></th>
                </tr>
            </thead>
            <tbody>
                ${estado.convites.map((c) => `
                    <tr>
                        <td>${c.codigo
                            ? `<code class="codigo">${texto(c.codigo)}</code>
                               <button class="botao pequeno" data-copiar="${texto(c.codigo)}">Copiar</button>`
                            : `<span class="ajuda">&mdash;</span>`}</td>
                        <td>${texto(c.nota) || `<span class="ajuda">&mdash;</span>`}</td>
                        <td>${badge(c.estado)}</td>
                        <td>${data(c.criadoEm)}</td>
                        <td>${c.expiraEm ? data(c.expiraEm) : `<span class="ajuda">sem prazo</span>`}</td>
                        <td>${texto(c.usadoPor) || `<span class="ajuda">&mdash;</span>`}</td>
                        <td class="numero">${c.estado === "DISPONIVEL"
                            ? `<button class="botao pequeno perigo" data-revogar="${c.id}">Revogar</button>`
                            : ""}</td>
                    </tr>`).join("")}
            </tbody>
        </table>`;
}

/* ------------------------------------------------------------- gestores -- */

function desenharGestores() {
    $("#tabela-gestores").innerHTML = `
        <table>
            <thead>
                <tr><th>Nome</th><th>Email</th><th>Papel</th><th>Estado</th><th>Desde</th><th></th></tr>
            </thead>
            <tbody>
                ${estado.gestores.map((g) => {
                    const proprio = g.id === estado.eu.id;
                    return `
                    <tr class="${g.ativo ? "" : "linha-desistente"}">
                        <td><strong>${texto(g.nome)}</strong>${proprio ? ` <span class="ajuda">(tu)</span>` : ""}</td>
                        <td>${texto(g.email)}</td>
                        <td>${badge(g.papel)}</td>
                        <td>${g.ativo ? badge("ATIVO") : badge("DESATIVADO")}</td>
                        <td>${data(g.criadoEm)}</td>
                        <td class="numero">
                            ${proprio ? `<span class="ajuda">a tua conta</span>` : `
                                <button class="botao pequeno" data-papel="${g.id}"
                                        data-novo-papel="${g.papel === "ADMIN" ? "GESTOR" : "ADMIN"}">
                                    ${g.papel === "ADMIN" ? "Despromover" : "Tornar admin"}
                                </button>
                                <button class="botao pequeno ${g.ativo ? "perigo" : ""}"
                                        data-estado="${g.id}" data-ativo="${!g.ativo}">
                                    ${g.ativo ? "Desativar" : "Reativar"}
                                </button>`}
                        </td>
                    </tr>`;
                }).join("")}
            </tbody>
        </table>`;
}

/* --------------------------------------------------------------- ações --- */

async function carregar() {
    [estado.convites, estado.gestores] = await Promise.all([
        api("/api/admin/convites"),
        api("/api/admin/gestores")
    ]);
    desenharConvites();
    desenharGestores();
}

$("#form-convite").addEventListener("submit", (evento) => {
    evento.preventDefault();
    executar(async () => {
        const dias = $("#convite-dias").value;
        const convite = await api("/api/admin/convites", {
            method: "POST",
            body: JSON.stringify({
                nota: $("#convite-nota").value.trim() || null,
                diasValidade: dias ? Number(dias) : null
            })
        });
        $("#convite-nota").value = "";
        $("#convite-dias").value = "";
        await carregar();
        mostrarAlerta(`Convite criado: ${convite.codigo}`, "sucesso");
    });
});

$("#btn-sair").addEventListener("click", () => {
    executar(async () => {
        await api("/api/auth/logout", { method: "POST" });
        window.location.href = "/login.html";
    });
});

document.addEventListener("click", (evento) => {
    const alvo = evento.target.closest("[data-revogar], [data-copiar], [data-estado], [data-papel]");
    if (!alvo) {
        return;
    }

    if (alvo.dataset.copiar) {
        navigator.clipboard.writeText(alvo.dataset.copiar)
            .then(() => mostrarAlerta("Código copiado.", "sucesso"))
            .catch(() => mostrarAlerta("Não foi possível copiar. Seleciona o código à mão."));
        return;
    }

    if (alvo.dataset.revogar) {
        if (!confirm("Revogar este convite? Deixa de poder ser usado.")) return;
        executar(async () => {
            await api(`/api/admin/convites/${alvo.dataset.revogar}`, { method: "DELETE" });
            await carregar();
            mostrarAlerta("Convite revogado.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.estado) {
        const ativar = alvo.dataset.ativo === "true";
        if (!confirm(ativar ? "Reativar esta conta?" : "Desativar esta conta? A sessão dela termina já.")) return;
        executar(async () => {
            await api(`/api/admin/gestores/${alvo.dataset.estado}`, {
                method: "PATCH",
                body: JSON.stringify({ ativo: ativar })
            });
            await carregar();
            mostrarAlerta(ativar ? "Conta reativada." : "Conta desativada.", "sucesso");
        });
        return;
    }

    if (alvo.dataset.papel) {
        const novo = alvo.dataset.novoPapel;
        if (!confirm(`Alterar o papel para ${novo}?`)) return;
        executar(async () => {
            await api(`/api/admin/gestores/${alvo.dataset.papel}`, {
                method: "PATCH",
                body: JSON.stringify({ papel: novo })
            });
            await carregar();
            mostrarAlerta("Papel alterado.", "sucesso");
        });
    }
});

/* -------------------------------------------------------------- início --- */

executar(async () => {
    const resposta = await fetch("/api/auth/estado");
    if (!resposta.ok) {
        window.location.href = "/login.html";
        return;
    }
    estado.eu = await resposta.json();
    if (!estado.eu.admin) {
        window.location.href = "/";
        return;
    }
    $("#gestor-nome").textContent = estado.eu.nome;
    await carregar();
});
