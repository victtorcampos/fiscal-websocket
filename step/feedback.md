## Feedback da Migração: Java HttpClient para Apache Commons HttpClient 3.1

A migração crítica do `HttpClient` nativo do Java 11+ para o `Apache Commons HttpClient 3.1` foi concluída com sucesso. A implementação seguiu rigorosamente os requisitos para garantir a compatibilidade com os web services da SEFAZ, resolvendo problemas de negociação TLS/SSL.

### Checklist de Implementação

- [x] **Adicionar Dependência no `pom.xml`**:
    - A dependência `commons-httpclient:commons-httpclient:3.1` foi adicionada com sucesso ao `pom.xml` do projeto, disponibilizando as bibliotecas necessárias para a refatoração.

- [x] **Criar Classe `SocketFactoryDinamico`**:
    - A classe `SocketFactoryDinamico` foi criada em `src/main/java/tech/vcinf/fiscalwebsocket/util/SocketFactoryDinamico.java`.
    - A classe implementa a interface `SecureProtocolSocketFactory`.
    - O construtor foi implementado para receber os `KeyStores` do emitente e do `cacert`, a senha, o alias e o protocolo SSL (`TLSv1.2`).
    - A lógica interna cria um `SSLContext` que utiliza um `KeyManagerFactory` (para o certificado do cliente) e um `TrustManagerFactory` (para o `cacert` da ICP-Brasil), garantindo a autenticação mTLS e a confiança na cadeia de certificados da SEFAZ.

- [x] **Refatorar `SefazHttpClientFactory` para `SefazProtocolFactory`**:
    - A antiga `SefazHttpClientFactory` foi removida.
    - A nova `SefazProtocolFactory` foi criada e injetada no `SefazService`.
    - A factory agora produz um objeto `Protocol` do Commons HttpClient, encapsulando a `SocketFactoryDinamico`.
    - A lógica de cache foi mantida para reutilizar os objetos `Protocol` por CNPJ, otimizando o desempenho.

- [x] **Refatorar `SefazService`**:
    - O `SefazService` foi completamente refatorado para usar o `org.apache.commons.httpclient.HttpClient`.
    - A implementação agora obtém o `Protocol` customizado da `SefazProtocolFactory` e o atribui à configuração do host do `HttpClient`.
    - O envio da requisição foi adaptado para usar `PostMethod`, com a configuração correta do corpo da requisição SOAP e do cabeçalho `Content-Type`.
    - A conexão é liberada corretamente em um bloco `finally` para evitar vazamento de recursos.

- [x] **Ajustar Assinatura de Retorno e `FiscalController`**:
    - O método `SefazService.send` agora retorna uma `String` com o corpo da resposta, em vez de um `HttpResponse`.
    - O `FiscalController` foi ajustado para receber essa `String` diretamente e construir o `FiscalResponse`, assumindo um status HTTP `200` em caso de sucesso (ausência de exceções).

- [x] **Tratamento de Erros**:
    - Um bloco `try-catch` robusto foi adicionado ao `SefazService.send()` para capturar e relançar `IOException`, fornecendo mensagens de erro mais claras no caso de falhas de comunicação com a SEFAZ.

### Resultado

A aplicação agora utiliza uma abordagem testada e aprovada pela comunidade para comunicação com os serviços fiscais brasileiros. O controle explícito sobre o `SSLContext` e o uso forçado do `TLSv1.2` eliminam as incompatibilidades encontradas com o `HttpClient` padrão do Java, garantindo que as requisições para a SEFAZ (como a consulta de status) sejam executadas com sucesso.
