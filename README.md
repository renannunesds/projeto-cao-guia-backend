# ProjetoCaoGuiaBackend

Backend da aplicacao Cao Guia, construido com Spring Boot, JPA, seguranca JWT e SQL Server.

## Tecnologias

- Java 21
- Spring Boot 4.0.6
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- JWT (jjwt)
- SQL Server (mssql-jdbc)
- Docker Compose
- Jsoup (varredura unificada de noticias do portal do IF e postagens do Instagram)
- Integracao com API Gemini (audiodescricao automatica de imagens)
- OpenAPI/Swagger (springdoc)

## Estrutura principal

- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/core`: configuracoes globais, excecoes e bootstrap (`DataLoader`)
- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/core/security`: autenticacao/autorizacao JWT
- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/noticias`: modulo unificado de noticias, integrando a varredura automatizada do portal do IF Goiano e do Instagram (`SincronizacaoNoticiasService`) e a audiodescricao por IA (`AudiodescricaoIAService`)
- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/postagemInstagram`: servicos e endpoints auxiliares de coleta do Instagram (`InstagramScraperService`)
- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/treinamento`: modulo de treinamento
- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/depoimento`: modulo de depoimentos
- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/estatisticas`: modulo de estatisticas
- `src/main/java/com/ifgoiano/urt/projetocaoguia/projetocaoguiabackend/usuarios`: modulo de usuarios
- `database/database.sql`: script SQL de referencia do banco
- `collections/postman/CaoGuia.postman_collection.json`: colecao de endpoints para testes no Postman

## Pre-requisitos

- Java 21 instalado e no `PATH`
- Docker Desktop ativo (para SQL Server via Compose)
- Maven Wrapper do projeto (`mvnw.cmd`)

## Como rodar em desenvolvimento

1. Suba o banco:

```powershell
docker compose up -d
```

2. Rode a aplicacao:

```powershell
.\mvnw.cmd spring-boot:run
```

## Como gerar e rodar JAR

1. Gere o pacote:

```powershell
.\mvnw.cmd -DskipTests package
```

2. Rode o JAR:

```powershell
java -jar .\target\ProjetoCaoGuiaBackend-0.0.1-SNAPSHOT.jar
```

## Configuracao

As configuracoes principais ficam em `src/main/resources/application.properties`.

Pontos importantes:

- Datasource SQL Server (`spring.datasource.*`)
- JPA/Hibernate (`spring.jpa.*`)
- JWT (`jwt.secret`, `jwt.expiration`)
- Integracao com API Gemini (chave configurada para geracao de audiodescricao das imagens)
- Porta HTTP (`server.port`)
- Integracao com Docker Compose do Spring (`spring.docker.compose.*`)

## API docs e testes

Com a aplicacao em execucao, acesse o Swagger UI em:

- `http://localhost:8080/swagger-ui/index.html`

Para testar via Postman, importe a colecao disponivel em:

- `collections/postman/CaoGuia.postman_collection.json`

## Banco inicial

No primeiro bootstrap com tabela de usuarios vazia, o sistema cria apenas o usuario admin padrao.

Credenciais padrao:

- email: `admin@caoguia.com`
- senha: `admin123`

## Historico de mudancas

Consulte `CHANGELOG.md` para o historico versionado (SemVer) e dump de commits.
