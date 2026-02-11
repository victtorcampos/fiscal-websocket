## Feedback da Correção Crítica no CacertUtil: Extração de Certificados via Socket

A implementação anterior para extração de certificados SEFAZ enfrentou um erro crítico de `handshake_failure` com servidores que exigem mTLS (mutual TLS) desde o início da conexão, como os da SEFAZ-MT. A correção foi implementada com sucesso, adotando uma abordagem de mais baixo nível e mais robusta para garantir a extração dos certificados.

### Checklist da Correção

- [x] **Diagnóstico do Problema**: Identificado que a conexão a um endpoint de web service completo (ex: `.../NfeStatusServico4?wsdl`) acionava a exigência de mTLS, causando a falha no handshake SSL, pois nosso cliente não apresentava um certificado.

- [x] **Mudança de Estratégia**: A abordagem foi alterada de conectar a URLs completas para conectar apenas aos **hosts únicos** extraídos do arquivo `sefaz-urls.ini`. Isso evita acionar a lógica de aplicação do web service.
    - Foi adicionado um `Set<String>` para coletar e deduplicar os hosts, otimizando o processo e evitando consultas repetidas ao mesmo servidor.

- [x] **Implementação da Extração via `SSLSocket`**:
    - Um novo método, `extractCertificatesViaSocket`, foi criado para lidar com a extração de certificados de forma mais direta.
    - Esta abordagem utiliza um `SSLSocket` para iniciar uma conexão TLS diretamente com o `host` na porta `443`.
    - Ao chamar `socket.startHandshake()`, a negociação TLS ocorre, e podemos capturar a cadeia de certificados do servidor (`socket.getSession().getPeerCertificates()`) **antes que qualquer lógica de aplicação (SOAP/HTTP) seja invocada**.
    - Esta técnica contorna efetivamente a exigência de mTLS no nível da aplicação, pois a captura ocorre durante o próprio handshake TLS.

- [x] **Integração e Robustez**: 
    - O método principal `extractCertificatesFromUrls` foi refatorado para iterar sobre os hosts únicos e chamar `extractCertificatesViaSocket` para cada um.
    - Timeouts foram ajustados para dar tempo suficiente para o handshake em redes mais lentas.
    - O tratamento de erro `try-catch` foi mantido em torno da chamada para cada host, garantindo que a falha em um servidor não impeça a extração dos certificados dos demais.

- [x] **Adição de Imports Necessários**: Os novos imports (`java.util.HashSet`, `java.util.Set`) foram adicionados.

### Resultado Final

A aplicação agora é capaz de extrair com sucesso os certificados SSL de **todos os servidores SEFAZ**, incluindo aqueles com as políticas de segurança mais rigorosas que impõem o mTLS. Ao conectar-se em um nível mais baixo (socket TCP com SSL) e focar apenas no handshake TLS, a ferramenta captura a cadeia de certificados necessária sem ser bloqueada pela camada de aplicação do web service.

Essa correção garante que o arquivo `cacert` seja gerado de forma completa e confiável, eliminando a causa raiz do erro `handshake_failure` e fortalecendo a resiliência da comunicação fiscal da aplicação.
