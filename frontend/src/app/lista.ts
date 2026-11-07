import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Inscricao, mensagemDeErro, ROTULO_CATEGORIA, ROTULO_PERCURSO } from './api';

@Component({
  selector: 'app-lista',
  imports: [RouterLink, DatePipe],
  template: `
    <h2>Lista de inscrições</h2>

    @if (erro(); as erro) {
      <p class="erro-geral" role="alert">{{ erro }}</p>
    }

    <div class="lista">
      @for (inscricao of api.inscricoes(); track inscricao.email) {
        <article class="cartao">
          <header class="cartao-topo">
            <h3>{{ inscricao.nome }}</h3>
            <span class="selo" [class.selo-10]="inscricao.percurso === 'KM_10'">
              {{ rotuloPercurso[inscricao.percurso] }}
            </span>
          </header>

          <dl class="cartao-dados">
            <div><dt>E-mail</dt><dd>{{ inscricao.email }}</dd></div>
            <div><dt>Nascimento</dt><dd>{{ inscricao.dataNascimento | date: 'dd/MM/yyyy' }}</dd></div>
            <div><dt>Idade na prova</dt><dd>{{ inscricao.idadeNaProva }} anos</dd></div>
            <div><dt>Categoria</dt><dd>{{ rotuloCategoria[inscricao.categoria] }}</dd></div>
          </dl>

          <div class="cartao-acoes">
            <a [routerLink]="['/editar', inscricao.email]">Editar</a>
            <button type="button" class="botao-texto perigo" (click)="cancelar(inscricao)">
              Cancelar inscrição
            </button>
          </div>
        </article>
      } @empty {
        <p class="painel vazio">
          Nenhuma inscrição por enquanto. <a routerLink="/nova">Fazer a primeira</a>
        </p>
      }
    </div>
  `,
})
export class Lista {
  protected readonly api = inject(Api);
  protected readonly erro = signal<string | null>(null);
  protected readonly rotuloPercurso = ROTULO_PERCURSO;
  protected readonly rotuloCategoria = ROTULO_CATEGORIA;

  protected async cancelar(inscricao: Inscricao): Promise<void> {
    if (!confirm(`Cancelar a inscrição de ${inscricao.nome} (${inscricao.email})?`)) return;

    this.erro.set(null);
    try {
      await this.api.cancelar(inscricao.email);
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    }
  }
}
