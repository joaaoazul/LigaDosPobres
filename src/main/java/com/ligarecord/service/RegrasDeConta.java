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
        return normalizado;
    }

    static String nomeValidado(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }
        return nome.trim();
    }

    static void validarPassword(String password) {
        if (password == null || password.length() < MINIMO_PASSWORD) {
            throw new IllegalArgumentException(
                    "A password tem de ter pelo menos " + MINIMO_PASSWORD + " caracteres.");
        }
    }

    static void validarPasswordNova(String password) {
        if (password == null || password.length() < MINIMO_PASSWORD) {
            throw new IllegalArgumentException(
                    "A password nova tem de ter pelo menos " + MINIMO_PASSWORD + " caracteres.");
        }
    }
}
