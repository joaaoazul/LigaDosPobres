package com.ligarecord.web;

import com.ligarecord.web.dto.ErroDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traduz as excepções do domínio em respostas HTTP:
 * validação -> 400, regra de negócio violada -> 409, recurso inexistente -> 404.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroDto> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroDto> credenciaisInvalidas(CredenciaisInvalidasException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ConviteInvalidoException.class)
    public ResponseEntity<ErroDto> conviteInvalido(ConviteInvalidoException ex) {
        return resposta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroDto> pedidoInvalido(IllegalArgumentException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Corpo que o Jackson não consegue ler: JSON malformado, bytes que não são
     * UTF-8, um texto onde se esperava um número. A culpa é de quem enviou, e um
     * 500 dizia o contrário — mandava o cliente repetir um pedido que nunca vai
     * funcionar, e enchia o log de erros que não são avarias do servidor.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroDto> corpoIlegivel(HttpMessageNotReadableException ex) {
        log.warn("Pedido com corpo ilegível: {}", ex.getMostSpecificCause().getMessage());
        String campo = campoProblematico(ex);
        return resposta(HttpStatus.BAD_REQUEST, campo == null
                ? "O corpo do pedido não é válido."
                : "O valor do campo '" + campo + "' não é válido.");
    }

    /**
     * Nem todo o corpo que o Jackson recusa é JSON malformado: muitas vezes é JSON
     * perfeito com um campo errado — um UUID que não é UUID, um decimal onde se
     * espera um inteiro. Dizer "não é JSON válido" nesses casos manda o cliente
     * procurar no sítio errado, e ele não tem como se corrigir sozinho.
     */
    private String campoProblematico(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException erro && !erro.getPath().isEmpty()) {
            return erro.getPath().get(erro.getPath().size() - 1).getFieldName();
        }
        return null;
    }

    /**
     * Um caminho mal escrito no URL: /api/ligas/isto-nao-e-uuid. É culpa de quem
     * pediu, não do servidor.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroDto> parametroMalFormado(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getName() + "' não tem um formato válido.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroDto> conflito(IllegalStateException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Rede de segurança. Sem isto, uma excepção inesperada devolve ao cliente a
     * mensagem interna — em produção isso revela a stack, os nomes das classes e,
     * conforme o erro, partes da consulta SQL.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroDto> inesperado(Exception ex) {
        // Este @ControllerAdvice corre ANTES do resolvedor por omissão do Spring.
        // Sem esta passagem, o catch-all engolia tudo o que o Spring já sabia
        // classificar e devolvia 500: um caminho inexistente, um verbo errado, um
        // Content-Type errado. Pior do que o estado errado era o registo — cada
        // rastreador que passasse por um endereço que não existe escrevia um
        // stack trace no log como se o servidor tivesse avariado. Ao fim de um
        // mês, ninguém olha para um log assim, e a avaria a sério passa no meio.
        if (ex instanceof ErrorResponse conhecido) {
            HttpStatus estado = HttpStatus.valueOf(conhecido.getStatusCode().value());
            log.warn("Pedido recusado com {}: {}", estado.value(), ex.getMessage());
            return resposta(estado, switch (estado) {
                case NOT_FOUND -> "O recurso pedido não existe.";
                case METHOD_NOT_ALLOWED -> "Método não permitido neste endereço.";
                case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado.";
                default -> "O pedido não pôde ser processado.";
            });
        }

        log.error("Erro não tratado a processar o pedido", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tenta outra vez.");
    }

    private ResponseEntity<ErroDto> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroDto(status.value(), mensagem));
    }
}
