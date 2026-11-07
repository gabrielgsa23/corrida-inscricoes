package br.com.corrida.enums;

public enum Categoria {
    JUVENIL,
    ADULTO,
    MASTER,
    SENIOR;

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
