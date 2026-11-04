import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

// ---------------------------------------------------------------------------
// Tipos: espelham o contrato da API (mesmos nomes de campo do back-end).
// ---------------------------------------------------------------------------

export type Percurso = 'KM_5' | 'KM_10';
export type Categoria = 'JUVENIL' | 'ADULTO' | 'MASTER' | 'SENIOR';

/** Inscrição como o back-end devolve. Idade e categoria vêm calculadas de lá. */
export interface Inscricao {
  nome: string;
  email: string;
  dataNascimento: string; // aaaa-mm-dd
  percurso: Percurso;
  idadeNaProva: number;
  categoria: Categoria;
}

/** O que o formulário envia. Na edição o back-end ignora o e-mail do corpo. */
export interface DadosInscricao {
  nome: string;
  email: string;
  dataNascimento: string;
  percurso: string;
}

export interface Resumo {
  total: number;
  km5: number;
  km10: number;
}

// Texto que aparece na tela para cada código do back-end.
export const ROTULO_PERCURSO: Record<Percurso, string> = { KM_5: '5 km', KM_10: '10 km' };
export const ROTULO_CATEGORIA: Record<Categoria, string> = {
  JUVENIL: 'Juvenil',
  ADULTO: 'Adulto',
  MASTER: 'Master',
  SENIOR: 'Sênior',
};

// ---------------------------------------------------------------------------
// Serviço: único lugar do front que faz requisições HTTP.
// ---------------------------------------------------------------------------

// O endereço é relativo ("/api/..."). Em desenvolvimento, o "ng serve" repassa
// tudo que começa com /api para http://localhost:8080 (arquivo proxy.conf.json).
// Por isso o back-end não precisa de configuração de CORS.
const URL_BASE = '/api/inscricoes';

/**
 * Guarda a lista e o resumo em signals (valores reativos: quando mudam, a tela
 * se redesenha sozinha) e expõe uma função para cada rota do contrato.
 *
 * Não existe regra de negócio aqui: quem valida, calcula idade e categoria e
 * conta o resumo é o back-end. O front só mostra o resultado.
 *
 * Uso async/await com firstValueFrom (transforma o Observable do HttpClient
 * em Promise) porque fica mais fácil de ler do que encadear subscribe().
 */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  readonly inscricoes = signal<Inscricao[]>([]);
  readonly resumo = signal<Resumo>({ total: 0, km5: 0, km10: 0 });

  /** Busca lista e resumo de novo. Chamado ao abrir o site e depois de cada alteração. */
  async recarregar(): Promise<void> {
    // Promise.all dispara as duas requisições ao mesmo tempo e espera as duas.
    const [lista, resumo] = await Promise.all([
      firstValueFrom(this.http.get<Inscricao[]>(URL_BASE)),
      firstValueFrom(this.http.get<Resumo>(`${URL_BASE}/resumo`)),
    ]);
    this.inscricoes.set(lista);
    this.resumo.set(resumo);
  }

  consultar(email: string): Promise<Inscricao> {
    return firstValueFrom(this.http.get<Inscricao>(urlDe(email)));
  }

  async cadastrar(dados: DadosInscricao): Promise<void> {
    await firstValueFrom(this.http.post(URL_BASE, dados));
    await this.recarregar();
  }

  async editar(email: string, dados: DadosInscricao): Promise<void> {
    await firstValueFrom(this.http.put(urlDe(email), dados));
    await this.recarregar();
  }

  async cancelar(email: string): Promise<void> {
    await firstValueFrom(this.http.delete(urlDe(email)));
    await this.recarregar();
  }
}

// encodeURIComponent protege caracteres especiais do e-mail na URL (o "+" vira %2B, por exemplo).
function urlDe(email: string): string {
  return `${URL_BASE}/${encodeURIComponent(email)}`;
}

/**
 * Transforma um erro do HttpClient em texto para a tela.
 * Toda recusa do back-end vem como { "mensagem": "..." }, então basta mostrá-la.
 */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    if (typeof erro.error?.mensagem === 'string') {
      return erro.error.mensagem;
    }
    // Status 0 = sem resposta. 5xx sem "mensagem" = o proxy do ng serve não achou o back-end.
    if (erro.status === 0 || erro.status >= 500) {
      return 'Não foi possível falar com o servidor. Confira se o back-end está rodando na porta 8080.';
    }
  }
  return 'Ocorreu um erro inesperado. Tente novamente.';
}
