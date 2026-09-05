"use strict";

document.getElementById("form-password").addEventListener("submit", (evento) => {
    evento.preventDefault();

    const nova = document.getElementById("nova").value;

    // Comparado aqui e não no servidor: a repetição existe para apanhar uma gralha
    // de quem escreve, e o servidor não tem nada que saber que ela existiu.
    if (nova !== document.getElementById("repetir").value) {
        mostrarErro("As duas passwords novas não são iguais.");
        return;
    }

    submeter("/api/auth/password", {
        atual: document.getElementById("atual").value,
        nova: nova
    }, evento.target);
});

/* Distinto de submeter(): fica nesta página em vez de ir para "/" (aqui não
   há sessão nova a começar), e o botão que desativa é o desta forma — o
   submeter() partilhado agarra sempre o primeiro botão de submissão da
   página, que já não é este se houver dois formulários. */
document.getElementById("form-ligar-treinador").addEventListener("submit", (evento) => {
    evento.preventDefault();

    const botao = evento.target.querySelector("button[type=submit]");
    botao.disabled = true;

    (async () => {
        try {
            const cabecalhos = { "Content-Type": "application/json" };
            const token = tokenCsrf();
            if (token) {
                cabecalhos["X-XSRF-TOKEN"] = token;
            }

            const resposta = await fetch("/api/auth/treinador/ligar", {
                method: "POST",
                headers: cabecalhos,
                body: JSON.stringify({ codigo: document.getElementById("codigo-treinador").value.trim() })
            });

            if (resposta.ok) {
                window.location.href = "/minhas-dividas.html";
                return;
            }

            const erro = await resposta.json().catch(() => null);
            mostrarErro((erro && erro.mensagem) || "Não foi possível ligar o convite.");
        } catch (erro) {
            mostrarErro("Não foi possível contactar o servidor.");
        } finally {
            botao.disabled = false;
        }
    })();
});
