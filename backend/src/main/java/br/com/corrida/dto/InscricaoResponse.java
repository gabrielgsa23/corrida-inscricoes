package br.com.corrida.dto;

import br.com.corrida.enums.Categoria;
import br.com.corrida.enums.Percurso;

/**
 * DTO de SAÍDA: a inscrição devolvida no cadastro, listagem, consulta e edição.
 * idadeNaProva e categoria não estão no banco: são calculadas toda vez que a resposta é montada.
 */
public record InscricaoResponse(
        String nome,
        String email,
        String dataNascimento, // sempre aaaa-mm-dd (LocalDate.toString)
        Percurso percurso,
        int idadeNaProva,
        Categoria categoria) {
}
