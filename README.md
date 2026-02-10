# Middleware Fiscal WebSocket para SEFAZ

Este projeto é um middleware de código aberto construído em Java com Spring Boot. Ele atua como uma ponte de comunicação segura entre uma aplicação cliente e os web services da Secretaria da Fazenda (SEFAZ) do Brasil.

O principal objetivo é simplificar o processo de emissão de documentos fiscais eletrônicos (DF-e), abstraindo as complexidades da comunicação mTLS (Mutual TLS), assinatura de XML e gerenciamento de certificados digitais. A comunicação com a aplicação cliente é feita de forma moderna e em tempo real através de WebSockets com STOMP.

## Principais Funcionalidades

- **Comunicação Real-time:** Utiliza WebSockets e STOMP para uma comunicação eficiente e em tempo real.
- **Segurança mTLS:** Gerencia a comunicação segura com os servidores da SEFAZ usando autenticação mútua (mTLS).
- **Assinatura de XML:** Assina digitalmente os documentos XML em conformidade com os padrões da SEFAZ (JSR 105).
- **Gerenciamento de Certificados:** Armazena e gerencia com segurança as informações dos certificados digitais (A1) dos emitentes.
- **Mapeamento de Endpoints:** Localiza dinamicamente as URLs dos web services da SEFAZ para cada Unidade Federativa (UF) a partir de um arquivo de configuração.
- **Arquitetura Orientada a Serviços:** Código modular e desacoplado, facilitando a manutenção e a adição de novas funcionalidades.
- **Banco de Dados:** Utiliza H2 em memória para persistência de logs e cadastro de emitentes, facilmente substituível por outro banco de dados.

## Como Iniciar

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6 ou superior

### Executando a Aplicação (Linux/macOS)
O servidor deve iniciar automaticamente ao abrir o ambiente de desenvolvimento. Para executar manualmente, utilize o terminal:

```sh
mvn spring-boot:run
```

### Executando a Aplicação (Windows com PowerShell 7+)

1.  **Verifique as dependências:**
    Abra o PowerShell e verifique se o Java e o Maven estão instalados e configurados no seu `PATH`.

    ```powershell
    java -version
    mvn -v
    ```
    *Se os comandos não forem encontrados, instale o [Java (OpenJDK)](https://adoptium.net/) e o [Maven](https://maven.apache.org/download.cgi) antes de continuar.*

2.  **Execute a aplicação:**
    No diretório raiz do projeto, execute o seguinte comando:

    ```powershell
    mvn spring-boot:run
    ```

O servidor estará em execução e pronto para aceitar conexões WebSocket na porta `8080`.

## Testando no Navegador

Você pode testar a comunicação WebSocket diretamente do console do seu navegador.

1.  Abra uma página web qualquer (pode ser `about:blank`).
2.  Abra as ferramentas de desenvolvedor (F12) e vá para a aba "Console".
3.  **Carregue as bibliotecas SockJS e STOMP:**
    Cole e execute o seguinte código para carregar os scripts necessários.

    ```javascript
    (function() {
        const sockjs = document.createElement('script');
        sockjs.src = 'https://cdn.jsdelivr.net/npm/sockjs-client@1.5.1/dist/sockjs.min.js';
        document.head.appendChild(sockjs);

        const stomp = document.createElement('script');
        stomp.src = 'https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js';
        document.head.appendChild(stomp);
    })();
    ```

4.  **Conecte e envie mensagens:**
    Após alguns segundos, as bibliotecas estarão carregadas. Use o código abaixo para conectar, se inscrever em um tópico de resposta e enviar mensagens.

    ```javascript
    const socket = new SockJS('/ws'); // Endpoint do WebSocket
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Conectado: ' + frame);

        // Se inscreve no tópico para receber as respostas do servidor
        stompClient.subscribe('/topic/responses', function (response) {
            console.log('Resposta recebida: ', JSON.parse(response.body));
        });

        // Exemplo 1: Cadastrar um novo emitente
        // ATENÇÃO: Substitua os valores de 'caminhoCertificado' e 'senha'
        const registerPayload = {
            action: 'register',
            data: {
                cnpj: '12345678000199', // CNPJ do emitente
                razaoSocial: 'Empresa Exemplo LTDA',
                uf: 'SP', // UF do emitente
                caminhoCertificado: 'C:/path/to/your/certificate.pfx', // Caminho LOCAL no servidor para o certificado
                senha: 'your_cert_password' // Senha do certificado
            }
        };
        stompClient.send("/app/transmitir", {}, JSON.stringify(registerPayload));
        console.log("Enviado pedido de cadastro de emitente.");


        // Exemplo 2: Enviar um XML para autorização (após cadastrar o emitente)
        // XML de exemplo para consulta de status do serviço
        const xmlContent = '<consStatServ xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00"><tpAmb>2</tpAmb><cUF>35</cUF><xServ>STATUS</xServ></consStatServ>';

        const transmitPayload = {
            action: 'transmit',
            cnpj: '12345678000199', // CNPJ do emitente já cadastrado
            servico: 'NFeStatusServico4', // Serviço da SEFAZ a ser consumido
            xml: xmlContent
        };
        setTimeout(() => {
             stompClient.send("/app/transmitir", {}, JSON.stringify(transmitPayload));
             console.log("Enviado pedido de transmissão de XML.");
        }, 2000); // Pequeno delay para enviar após o registro

    }, function(error) {
        console.error('Erro de conexão: ' + error);
    });
    ```
    *Observe os logs no console para ver a resposta do servidor.*
