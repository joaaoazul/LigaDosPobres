package com.ligarecord.service;

/**
 * Regras comuns a todas as contas com credenciais — de gestor e de treinador.
 *
 * <p>Existe para a política de passwords ter um só sítio. Enquanto estava
 * copiada em cada serviço, era só uma questão de tempo até alguém apertar o
 * mínimo num lado e deixar o outro por mudar, sem nada a assinalar a diferença.
 */
final class RegrasDeConta {

    /** Validação deliberadamente permissiva: só rejeita o que é obviamente inválido. */
    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    static final int MINIMO_PASSWORD = 10;

    /**
     * Os limites das colunas de texto, do lado de cá.
     *
     * <p>Sem isto, um nome comprido de mais só era travado pela base de dados, e
     * a violação chegava ao utilizador como "Isto foi alterado por outro pedido
     * ao mesmo tempo. Tenta outra vez." — uma mensagem sobre uma corrida, para
     * quem só escreveu um nome grande, e um convite a tentar outra vez para
     * sempre.
     */
    static final int MAXIMO_NOME = 120;
    static final int MAXIMO_EMAIL = 180;

    private RegrasDeConta() {
    }

    /**
     * Normaliza e valida o email. Minúsculas para que não existam duas contas
     * que só diferem em maiúsculas — o utilizador não distingue as duas, e o
     * login também não deveria.
     */
    static String emailNormalizado(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O email é obrigatório.");
        }
        String normalizado = email.trim().toLowerCase();
        if (!EMAIL.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("O email não é válido.");
        }
        if (normalizado.length() > MAXIMO_EMAIL) {
            throw new IllegalArgumentException(
                    "O email não pode ter mais de " + MAXIMO_EMAIL + " caracteres.");
        }
        return normalizado;
    }

    static String nomeValidado(String nome) {
        return textoValidado(nome, MAXIMO_NOME, "O nome");
    }

    /** Obrigatório, sem espaços à volta, e dentro do que a coluna aguenta. */
    static String textoValidado(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        String limpo = valor.trim();
        if (limpo.length() > maximo) {
            throw new IllegalArgumentException(
                    campo + " não pode ter mais de " + maximo + " caracteres.");
        }
        return limpo;
    }

    static void validarPassword(String password) {
        validarComprimento(password, "A password tem de ter pelo menos " + MINIMO_PASSWORD + " caracteres.");
    }

    static void validarPasswordNova(String password) {
        validarComprimento(password, "A password nova tem de ter pelo menos " + MINIMO_PASSWORD + " caracteres.");
    }

    private static void validarComprimento(String password, String mensagemErro) {
        if (password == null || password.length() < MINIMO_PASSWORD) {
            throw new IllegalArgumentException(mensagemErro);
        }
    }
}
