import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Api, mensagemDeErro, ROTULO_PERCURSO } from './api';

@Component({
  selector: 'app-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    @if (naoEncontrada()) {
      <section class="painel vazio">
        <h2>Inscrição não encontrada.</h2>
        <a routerLink="/" class="botao">Voltar para a lista</a>
      </section>
    } @else {
      <section class="painel">
        <h2>{{ email() ? 'Editar inscrição' : 'Nova inscrição' }}</h2>
        <p class="dica">Todos os campos são obrigatórios.</p>

        @if (erro(); as erro) {
          <p class="erro-geral" role="alert">{{ erro }}</p>
        }

        <form [formGroup]="form" (ngSubmit)="salvar()" novalidate>
          <div class="campo">
            <label for="nome">Nome completo</label>
            <input id="nome" formControlName="nome" autocomplete="name" />
          </div>

          <div class="campo">
            <label for="email">E-mail</label>
            <input id="email" type="email" formControlName="email" autocomplete="email" />
            @if (email()) {
              <small class="dica">O e-mail identifica a inscrição e não pode ser alterado.</small>
            }
          </div>

          <div class="campo">
            <label for="dataNascimento">Data de nascimento</label>
            <input id="dataNascimento" type="date" formControlName="dataNascimento" />
          </div>

          <div class="campo">
            <label for="percurso">Percurso</label>
            <select id="percurso" formControlName="percurso">
              <option value="" disabled>Selecione o percurso</option>
              <option value="KM_5">{{ rotulos.KM_5 }}</option>
              <option value="KM_10">{{ rotulos.KM_10 }}</option>
            </select>
          </div>

          <div class="form-acoes">
            <a routerLink="/" class="botao secundario">Cancelar</a>
            <button type="submit" class="botao" [disabled]="enviando()">
              {{ email() ? 'Salvar alterações' : 'Confirmar inscrição' }}
            </button>
          </div>
        </form>
      </section>
    }
  `,
})
export class Formulario implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  readonly email = input<string>();

  protected readonly erro = signal<string | null>(null);
  protected readonly naoEncontrada = signal(false);
  protected readonly enviando = signal(false);
  protected readonly rotulos = ROTULO_PERCURSO;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: '',
    email: '',
    dataNascimento: '',
    percurso: '',
  });

  async ngOnInit(): Promise<void> {
    const email = this.email();
    if (!email) return;

    try {
      const atual = await this.api.consultar(email);
      this.form.setValue({
        nome: atual.nome,
        email: atual.email,
        dataNascimento: atual.dataNascimento,
        percurso: atual.percurso,
      });
      this.form.controls.email.disable();
    } catch (e) {
      if (e instanceof HttpErrorResponse && e.status === 404) {
        this.naoEncontrada.set(true);
      } else {
        this.erro.set(mensagemDeErro(e));
      }
    }
  }

  protected async salvar(): Promise<void> {
    const dados = this.form.getRawValue();
    const email = this.email();

    this.erro.set(null);
    this.enviando.set(true);
    try {
      if (email) {
        await this.api.editar(email, dados);
      } else {
        await this.api.cadastrar(dados);
      }
      this.router.navigate(['/']);
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    } finally {
      this.enviando.set(false);
    }
  }
}
