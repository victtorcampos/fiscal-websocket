package tech.vcinf.fiscalwebsocket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tech.vcinf.fiscalwebsocket.model.Emitente;
import tech.vcinf.fiscalwebsocket.model.TransactionLog;
import tech.vcinf.fiscalwebsocket.repository.EmitenteRepository;
import tech.vcinf.fiscalwebsocket.repository.TransactionLogRepository;
import tech.vcinf.fiscalwebsocket.service.SefazService;
import tech.vcinf.fiscalwebsocket.service.UfWebService;
import tech.vcinf.fiscalwebsocket.service.XmlSignatureService;

import java.io.File;
import java.io.FileWriter;
import java.net.http.HttpResponse;
import java.util.Map;

@Component
public class FiscalWebSocketHandler extends TextWebSocketHandler {

    private final EmitenteRepository emitenteRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UfWebService ufWebService;
    private final XmlSignatureService xmlSignatureService;
    private final SefazService sefazService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FiscalWebSocketHandler(EmitenteRepository emitenteRepository,
                                  TransactionLogRepository transactionLogRepository,
                                  UfWebService ufWebService,
                                  XmlSignatureService xmlSignatureService,
                                  SefazService sefazService) {
        this.emitenteRepository = emitenteRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.ufWebService = ufWebService;
        this.xmlSignatureService = xmlSignatureService;
        this.sefazService = sefazService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> payload = objectMapper.readValue(message.getPayload(), Map.class);

        if ("register".equals(payload.get("action"))) {
            Emitente emitente = objectMapper.convertValue(payload.get("data"), Emitente.class);
            emitenteRepository.save(emitente);
            session.sendMessage(new TextMessage("Emitente registered successfully"));
            return;
        }

        String cnpj = payload.get("cnpj");
        String xml = payload.get("xml");
        String servico = payload.get("servico");

        Emitente emitente = emitenteRepository.findById(cnpj)
                .orElseThrow(() -> new RuntimeException("Emitente not found"));

        String url = ufWebService.getUrl(emitente.getUf(), servico);

        File xmlFile = new File("temp.xml");
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }

        xmlSignatureService.sign(xmlFile.getAbsolutePath(), emitente.getCaminhoCertificado(), emitente.getSenha());

        HttpResponse<String> response = sefazService.send(url, new String(java.nio.file.Files.readAllBytes(xmlFile.toPath())), emitente.getCaminhoCertificado(), emitente.getSenha());

        TransactionLog log = new TransactionLog();
        log.setCnpj(cnpj);
        log.setXmlEnviado(xml);
        log.setXmlResposta(response.body());
        log.setStatusHttp(response.statusCode());
        transactionLogRepository.save(log);

        session.sendMessage(new TextMessage(response.body()));
    }
}
