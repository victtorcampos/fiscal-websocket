package tech.vcinf.fiscalwebsocket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import tech.vcinf.fiscalwebsocket.dto.FiscalRequest;
import tech.vcinf.fiscalwebsocket.dto.FiscalResponse;
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
import java.nio.file.Files;

@Controller
public class FiscalController {

    private final EmitenteRepository emitenteRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UfWebService ufWebService;
    private final XmlSignatureService xmlSignatureService;
    private final SefazService sefazService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FiscalController(EmitenteRepository emitenteRepository,
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

    @MessageMapping("/transmitir")
    @SendTo("/topic/responses")
    public FiscalResponse transmitir(FiscalRequest request) {
        try {
            if ("register".equals(request.getAction())) {
                Emitente emitente = objectMapper.convertValue(request.getData(), Emitente.class);
                emitenteRepository.save(emitente);
                return new FiscalResponse(200, null, "Emitente registered successfully");
            }

            String cnpj = request.getCnpj();
            String xml = request.getXml();
            String servico = request.getServico();

            Emitente emitente = emitenteRepository.findById(cnpj)
                    .orElseThrow(() -> new RuntimeException("Emitente not found"));

            String url = ufWebService.getUrl(emitente.getUf(), servico);

            File xmlFile = new File("temp.xml");
            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(xml);
            }

            xmlSignatureService.sign(xmlFile.getAbsolutePath(), emitente.getCaminhoCertificado(), emitente.getSenha());

            String signedXml = new String(Files.readAllBytes(xmlFile.toPath()));

            HttpResponse<String> response = sefazService.send(url, signedXml, emitente, servico);

            TransactionLog log = new TransactionLog();
            log.setCnpj(cnpj);
            log.setXmlEnviado(xml); // Logs the original XML
            log.setXmlResposta(response.body());
            log.setStatusHttp(response.statusCode());
            transactionLogRepository.save(log);

            return new FiscalResponse(response.statusCode(), response.body(), "Success");
        } catch (Exception e) {
            return new FiscalResponse(500, null, e.getMessage());
        }
    }
}
