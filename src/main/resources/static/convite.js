"use strict";

/*
 * A página que o link do convite abre. Faz três coisas por esta ordem: confirma
 * que o convite ainda serve, diz de que equipa se trata, e oferece o caminho
 * certo — criar conta, ou juntar a equipa à conta que a pessoa já tem.
 *
 * O código fica no endereço de propósito. Tirá-lo dali protegia pouco (o link
 * está na conversa onde o gestor o mandou, que dura muito mais do que o
 * histórico do browser) e estragava o caso de quem tem de ir entrar primeiro e
 * voltar a abrir o mesmo link.
 */

const codigo = new URLSearchParams(window.location.search).get("c");

const resumo = document.getElementById("convite-resumo");
const validade = document.getElementById("convite-validade");
const formLigar = document.getElementById("form-ligar");
const formRegisto = document.getElementById("form-registo");
const rodapeLogin = document.getElementById("rodape-login");

function mostrar(elemento) {
    elemento.classList.remove("oculto");
}

function inutilizavel(mensagem) {
    resumo.textContent = mensagem;
    formLigar.classList.add("oculto");
    formRegisto.classList.add("oculto");
    rodapeLogin.classList.add("oculto");
}

/** Datas por extenso, na forma curta que o resto da aplicação usa. */
function data(iso) {
    return new Date(iso).toLocaleDateString("pt-PT", { day: "numeric", month: "long", year: "numeric" });
}

async function iniciar() {
    if (!codigo) {
        inutilizavel("Falta o código. Abre o link tal como ele te foi enviado.");
        return;
    }

    // Um convite inválido, gasto, revogado ou expirado responde tudo o mesmo,
    // e é o mesmo que aqui se diz: não há nada a distinguir para quem chega.
    const resposta = await fetch(`/api/convites-treinador/${encodeURIComponent(codigo)}`);
    if (!resposta.ok) {
        inutilizavel("Este convite já não serve. Pede outro ao gestor da tua liga.");
        return;
    }

    const convite = await resposta.json();
    const liga = convite.ligaNome ? `, na ${convite.ligaNome}` : "";
    resumo.textContent = `${convite.gestorNome} convidou-te para treinar ${convite.equipaNome}${liga}.`;

    if (convite.expiraEm) {
        validade.textContent = `O convite é válido até ${data(convite.expiraEm)}.`;
        mostrar(validade);
    }

    const sessao = await fetch("/api/auth/estado");
    if (sessao.ok) {
        const conta = await sessao.json();
        document.getElementById("ligar-ajuda").textContent =
            `A equipa fica ligada à conta de ${conta.nome}. Não precisas de um segundo login.`;
        mostrar(formLigar);
        return;
    }

    // O nome vem preenchido com o que o gestor escreveu, mas é editável: quem
    // decide como se chama é a pessoa, não a lista de equipas da liga.
    document.getElementById("nome").value = convite.treinadorNome || "";
    mostrar(formRegisto);
    mostrar(rodapeLogin);
}

formRegisto.addEventListener("submit", (evento) => {
    evento.preventDefault();
    submeter("/api/auth/registo-treinador", {
        codigo,
        nome: document.getElementById("nome").value.trim(),
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value
    }, evento.target);
});

formLigar.addEventListener("submit", (evento) => {
    evento.preventDefault();
    submeter("/api/auth/treinador/ligar", { codigo }, evento.target,
        () => { window.location.href = "/minhas-dividas.html"; });
});

iniciar().catch(() => inutilizavel("Não foi possível contactar o servidor."));
