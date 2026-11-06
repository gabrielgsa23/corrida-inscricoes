package br.com.corrida.dto;

/**
 * DTO de ENTRADA: o corpo JSON do POST e do PUT.
 *
 * DTO (Data Transfer Object) é a classe que define o formato do JSON, separada da
 * entidade do banco (model/Inscricao). Assim o cliente nunca consegue mandar id,
 * idadeNaProva ou categoria: esses campos simplesmente não existem aqui.
 *
 * "record" é uma classe imutável só com dados; o Jackson (biblioteca de JSON do Spring)
 * usa os nomes dos campos como chaves do JSON.
 *
 * Tudo chega como String de propósito: assim um percurso "KM_7" ou uma data "2023-02-30"
 * chegam até o validador, que devolve uma mensagem clara, em vez de um erro genérico
 * de conversão. No PUT o campo email é ignorado: quem identifica a inscrição é a URL.
 * Campos a mais no JSON (ex.: "categoria") também são ignorados.
 */
public record InscricaoRequest(String nome, String email, String dataNascimento, String percurso) {
}
