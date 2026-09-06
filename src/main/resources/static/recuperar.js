"use strict";

document.getElementById("form-recuperar").addEventListener("submit", (evento) => {
    evento.preventDefault();
    const form = evento.target;

    submeter("/api/auth/recuperar", {
        email: document.getElementById("email").value.trim()
    }, form, () => {
        // A mensagem é a mesma exista ou não a conta, tal como a resposta do
        // servidor. Dizer "esse email não está registado" era entregar a quem
        // andasse a tentar a lista de quem tem conta aqui.
        mostrarAlerta("Se existir uma conta com esse email, o link já vai a caminho. "
            + "Vê a tua caixa de correio.", "sucesso");
        form.reset();
    });
});
