package tech.vcinf.fiscalwebsocket.service;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.springframework.stereotype.Service;
import tech.vcinf.fiscalwebsocket.model.Emitente;
import tech.vcinf.fiscalwebsocket.util.SoapEnvelopeUtils;

import java.io.IOException;
import java.net.URI;

@Service
public class SefazService {

    private final SefazProtocolFactory protocolFactory;

    public SefazService(SefazProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public String send(String url, String xml, Emitente emitente, String servico) throws Exception {
        PostMethod postMethod = null;
        try {
            // 1. Obter o Protocol customizado
            Protocol protocol = protocolFactory.getProtocol(emitente);

            // 2. Criar envelope SOAP
            String soapXml = SoapEnvelopeUtils.createEnvelope(xml, servico, emitente.getUf());

            // 3. Extrair host e path da URL
            URI uri = URI.create(url);
            String host = uri.getHost();
            String path = uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

            // 4. Configurar HttpClient
            HttpClient httpClient = new HttpClient();
            httpClient.getHostConfiguration().setHost(host, 443, protocol);

            // 5. Criar método POST
            postMethod = new PostMethod(path);
            postMethod.setRequestHeader("Content-Type", "application/soap+xml; charset=utf-8");
            postMethod.setRequestBody(soapXml);

            // 6. Executar requisição
            // O HttpClient gerencia códigos de status HTTP, lançando exceções para erros de servidor (5xx) ou cliente (4xx)
            // se a propriedade http.protocol.strict-rect-auth estiver ativada (padrão).
            // Portanto, um código de sucesso (2xx) simplesmente continua.
            int statusCode = httpClient.executeMethod(postMethod);

            String responseBody = postMethod.getResponseBodyAsString();
            
            System.out.println("Status da Resposta: " + statusCode);

            // 7. Retornar a resposta
            return responseBody;

        } catch (IOException e) {
            // Log de erro de rede
            System.err.println("Erro de I/O ao comunicar com a SEFAZ: " + e.getMessage());
            throw new Exception("Erro de rede ao conectar com a SEFAZ.", e);
        } finally {
            // 8. Liberar a conexão para que possa ser reutilizada
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
    }
}
