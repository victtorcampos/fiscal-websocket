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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class FiscalController {

    private final EmitenteRepository emitenteRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UfWebService ufWebService;
    private final XmlSignatureService xmlSignatureService;
    private final SefazService sefazService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> SERVICE_NAME_MAP = Map.of(
            "NFeStatusServico4", "STATUS",
            "NFeAutorizacao4", "AUTORIZACAO",
            "NFeRetAutorizacao4", "RET_AUTORIZACAO",
            "NFeConsulta4", "CONSULTA",
            "NFeInutilizacao4", "INUTILIZACAO",
            "RecepcaoEvento4", "EVENTO",
            "CadConsultaCadastro4", "CONSULTA_CADASTRO"
    );

    private static final Set<String> SERVICES_WITHOUT_SIGNATURE = Set.of("STATUS", "CONSULTA", "CONSULTA_CADASTRO", "RET_AUTORIZACAO");

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
        File tempFile = null;
        try {
            if ("register".equals(request.getAction())) {
                Emitente emitente = objectMapper.convertValue(request.getData(), Emitente.class);
                emitenteRepository.save(emitente);
                return new FiscalResponse(200, null, "Emitente registered successfully");
            }

            String cnpj = request.getCnpj();
            String xml = request.getXml();
            String servicoCompleto = request.getServico();

            Emitente emitente = emitenteRepository.findById(cnpj)
                    .orElseThrow(() -> new RuntimeException("Emitente not found: " + cnpj));

            String modelo = extractFromXml(xml, "mod");
            if (modelo == null) {
                if (servicoCompleto.toUpperCase().contains("NFCE")) {
                    modelo = "NFCE";
                } else {
                    modelo = "NFE"; // Fallback for services like status check
                }
            }

            String ambiente = "1".equals(extractFromXml(xml, "tpAmb")) ? "PROD" : "HOMOL";

            String servicoSimples = SERVICE_NAME_MAP.get(servicoCompleto);
            if (servicoSimples == null) {
                throw new IllegalArgumentException("Serviço não mapeado: " + servicoCompleto);
            }

            String url = ufWebService.getUrl(modelo, servicoSimples, emitente.getUf(), ambiente);

            System.out.println("URL Serviço Sefaz: " + url);

            String xmlToSend = xml;
            if (!SERVICES_WITHOUT_SIGNATURE.contains(servicoSimples)) {
                tempFile = File.createTempFile("fiscal_xml_", ".xml");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(xml);
                }

                xmlSignatureService.sign(tempFile.getAbsolutePath(), emitente.getCaminhoCertificado(), emitente.getSenha());
                xmlToSend = new String(Files.readAllBytes(tempFile.toPath()));
            }


            HttpResponse<String> response = sefazService.send(url, xmlToSend, emitente, servicoCompleto);

            TransactionLog log = new TransactionLog();
            log.setCnpj(cnpj);
            log.setXmlEnviado(xml);
            log.setXmlResposta(response.body());
            log.setStatusHttp(response.statusCode());
            transactionLogRepository.save(log);

            return new FiscalResponse(response.statusCode(), response.body(), "Success");
        } catch (Exception e) {
            e.printStackTrace(); // For better logging
            return new FiscalResponse(500, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private String extractFromXml(String xml, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + ">([^<]+)</" + tagName + ">");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null; // Tag not found
    }
}