package br.com.corrida.exception;

/** 409 Conflict: o e-mail já tem inscrição ativa (só acontece no cadastro). */
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("Já existe uma inscrição ativa para este e-mail.");
    }
}
