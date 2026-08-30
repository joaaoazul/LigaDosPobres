"use strict";

document.getElementById("form-registo").addEventListener("submit", (evento) => {
    evento.preventDefault();
    submeter("/api/auth/registo", {
        codigo: document.getElementById("codigo").value.trim(),
        nome: document.getElementById("nome").value.trim(),
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value
    });
});
