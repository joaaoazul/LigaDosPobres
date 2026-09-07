package com.ligarecord.email;

/**
 * Os textos e o desenho das mensagens que a aplicação envia.
 *
 * <p>Cada mensagem leva sempre as duas versões, a de texto e a de HTML. Não é
 * só boa educação: quem recebe em texto simples, ou num cliente que recusa
 * HTML, tem de continuar a conseguir recuperar a password, e um email só com
 * HTML costuma ser tratado com mais desconfiança pelos filtros de spam.
 *
 * <p><b>O HTML de email não é HTML de página.</b> O que aqui está parece
 * antiquado de propósito:
 *
 * <ul>
 *   <li><b>Tabelas para a disposição</b>, porque metade dos clientes ignora
 *       flexbox e grid, e o Outlook desenha com o motor do Word.
 *   <li><b>Estilos em linha</b>, porque folhas de estilo e a etiqueta
 *       {@code <style>} são deitadas fora por vários clientes, o Gmail
 *       incluído em algumas vistas.
 *   <li><b>Sem os tipos de letra da aplicação.</b> Um {@code @font-face} não
 *       carrega em lado nenhum aqui; fica a pilha do sistema, e o ar de
 *       família vem das cores, das versaletes e do espaçamento.
 *   <li><b>Sem imagens.</b> A maioria dos clientes não as mostra sem
 *       autorização, e um email cujo cabeçalho é uma imagem bloqueada aparece
 *       vazio. O emblema é desenhado com bordas arredondadas.
 *   <li><b>O botão é uma célula de tabela com cor de fundo</b>, e não um
 *       {@code <a>} com padding, que o Outlook desenharia sem o fundo.
 * </ul>
 */
public final class ModeloDeEmail {

    /** Grafite, laranja de sinal e cinzentos: os mesmos da aplicação. */
    private static final String FUNDO = "#eceef1";
    private static final String CARTAO = "#16181c";
    private static final String SINAL = "#ff5a1f";
    private static final String TINTA = "#f0f1f3";
    private static final String TINTA_MEDIA = "#a0a7b0";
    private static final String TINTA_FRACA = "#6e757e";
    private static final String REGUA = "#2b2f35";

    private static final String LETRA =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";
    private static final String LETRA_ESTREITA =
            "'Arial Narrow','Helvetica Neue',Helvetica,Arial,sans-serif";

    private ModeloDeEmail() {
    }

    /** O assunto, o corpo em texto e o corpo em HTML de uma mensagem. */
    public record Mensagem(String assunto, String texto, String html) {
    }

    public static Mensagem recuperacaoDePassword(String nome, String link, String validade) {
        String assunto = "Recuperar a password da Quota";

        String texto = "Olá " + nome + ",\n\n"
                + "Alguém pediu para redefinir a password da tua conta na Quota.\n"
                + "Se foste tu, abre este link:\n\n"
                + link + "\n\n"
                + "O link serve uma vez e expira " + validade + ".\n\n"
                + "Se não foste tu, não tens de fazer nada: sem este link ninguém\n"
                + "muda a password, e a que tens continua a servir.\n";

        String html = pagina(
                "Redefinir a password da tua conta na Quota.",
                bloco(
                        "<p style=\"margin:0 0 16px;font-size:15px;line-height:1.55;color:" + TINTA + ";\">"
                                + "Olá " + escapar(nome) + ","
                                + "</p>"
                                + "<p style=\"margin:0 0 24px;font-size:15px;line-height:1.55;color:" + TINTA_MEDIA + ";\">"
                                + "Alguém pediu para redefinir a password da tua conta. Se foste tu, "
                                + "carrega no botão. O link serve <strong style=\"color:" + TINTA + ";\">uma vez</strong> "
                                + "e expira " + escapar(validade) + "."
                                + "</p>"
                                + botao(link, "Escolher password nova")
                                + "<p style=\"margin:24px 0 6px;font-size:12px;line-height:1.5;color:" + TINTA_FRACA + ";\">"
                                + "Se o botão não funcionar, copia este endereço para o browser:"
                                + "</p>"
                                + "<p style=\"margin:0;font-size:12px;line-height:1.5;word-break:break-all;\">"
                                + "<a href=\"" + escapar(link) + "\" style=\"color:" + SINAL + ";text-decoration:none;\">"
                                + escapar(link) + "</a>"
                                + "</p>"),
                "Se não foste tu que pediste isto, não tens de fazer nada. "
                        + "Sem este link ninguém muda a password, e a que tens continua a servir.");

        return new Mensagem(assunto, texto, html);
    }

    /**
     * O convite para treinar uma equipa.
     *
     * <p>Diz sempre quem convidou e para que equipa, e a última linha diz a
     * quem não estava à espera disto o que fazer: nada. O endereço foi escrito
     * pelo gestor, e pode ter-se enganado — o email tem de fazer sentido para
     * quem o recebe por engano, não só para o destinatário certo.
     */
    public static Mensagem conviteDeTreinador(String nomeTreinador, String nomeGestor,
                                              String nomeEquipa, String nomeLiga,
                                              String link, String validade) {
        String equipaELiga = nomeLiga == null || nomeLiga.isBlank()
                ? nomeEquipa
                : nomeEquipa + ", na " + nomeLiga;

        String assunto = "Convite para treinar " + nomeEquipa + " na Quota";

        String texto = "Olá " + nomeTreinador + ",\n\n"
                + nomeGestor + " convidou-te para treinar " + equipaELiga + ".\n"
                + "A Quota é onde essa liga trata das jornadas, da classificação e das quotas.\n\n"
                + "Aceita aqui:\n\n"
                + link + "\n\n"
                + "O convite serve uma vez e expira " + validade + ".\n"
                + "Se já tiveres conta na Quota, o mesmo link junta a equipa a essa conta,\n"
                + "sem criares um segundo início de sessão.\n\n"
                + "Se isto não te diz respeito, ignora esta mensagem: sem carregares no\n"
                + "link não fica nada em teu nome.\n";

        String html = pagina(
                nomeGestor + " convidou-te para treinar " + nomeEquipa + ".",
                bloco(
                        "<p style=\"margin:0 0 16px;font-size:15px;line-height:1.55;color:" + TINTA + ";\">"
                                + "Olá " + escapar(nomeTreinador) + ","
                                + "</p>"
                                + "<p style=\"margin:0 0 24px;font-size:15px;line-height:1.55;color:" + TINTA_MEDIA + ";\">"
                                + escapar(nomeGestor) + " convidou-te para treinar "
                                + "<strong style=\"color:" + TINTA + ";\">" + escapar(equipaELiga) + "</strong>. "
                                + "O convite serve <strong style=\"color:" + TINTA + ";\">uma vez</strong> "
                                + "e expira " + escapar(validade) + "."
                                + "</p>"
                                + botao(link, "Aceitar o convite")
                                + "<p style=\"margin:24px 0 6px;font-size:12px;line-height:1.5;color:" + TINTA_FRACA + ";\">"
                                + "Se o botão não funcionar, copia este endereço para o browser:"
                                + "</p>"
                                + "<p style=\"margin:0 0 16px;font-size:12px;line-height:1.5;word-break:break-all;\">"
                                + "<a href=\"" + escapar(link) + "\" style=\"color:" + SINAL + ";text-decoration:none;\">"
                                + escapar(link) + "</a>"
                                + "</p>"
                                + "<p style=\"margin:0;font-size:12px;line-height:1.5;color:" + TINTA_FRACA + ";\">"
                                + "Já tens conta na Quota? O mesmo link junta esta equipa a essa conta, "
                                + "sem criares um segundo início de sessão."
                                + "</p>"),
                "Se isto não te diz respeito, ignora esta mensagem. Sem carregares no link "
                        + "não fica nada em teu nome.");

        return new Mensagem(assunto, texto, html);
    }

    /**
     * O invólucro: fundo claro, cartão escuro com a régua de sinal no topo, a
     * marca, o conteúdo e o rodapé.
     *
     * <p>O texto de pré-visualização é o que os clientes mostram na lista, a
     * seguir ao assunto. Sem ele apanham a primeira linha do corpo, que aqui
     * seria "Olá fulano" e não diz nada a quem está a decidir se abre.
     */
    private static String pagina(String previsualizacao, String conteudo, String rodape) {
        return "<!doctype html><html lang=\"pt\"><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<meta name=\"color-scheme\" content=\"light dark\">"
                + "<meta name=\"supported-color-schemes\" content=\"light dark\">"
                + "<title>Quota</title>"
                + "</head>"
                + "<body style=\"margin:0;padding:0;background:" + FUNDO + ";\">"

                // Fora do ecrã, mas lido pelo cliente para a pré-visualização.
                + "<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;\">"
                + escapar(previsualizacao)
                + "</div>"

                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
                + " style=\"background:" + FUNDO + ";\">"
                + "<tr><td align=\"center\" style=\"padding:32px 16px;\">"

                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
                + " style=\"width:480px;max-width:100%;background:" + CARTAO + ";border-radius:6px;\">"

                // A régua de sinal, a única coisa gritada na mensagem inteira.
                + "<tr><td style=\"background:" + SINAL + ";height:3px;line-height:3px;font-size:0;"
                + "border-radius:6px 6px 0 0;\">&nbsp;</td></tr>"

                + "<tr><td style=\"padding:32px 32px 0;\" align=\"center\">" + marca() + "</td></tr>"
                + "<tr><td style=\"padding:28px 32px 0;\">" + conteudo + "</td></tr>"

                + "<tr><td style=\"padding:28px 32px 32px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
                + "<tr><td style=\"border-top:1px solid " + REGUA + ";padding-top:16px;"
                + "font-family:" + LETRA + ";font-size:12px;line-height:1.5;color:" + TINTA_FRACA + ";\">"
                + escapar(rodape)
                + "</td></tr></table>"
                + "</td></tr>"

                + "</table>"

                + "<p style=\"margin:16px 0 0;font-family:" + LETRA + ";font-size:11px;color:#8b939d;\">"
                + "Quota &middot; gestão de ligas amadoras"
                + "</p>"

                + "</td></tr></table>"
                + "</body></html>";
    }

    /**
     * O emblema e o nome. O anel com o arco laranja da aplicação não sobrevive
     * a um email (SVG é ignorado, PNG é bloqueado até se autorizarem imagens),
     * por isso fica um anel desenhado com bordas: um círculo com o topo em
     * laranja e o resto apagado, que é a mesma ideia.
     */
    private static String marca() {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>"
                + "<td align=\"center\">"
                + "<div style=\"width:34px;height:34px;border-radius:50%;"
                + "border:3px solid " + REGUA + ";border-top-color:" + SINAL + ";"
                + "font-size:0;line-height:0;\">&nbsp;</div>"
                + "<div style=\"font-family:" + LETRA_ESTREITA + ";font-size:19px;font-weight:bold;"
                + "letter-spacing:1.5px;color:" + TINTA + ";padding-top:12px;\">QUOTA</div>"
                + "<div style=\"font-family:" + LETRA_ESTREITA + ";font-size:11px;letter-spacing:2.5px;"
                + "color:" + TINTA_FRACA + ";padding-top:5px;\">SISTEMA DE GEST&Atilde;O</div>"
                + "</td></tr></table>";
    }

    private static String bloco(String html) {
        return "<div style=\"font-family:" + LETRA + ";\">" + html + "</div>";
    }

    /**
     * Botão à prova de Outlook: o fundo está na célula da tabela, não no
     * {@code <a>}. O Outlook desenha com o motor do Word e ignora padding e
     * background num {@code <a>}, o que dava um link azul sublinhado onde devia
     * estar um botão laranja.
     */
    private static String botao(String link, String texto) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">"
                + "<tr><td align=\"center\" bgcolor=\"" + SINAL + "\""
                + " style=\"background:" + SINAL + ";border-radius:4px;\">"
                + "<a href=\"" + escapar(link) + "\""
                + " style=\"display:block;padding:14px 24px;font-family:" + LETRA_ESTREITA + ";"
                + "font-size:16px;font-weight:bold;letter-spacing:0.3px;color:#14150f;"
                + "text-decoration:none;\">"
                + escapar(texto)
                + "</a></td></tr></table>";
    }

    /**
     * O nome vem do que a pessoa escreveu no registo, e vai para dentro de
     * HTML. Sem escapar, um nome com {@code <} ou {@code "} desfazia a
     * mensagem, e é o género de coisa que só se descobre no dia em que alguém
     * se chama qualquer coisa com um sinal desses.
     */
    static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
