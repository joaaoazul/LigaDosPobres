"use strict";

/** Lê o token CSRF que o servidor deixou em cookie, para o reenviar no cabeçalho. */
function tokenCsrf() {
    const par = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
    return par ? decodeURIComponent(par.split("=").slice(1).join("=")) : null;
}

function mostrarErro(mensagem) {
    const alerta = document.getElementById("alerta");
    alerta.textContent = mensagem;
    alerta.className = "alerta";
}

async function submeter(caminho, corpo) {
    const botao = document.querySelector("button[type=submit]");
    botao.disabled = true;
    try {
        const cabecalhos = { "Content-Type": "application/json" };
        const token = tokenCsrf();
        if (token) {
            cabecalhos["X-XSRF-TOKEN"] = token;
        }

        const resposta = await fetch(caminho, {
            method: "POST",
            headers: cabecalhos,
            body: JSON.stringify(corpo)
        });

        if (resposta.ok) {
            window.location.href = "/";
            return;
        }

        const erro = await resposta.json().catch(() => null);
        mostrarErro((erro && erro.mensagem) || "Não foi possível continuar.");
    } catch (erro) {
        mostrarErro("Não foi possível contactar o servidor.");
    } finally {
        botao.disabled = false;
    }
}

// Uma primeira leitura garante que o cookie XSRF-TOKEN existe antes do POST.
fetch("/api/auth/estado").catch(() => {});
