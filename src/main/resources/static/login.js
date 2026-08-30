"use strict";

document.getElementById("form-login").addEventListener("submit", (evento) => {
    evento.preventDefault();
    submeter("/api/auth/login", {
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value
    });
});
