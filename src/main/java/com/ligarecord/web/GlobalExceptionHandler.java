package com.ligarecord.web;

import com.ligarecord.web.dto.ErroDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroDto> pedidoInvalido(IllegalArgumentException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
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
        log.error("Erro não tratado a processar o pedido", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tenta outra vez.");
    }

    private ResponseEntity<ErroDto> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroDto(status.value(), mensagem));
    }
}
