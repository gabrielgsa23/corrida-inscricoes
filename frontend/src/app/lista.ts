import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Inscricao, mensagemDeErro, ROTULO_CATEGORIA, ROTULO_PERCURSO } from './api';

/**
 * Tela da lista (rota "/"): um cartão por inscrição, na ordem que o back-end devolve
 * (ordem de cadastro), com os botões de editar e cancelar.
 */
@Component({
  selector: 'app-lista',
  imports: [RouterLink, DatePipe],
  template: `
    <h2>Lista de inscrições</h2>

    @if (erro(); as erro) {
      <p class="erro-geral" role="alert">{{ erro }}</p>
    }

    <div class="lista">
      <!-- @for repete o cartão; track usa o e-mail porque ele é único. @empty aparece se a lista for []. -->
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
            <!-- O DatePipe trata "aaaa-mm-dd" como data local, então o dia não muda por causa do fuso. -->
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

  /** Pede confirmação e chama DELETE /api/inscricoes/{email}. A lista e o resumo recarregam sozinhos. */
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
