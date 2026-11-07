import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type Percurso = 'KM_5' | 'KM_10';
export type Categoria = 'JUVENIL' | 'ADULTO' | 'MASTER' | 'SENIOR';

export interface Inscricao {
  nome: string;
  email: string;
  dataNascimento: string;
  percurso: Percurso;
  idadeNaProva: number;
  categoria: Categoria;
}

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

export const ROTULO_PERCURSO: Record<Percurso, string> = { KM_5: '5 km', KM_10: '10 km' };
export const ROTULO_CATEGORIA: Record<Categoria, string> = {
  JUVENIL: 'Juvenil',
  ADULTO: 'Adulto',
  MASTER: 'Master',
  SENIOR: 'Sênior',
};

const URL_BASE = '/api/inscricoes';

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  readonly inscricoes = signal<Inscricao[]>([]);
  readonly resumo = signal<Resumo>({ total: 0, km5: 0, km10: 0 });

  async recarregar(): Promise<void> {
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

function urlDe(email: string): string {
  return `${URL_BASE}/${encodeURIComponent(email)}`;
}

export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    if (typeof erro.error?.mensagem === 'string') {
      return erro.error.mensagem;
    }

    if (erro.status === 0 || erro.status >= 500) {
      return 'Não foi possível falar com o servidor. Confira se o back-end está rodando na porta 8080.';
    }
  }
  return 'Ocorreu um erro inesperado. Tente novamente.';
}
