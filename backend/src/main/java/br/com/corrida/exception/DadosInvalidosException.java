package br.com.corrida.exception;

public class DadosInvalidosException extends RuntimeException {
    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
