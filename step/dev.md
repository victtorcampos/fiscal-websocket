# Middleware Fiscal Java Spring Boot (WebSocket to SEFAZ mTLS) - Development Steps

## Checklist

### Fase 1: Estrutura do Projeto e Dependências

- [x] Criar a estrutura de pacotes `tech.vcinf.fiscalwebsocket`.
- [x] Adicionar dependências no `pom.xml`:
    - `spring-boot-starter-websocket`
    - `spring-boot-starter-data-jpa`
    - `com.h2database:h2`
- [x] Configurar o H2 Database no `application.properties`.
- [x] Criar a classe principal da aplicação.

### Fase 2: Persistência e Configuração

- [x] Criar a entidade `Emitente`.
- [x] Criar o repositório `EmitenteRepository`.
- [x] Criar a entidade `TransactionLog`.
- [x] Criar o repositório `TransactionLogRepository`.
- [x] Criar o serviço de mapeamento de UF (`UfWebService`) para ler as URLs do arquivo `.ini`.
- [x] Criar o arquivo `sefaz-urls.ini` em `src/main/resources`.

### Fase 3: Segurança e Comunicação

- [x] Implementar `XmlSignatureService` para assinatura de XML (JSR 105).
- [ ] Implementar `SefazHttpClientFactory` para criar `HttpClient` com mTLS.
- [ ] Implementar `CertificateSessionManager` para gerenciar os `KeyStore` em memória.

### Fase 4: Lógica de Negócio e Endpoints

- [x] Criar DTOs para a comunicação WebSocket (`FiscalRequest`, `FiscalResponse`).
- [x] Criar a classe utilitária para o envelope SOAP (`SoapEnvelopeUtils`).
- [x] Criar `SefazService` para orquestrar o fluxo:
    - Receber dados.
    - Obter certificado.
    - Assinar XML.
    - Montar envelope SOAP.
    - Enviar para a SEFAZ.
    - Persistir log.
- [x] Implementar o WebSocket Endpoint (`FiscalController`) com `@MessageMapping("/transmitir")`.
- [ ] Implementar os métodos de serviço da SEFAZ (`statusServico`, `autorizacaoLote`, etc.).

### Fase 5: Testes e Validação

- [ ] Criar testes unitários para o `XmlSignatureService`.
- [ ] Criar testes unitários para o `UfWebService`.
- [ ] Criar testes de integração para o fluxo completo com um mock da SEFAZ.
- [ ] Validar o fluxo com um certificado de teste.

## Comentários Pós-Implementação

- **[Data]**: Comentários sobre a implementação da Fase 1.
- **[Data]**: Comentários sobre a implementação da Fase 2.
- **[Data]**: Comentários sobre a implementação da Fase 3.
- **[Data]**: Comentários sobre a implementação da Fase 4.
- **[Data]**: Comentários sobre a implementação da Fase 5.
