"use strict";

const codigo = new URLSearchParams(window.location.search).get("codigo");
const formulario = document.getElementById("form-nova-password");

// Sem código no endereço não há nada a fazer aqui: mais vale dizê-lo já do que
// deixar escrever uma password para depois falhar no envio.
if (!codigo) {
    mostrarErro("Falta o código. Abre o link tal como ele veio no email.");
    formulario.querySelector("button[type=submit]").disabled = true;
}

formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();

    submeter("/api/auth/recuperar/confirmar", {
        codigo,
        password: document.getElementById("password").value
    }, evento.target, () => {
        // Redefinir não abre sessão: quem chega aqui não tinha nenhuma, e o
        // servidor acabou de cortar as que houvesse. Vai entrar com a nova.
        window.location.href = "/login.html";
    });
});
