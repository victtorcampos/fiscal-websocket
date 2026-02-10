package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;
import tech.vcinf.fiscalwebsocket.model.Emitente;
import tech.vcinf.fiscalwebsocket.util.SoapEnvelopeUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SefazService {

    private final SefazHttpClientFactory httpClientFactory;

    public SefazService(SefazHttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public HttpResponse<String> send(String url, String xml, Emitente emitente, String servico) throws Exception {
        HttpClient client = httpClientFactory.getHttpClient(emitente);

        String soapXml = SoapEnvelopeUtils.createEnvelope(xml, servico, emitente.getUf());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/soap+xml")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
