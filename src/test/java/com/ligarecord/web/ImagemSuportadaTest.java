package com.ligarecord.web;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ImagemSuportadaTest {

    @Test
    void reconhecePng() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
        assertThat(ImagemSuportada.tipoDe(png)).isEqualTo("image/png");
    }

    @Test
    void reconheceJpeg() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
        assertThat(ImagemSuportada.tipoDe(jpeg)).isEqualTo("image/jpeg");
    }

    @Test
    void reconheceWebp() {
        byte[] webp = "RIFF????WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
        assertThat(ImagemSuportada.tipoDe(webp)).isEqualTo("image/webp");
    }

    /**
     * O caso que motivou isto: basta renomear um .html para .png e o browser
     * declara image/png. Se o formato fosse decidido pelo cabeçalho, estes bytes
     * ficavam guardados e voltavam a ser servidos do nosso próprio domínio.
     */
    @Test
    void recusaHtmlDisfarcadoDeImagem() {
        byte[] html = "<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        assertThat(ImagemSuportada.tipoDe(html)).isNull();
    }

    @Test
    void recusaSvgPorqueSvgPodeTrazerScripts() {
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'></svg>".getBytes(StandardCharsets.UTF_8);
        assertThat(ImagemSuportada.tipoDe(svg)).isNull();
    }

    @Test
    void recusaFicheiroVazioNuloOuCurtoDeMais() {
        assertThat(ImagemSuportada.tipoDe(null)).isNull();
        assertThat(ImagemSuportada.tipoDe(new byte[0])).isNull();
        assertThat(ImagemSuportada.tipoDe(new byte[]{(byte) 0xFF, (byte) 0xD8})).isNull();
        // "RIFF" sem "WEBP" a seguir é um WAV, não uma imagem.
        assertThat(ImagemSuportada.tipoDe("RIFF????WAVEfmt ".getBytes(StandardCharsets.US_ASCII))).isNull();
    }
}
