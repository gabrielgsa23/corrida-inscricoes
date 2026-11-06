package br.com.corrida.dto;

/** Resposta do GET /api/inscricoes/resumo: { "total": 3, "km5": 2, "km10": 1 } */
public record ResumoResponse(long total, long km5, long km10) {
}
