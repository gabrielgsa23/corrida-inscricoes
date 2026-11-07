import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Api, mensagemDeErro } from './api';

type Tema = 'claro' | 'escuro';
const CHAVE_TEMA = 'corrida.tema.v1';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="topo">
      <div class="container">
        <div class="topo-linha">
          <div class="marca">
            <img src="logo.png" alt="" class="logo" />
            <p class="topo-evento">5ª Corrida Solidária da Vila Esperança</p>
          </div>
          <button type="button" class="botao-tema" (click)="alternarTema()">
            {{ tema() === 'escuro' ? '☀ Modo claro' : '☾ Modo escuro' }}
          </button>
        </div>
        <h1>Inscrições</h1>
        <p class="topo-data">Domingo, 14/03/2027 · largada às 7h · Praça da Matriz</p>

        <ul class="resumo" aria-label="Resumo das inscrições">
          <li>Inscritos <strong>{{ api.resumo().total }}</strong></li>
          <li>5 km <strong>{{ api.resumo().km5 }}</strong></li>
          <li>10 km <strong>{{ api.resumo().km10 }}</strong></li>
        </ul>
      </div>
    </header>

    <div class="container">
      @if (aviso(); as aviso) {
        <p class="aviso" role="alert">{{ aviso }}</p>
      }

      <nav class="navegacao">
        <a routerLink="/">Todas as inscrições</a>
        <a routerLink="/nova" class="botao">+ Nova inscrição</a>
      </nav>

      <main>
        <router-outlet />
      </main>
    </div>
  `,
})
export class App {
  protected readonly api = inject(Api);

  protected readonly aviso = signal<string | null>(null);

  protected readonly tema = signal<Tema>(temaInicial());

  constructor() {
    this.api.recarregar().catch((erro) => this.aviso.set(mensagemDeErro(erro)));

    effect(() => {
      document.documentElement.dataset['tema'] = this.tema();
    });
  }

  protected alternarTema(): void {
    const novo: Tema = this.tema() === 'claro' ? 'escuro' : 'claro';
    this.tema.set(novo);
    try {
      localStorage.setItem(CHAVE_TEMA, novo);
    } catch {
    }
  }
}

function temaInicial(): Tema {
  try {
    const salvo = localStorage.getItem(CHAVE_TEMA);
    if (salvo === 'claro' || salvo === 'escuro') return salvo;
  } catch {
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro';
}
