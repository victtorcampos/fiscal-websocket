# Middleware Fiscal Java Spring Boot (WebSocket to SEFAZ mTLS) - Development Steps

## Checklist

### Fase 1: Estrutura do Projeto e Dependências

- [ ] Criar a estrutura de pacotes `tech.vcinf.fiscalwebsocket`.
- [ ] Adicionar dependências no `pom.xml`:
    - `spring-boot-starter-websocket`
    - `spring-boot-starter-data-jpa`
    - `com.h2database:h2`
- [ ] Configurar o H2 Database no `application.properties`.
- [ ] Criar a classe principal da aplicação.

### Fase 2: Persistência e Configuração

- [ ] Criar a entidade `Emitente`.
- [ ] Criar o repositório `EmitenteRepository`.
- [ ] Criar a entidade `TransactionLog`.
- [ ] Criar o repositório `TransactionLogRepository`.
- [ ] Criar o serviço de mapeamento de UF (`UfWebService`) para ler as URLs do arquivo `.ini`.
- [ ] Criar o arquivo `sefaz-urls.ini` em `src/main/resources`.

### Fase 3: Segurança e Comunicação

- [ ] Implementar `XmlSignatureService` para assinatura de XML (JSR 105).
- [ ] Implementar `SefazHttpClientFactory` para criar `HttpClient` com mTLS.
- [ ] Implementar `CertificateSessionManager` para gerenciar os `KeyStore` em memória.

### Fase 4: Lógica de Negócio e Endpoints

- [ ] Criar DTOs para a comunicação WebSocket (`FiscalRequest`, `FiscalResponse`).
- [ ] Criar a classe utilitária para o envelope SOAP (`SoapEnvelopeUtils`).
- [ ] Criar `SefazService` para orquestrar o fluxo:
    - Receber dados.
    - Obter certificado.
    - Assinar XML.
    - Montar envelope SOAP.
    - Enviar para a SEFAZ.
    - Persistir log.
- [ ] Implementar o WebSocket Endpoint (`FiscalController`) com `@MessageMapping("/transmitir")`.
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
