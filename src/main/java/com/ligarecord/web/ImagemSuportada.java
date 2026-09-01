package com.ligarecord.web;

/**
 * Reconhece o formato de uma imagem pelos primeiros bytes.
 *
 * <p>O {@code Content-Type} que vem no pedido é declarado por quem envia — o
 * browser deriva-o da extensão do ficheiro, portanto renomear
 * {@code pagina.html} para {@code logo.png} basta para ele dizer
 * {@code image/png}. Como esses bytes voltam a ser servidos do nosso próprio
 * domínio, aceitar a palavra do cliente era confiar no lado errado. Aqui olha-se
 * para o conteúdo, e é o formato reconhecido que fica guardado.
 *
 * <p>Vantagem lateral: um ficheiro corrompido ou vazio é recusado no momento do
 * upload, em vez de ficar guardado para sempre como uma imagem partida.
 */
public final class ImagemSuportada {

    private ImagemSuportada() {
    }

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /**
     * @return o tipo de conteúdo reconhecido, ou {@code null} se os bytes não
     *         forem PNG, JPEG nem WEBP.
     */
    public static String tipoDe(byte[] dados) {
        if (dados == null) {
            return null;
        }
        if (comecaCom(dados, PNG)) {
            return "image/png";
        }
        if (comecaCom(dados, JPEG)) {
            return "image/jpeg";
        }
        // WEBP: "RIFF" nos primeiros 4 bytes e "WEBP" a partir do 8.º.
        if (dados.length >= 12
                && dados[0] == 'R' && dados[1] == 'I' && dados[2] == 'F' && dados[3] == 'F'
                && dados[8] == 'W' && dados[9] == 'E' && dados[10] == 'B' && dados[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static boolean comecaCom(byte[] dados, byte[] assinatura) {
        if (dados.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (dados[i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
