import { provideHttpClient } from '@angular/common/http';
import { provideBrowserGlobalErrorListeners } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, Routes, withComponentInputBinding } from '@angular/router';
import { App } from './app/app';
import { Formulario } from './app/formulario';
import { Lista } from './app/lista';

// Rotas do front. São só três telas:
//   /                 -> lista de inscrições
//   /nova             -> formulário de cadastro
//   /editar/:email    -> o mesmo formulário, em modo edição
// Qualquer outro endereço volta para a lista.
const rotas: Routes = [
  { path: '', component: Lista, title: 'Inscrições' },
  { path: 'nova', component: Formulario, title: 'Nova inscrição' },
  { path: 'editar/:email', component: Formulario, title: 'Editar inscrição' },
  { path: '**', redirectTo: '' },
];

// Ponto de entrada: monta o componente App e registra os serviços globais.
bootstrapApplication(App, {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: o :email da URL chega no componente como input().
    provideRouter(rotas, withComponentInputBinding()),
    // HttpClient, usado em api.ts para falar com o back-end.
    provideHttpClient(),
  ],
}).catch((erro) => console.error(erro));
