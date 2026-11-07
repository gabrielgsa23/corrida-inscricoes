package br.com.corrida.exception;

public class InscricaoNaoEncontradaException extends RuntimeException {
    public InscricaoNaoEncontradaException() {
        super("Não existe inscrição ativa para este e-mail.");
    }
}
