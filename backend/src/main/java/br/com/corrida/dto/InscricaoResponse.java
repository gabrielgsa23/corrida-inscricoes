package br.com.corrida.dto;

import br.com.corrida.enums.Categoria;
import br.com.corrida.enums.Percurso;

public record InscricaoResponse(
        String nome,
        String email,
        String dataNascimento,
        Percurso percurso,
        int idadeNaProva,
        Categoria categoria) {
}
