package br.com.corrida.enums;

/**
 * Categorias do Art. 6º, IV do regulamento: Juvenil 14–17, Adulto 18–39, Master 40–59, Sênior 60+.
 *
 * A regra de "qual categoria para qual idade" mora no próprio enum: quem precisa da
 * categoria só chama Categoria.porIdade(idade), e a tabela fica num lugar só.
 */
public enum Categoria {
    JUVENIL,
    ADULTO,
    MASTER,
    SENIOR;

    /** A idade já chega como idade completa na data da prova (calculada pelo validador). */
    public static Categoria porIdade(int idade) {
        if (idade < 18) {
            return JUVENIL;
        }
        if (idade < 40) {
            return ADULTO;
        }
        if (idade < 60) {
            return MASTER;
        }
        return SENIOR;
    }
}
