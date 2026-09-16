"use strict";

document.getElementById("form-suporte").addEventListener("submit", (evento) => {
    evento.preventDefault();
    const form = evento.target;

    submeter("/api/suporte", {
        nome: document.getElementById("nome").value.trim(),
        email: document.getElementById("email").value.trim(),
        assunto: document.getElementById("assunto").value.trim(),
        mensagem: document.getElementById("mensagem").value.trim()
    }, form, (resposta) => {
        // A mensagem fica guardada do lado do servidor mesmo que o email não
        // saia, por isso diz-se sempre que foi recebida. O que muda é a
        // promessa: com o aviso por enviar, quem tem de responder ainda não
        // sabe que ela existe, e prometer a resposta do costume era prometer
        // o que não se controla.
        const enviado = !resposta || resposta.enviado !== false;
        mostrarAlerta(
            enviado
                ? "Mensagem recebida. Respondemos para o email que deixaste."
                : "Mensagem recebida e guardada, mas o aviso por email falhou. "
                    + "A resposta pode demorar mais do que o costume.",
            enviado ? "sucesso" : "aviso");
        form.reset();
    });
});
