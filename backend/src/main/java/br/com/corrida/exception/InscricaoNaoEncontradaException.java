package br.com.corrida.exception;

/** 404 Not Found: não existe inscrição ativa para o e-mail da URL (consulta, edição e cancelamento). */
public class InscricaoNaoEncontradaException extends RuntimeException {

    public InscricaoNaoEncontradaException() {
        super("Não existe inscrição ativa para este e-mail.");
    }
}
