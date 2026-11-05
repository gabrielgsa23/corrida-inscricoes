# Inscrições · 5ª Corrida Solidária da Vila Esperança

API REST para cadastrar, listar, consultar, editar e cancelar inscrições da corrida, com resumo por percurso.

```
backend/    API em Java 21 + Spring Boot 3 + H2 (é o que o teste back-end pede)
frontend/   Tela em Angular 21 que consome a API (opcional, não é preciso para avaliar a API)
```

## Como executar

Requisitos: **JDK 21** e **Git**. O Maven não precisa ser instalado: o `mvnw` baixa a versão certa sozinho.

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`, com a lista vazia. Os dados ficam em um banco H2 em memória e são apagados ao parar a aplicação.

Testes automatizados (regras nas bordas e fluxo completo da API):

```bash
./mvnw test
```

### Front-end (opcional)

Requisito: **Node 22** (ou 20.19+/24) e npm. Com o back-end rodando, em outro terminal:

```bash
cd frontend
npm install
npm start                     # http://localhost:4200
```

O `ng serve` repassa tudo que começa com `/api` para a porta 8080 (`proxy.conf.json`), por isso o back-end não precisa de CORS.

## Contrato

| Operação  | Rota                             | Sucesso | Recusas  |
|-----------|----------------------------------|---------|----------|
| Cadastrar | `POST /api/inscricoes`           | 201     | 400, 409 |
| Listar    | `GET /api/inscricoes`            | 200     | —        |
| Consultar | `GET /api/inscricoes/{email}`    | 200     | 404      |
| Editar    | `PUT /api/inscricoes/{email}`    | 200     | 400, 404 |
| Cancelar  | `DELETE /api/inscricoes/{email}` | 204     | 404      |
| Resumo    | `GET /api/inscricoes/resumo`     | 200     | —        |

Toda recusa devolve `{ "mensagem": "..." }`, inclusive JSON quebrado e rota inexistente.

```bash
curl -i -X POST localhost:8080/api/inscricoes -H "Content-Type: application/json" \
  -d '{"nome":"Ana Souza","email":"ana.souza@email.com","dataNascimento":"1990-05-20","percurso":"KM_10"}'
```

## Onde está cada coisa

Tudo em `backend/src/main/java/br/com/corrida/`:

| Arquivo | Papel |
|---|---|
| `InscricaoController` | Rotas HTTP do contrato. Sem regra de negócio |
| `InscricaoService` | Casos de uso: cadastrar, listar, consultar, editar, cancelar, resumo |
| `RegrasDaInscricao` | Normalização, validação e regulamento (Java puro, usado no cadastro e na edição) |
| `Inscricao` / `InscricaoRepository` | Tabela no H2 e acesso ao banco |
| `Contrato` | JSON de entrada e saída |
| `InscricaoRecusada` / `TratamentoDeErros` | Recusas 400/404/409 e conversão de qualquer erro para `{ "mensagem" }` |

A data da prova fica só em `backend/src/main/resources/application.properties` (`corrida.data-prova`).

## Regras e fontes

| Regra | Fonte |
|---|---|
| Quatro campos: nome, e-mail, nascimento, percurso | Regulamento, Art. 4º, I |
| Um e-mail, uma inscrição ativa | Art. 4º, II |
| Prova em 14/03/2027 | Art. 5º, I |
| Idade completa na data da prova | Art. 6º, I |
| Mínimo de 14 anos, verificado antes do percurso | Art. 6º, II e enunciado |
| 10 km só a partir de 18 anos | Art. 6º, III |
| Juvenil 14–17, Adulto 18–39, Master 40–59, Sênior 60+ | Art. 6º, IV |
| Cancelar libera o e-mail para recadastro | Art. 7º |
| Normalização de nome e e-mail, formatos e códigos de status | Enunciado |

Idade e categoria não são gravadas: são calculadas a cada resposta a partir do nascimento. O resumo é contado a partir da lista, então nunca fica fora de sincronia.

## Conflitos e decisões

| Conflito | Decisão | Motivo |
|---|---|---|
| Áudio diz "13 de março"; regulamento diz 14/03/2027 | 14/03/2027 | O regulamento prevalece. 13/03/2027 cai num sábado, e o próprio áudio fala em domingo |
| Valdir sugere categoria pelo ano de nascimento | Idade completa na data da prova | Art. 6º, I. Quem nasceu em 15/03/1987 tem 39 anos na prova e é Adulto |
| Dona Marta quer inscrever ela e os dois filhos no mesmo e-mail | Não é possível | Art. 4º, I e II. A Júlia (12) seria recusada pela idade. O Pedro (15) precisa de e-mail próprio e só pode correr 5 km |
| Lucas: `joao.silva@gmail.com` e `joaosilva@gmail.com` chegam na mesma caixa | São e-mails diferentes | O enunciado define a normalização (espaços e minúsculas). A regra do ponto é só do Gmail |
| Seu Jorge pede gerar Pix | Não implementado | Art. 4º, III: inscrição gratuita, com doação de 1 kg de alimento |

Premissas adotadas:

- No `PUT`, um `email` enviado no corpo é ignorado; vale o da URL.
- O nome aceita letras (com acento), espaços, apóstrofo (`'` ou `’`) e hífen entre as palavras.
- Nascimento a partir de 1900-01-01, para barrar datas absurdas como `0001-01-01`.
- "Hoje" é o dia em Brasília (`corrida.fuso`), para o servidor em UTC não adiantar a data à noite.
- Quem nasceu em 29/02 completa idade em 01/03 nos anos não bissextos.
- Na edição, o 404 é verificado antes da validação dos dados.
- Entradas atípicas: o espaço "não separável" (U+00A0) de textos copiados da web e o tab ou quebra de linha no nome são tratados como espaço comum; e-mail em minúsculas independe do idioma do computador.
- E-mail: sem ponto no início, no fim ou repetido antes do `@`, no máximo 64 caracteres antes do `@` e 254 no total.
- Percurso exige exatamente `KM_5` ou `KM_10` (o contrato fixa os valores; `km_5` é recusado com mensagem clara).
- Campos a mais no JSON (como `categoria` ou `idadeNaProva`) são ignorados; número ou lista no lugar de texto devolve 400, nunca 500.

## Fora de escopo

Pix, e-mail de confirmação, login, autorização de responsáveis para menores e prazo de inscrição: não são pedidos pelo enunciado.

## Perguntas ao cliente

1. "Nome completo" precisa ter pelo menos nome e sobrenome?
2. E-mails do Gmail com e sem ponto devem contar como a mesma pessoa?
3. Menores de 18 precisam de autorização dos responsáveis na retirada do kit?
4. Até quando as inscrições e os cancelamentos ficam abertos?
5. Existe limite de vagas por percurso?
