package br.com.corrida.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("Já existe uma inscrição ativa para este e-mail.");
    }
}
