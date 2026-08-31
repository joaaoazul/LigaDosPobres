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
    });
});
