package br.com.corrida.dto;

/** Corpo de toda recusa (400, 404, 409...): { "mensagem": "..." } */
public record ErroResponse(String mensagem) {
}
