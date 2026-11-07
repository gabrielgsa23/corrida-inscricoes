# Inscrições · 5ª Corrida Solidária da Vila Esperança

API REST para cadastrar, listar, consultar, editar e cancelar inscrições, com resumo por percurso.

```
backend/    API em Java 21, Spring Boot 3 e H2 em memória
frontend/   Tela em Angular que consome a API (opcional)
```

## Como executar

Requisito: JDK 21. O Maven Wrapper baixa o Maven sozinho.

```bash
cd backend
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
./mvnw test                 # testes automatizados
```

A API sobe em `http://localhost:8080` com a lista vazia. Os dados ficam em memória e são apagados ao parar a aplicação.

Front-end (opcional, requer Node 22 e o back-end rodando):

```bash
cd frontend
npm install
npm start                   # http://localhost:4200
```

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

## Estrutura

`backend/src/main/java/br/com/corrida/`

| Pasta | Papel |
|---|---|
| `controller` | Rotas HTTP, sem regra de negócio |
| `service` | Casos de uso: cadastrar, listar, consultar, editar, cancelar e resumir |
| `validator` | Normalização, validação e regulamento, compartilhados por cadastro e edição |
| `repository` | Acesso ao banco (Spring Data JPA) |
| `model` | Entidade `Inscricao` |
| `dto` | JSON de entrada e saída |
| `enums` | `Percurso` e `Categoria` |
| `exception` | Uma exceção por tipo de recusa e o `GlobalExceptionHandler` |

A data da prova fica só em `application.properties` (`corrida.data-prova`). Idade e categoria não são gravadas: são calculadas a cada resposta.

## Decisões e premissas

- Data da prova: 14/03/2027, conforme o regulamento (o áudio dizia 13/03).
- Idade completa na data da prova; idade mínima conferida antes da restrição dos 10 km.
- Categorias: Juvenil 14–17, Adulto 18–39, Master 40–59, Sênior 60+.
- E-mail é a chave: espaços nas pontas e maiúsculas são ignorados.
- No `PUT`, o `email` do corpo é ignorado; vale o da URL. O 404 é verificado antes da validação.
- Nome: 3 a 100 caracteres, com letras acentuadas, apóstrofo e hífen.
- Nascimento: data existente anterior a hoje (fuso de Brasília), a partir de 1900-01-01.
- Percurso: exatamente `KM_5` ou `KM_10`.
- Campos a mais no JSON são ignorados; tipos errados devolvem 400, nunca 500.
- Fora de escopo: Pix, e-mail de confirmação, login e autorização de responsáveis.
