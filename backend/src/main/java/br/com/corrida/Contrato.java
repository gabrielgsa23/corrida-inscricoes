package br.com.corrida;

import br.com.corrida.RegrasDaInscricao.Categoria;
import br.com.corrida.RegrasDaInscricao.Percurso;

/**
 * O formato de tudo que ENTRA e SAI da API em JSON, num arquivo só,
 * espelhando a seção "Contrato da API" do enunciado.
 *
 * São "records": classes imutáveis só com dados. O Jackson (biblioteca de JSON
 * do Spring) usa os nomes dos campos como nomes das chaves do JSON, por isso
 * os nomes aqui são exatamente os do contrato (dataNascimento, idadeNaProva...).
 *
 * Separar isso da entidade Inscricao (a tabela do banco) é proposital:
 * o cliente nunca consegue mandar idadeNaProva, categoria ou id, porque esses
 * campos simplesmente não existem no record de entrada.
 */
public final class Contrato {

    private Contrato() {
        // só agrupa os records, não é para ser instanciada
    }

    /**
     * Corpo do POST e do PUT. Tudo chega como String de propósito: assim um percurso
     * "KM_7" ou uma data "2023-02-30" chegam até a nossa validação, que devolve uma
     * mensagem clara, em vez de estourar um erro genérico de conversão do Jackson.
     * No PUT o campo email é ignorado: quem identifica a inscrição é o e-mail da URL.
     * Campos que o cliente mande a mais (ex.: "categoria") também são ignorados.
     */
    public record DadosInscricao(String nome, String email, String dataNascimento, String percurso) {}

    /** Inscrição devolvida no cadastro, listagem, consulta e edição. */
    public record InscricaoResposta(
            String nome,
            String email,
            String dataNascimento, // sempre aaaa-mm-dd (LocalDate.toString)
            Percurso percurso,
            int idadeNaProva,
            Categoria categoria) {}

    /** Resposta do GET /api/inscricoes/resumo: { "total": 3, "km5": 2, "km10": 1 } */
    public record Resumo(long total, long km5, long km10) {}

    /** Corpo de toda recusa: { "mensagem": "..." } */
    public record Mensagem(String mensagem) {}
}
