package tech.vcinf.fiscalwebsocket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import tech.vcinf.fiscalwebsocket.builder.XmlBuilder;
import tech.vcinf.fiscalwebsocket.dto.*;
import tech.vcinf.fiscalwebsocket.model.Emitente;
import tech.vcinf.fiscalwebsocket.model.TransactionLog;
import tech.vcinf.fiscalwebsocket.repository.EmitenteRepository;
import tech.vcinf.fiscalwebsocket.repository.TransactionLogRepository;
import tech.vcinf.fiscalwebsocket.service.OperationDiscoveryService;
import tech.vcinf.fiscalwebsocket.service.SefazService;
import tech.vcinf.fiscalwebsocket.service.UfWebService;
import tech.vcinf.fiscalwebsocket.service.XmlSignatureService;
import tech.vcinf.fiscalwebsocket.util.CertificateUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class FiscalController {

    private final EmitenteRepository emitenteRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UfWebService ufWebService;
    private final XmlSignatureService xmlSignatureService;
    private final SefazService sefazService;
    private final OperationDiscoveryService operationDiscoveryService;
    private final Map<String, XmlBuilder> xmlBuilders;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> SERVICES_WITHOUT_SIGNATURE = Set.of("STATUS", "CONSULTA", "CONSULTA_CADASTRO", "RET_AUTORIZACAO");

    private static final Set<String> SERVICES_TO_LOG = Set.of("AUTORIZACAO", "INUTILIZACAO", "EVENTO");

    public FiscalController(EmitenteRepository emitenteRepository,
                            TransactionLogRepository transactionLogRepository,
                            UfWebService ufWebService,
                            XmlSignatureService xmlSignatureService,
                            SefazService sefazService,
                            OperationDiscoveryService operationDiscoveryService,
                            Map<String, XmlBuilder> xmlBuilders) {
        this.emitenteRepository = emitenteRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.ufWebService = ufWebService;
        this.xmlSignatureService = xmlSignatureService;
        this.sefazService = sefazService;
        this.operationDiscoveryService = operationDiscoveryService;
        this.xmlBuilders = xmlBuilders;
    }

    @MessageMapping("/transmitir")
    @SendTo("/topic/responses")
    public FiscalResponse transmitir(FiscalRequest request) {
        File tempFile = null;
        try {
            String action = request.getAction();
            Object data = request.getData();

            if ("get_operations".equals(action)) {
                 @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) data;
                String modelo = (String) payload.get("modelo");
                String uf = (String) payload.get("uf");
                String ambiente = (String) payload.get("ambiente");
                OperationCatalog catalog = operationDiscoveryService.getAvailableOperations(modelo, uf, ambiente);
                return new FiscalResponse("operations_catalog", catalog);
            }

            if ("register".equals(action)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) data;
                String arquivoBase64 = (String) payload.get("arquivoBase64");
                String senha = (String) payload.get("senha");
                byte[] pfxBytes = Base64.getDecoder().decode(arquivoBase64);
                X509Certificate certificate = CertificateUtils.getCertificate(new ByteArrayInputStream(pfxBytes), senha);
                Map<String, String> certInfo = CertificateUtils.extractInfo(certificate);
                String cnpj = certInfo.get("cnpj");
                if (cnpj == null || cnpj.isEmpty()) {
                    throw new IllegalArgumentException("Não foi possível extrair o CNPJ do certificado digital.");
                }
                String certPath = "cert_" + cnpj + ".pfx";
                try (FileOutputStream fos = new FileOutputStream(certPath)) {
                    fos.write(pfxBytes);
                }
                Emitente emitente = emitenteRepository.findById(cnpj).orElse(new Emitente());
                emitente.setCnpj(cnpj);
                emitente.setSenha(senha);
                emitente.setCaminhoCertificado(certPath);
                emitente.setRazaoSocial(certInfo.get("razaoSocial"));
                LocalDateTime validity = LocalDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.systemDefault());
                emitente.setdValidate(validity);
                emitente.setUf((String) payload.get("uf"));
                emitente.setTipo((String) payload.get("tipo"));
                emitenteRepository.save(emitente);
                return new FiscalResponse(200, null, "Emitente registered successfully", "register");
            }

            if ("list_emitentes".equals(action)) {
                List<EmitenteInfo> emitentes = emitenteRepository.findAll().stream()
                        .map(e -> new EmitenteInfo(e.getCnpj(), e.getRazaoSocial(), e.getUf(), e.getTipo()))
                        .collect(Collectors.toList());
                return new FiscalResponse("list_emitentes", emitentes);
            }

            // Generic transmission logic
            String cnpj = request.getCnpj();
            Emitente emitente = emitenteRepository.findById(cnpj)
                    .orElseThrow(() -> new RuntimeException("Emitente not found: " + cnpj));

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) data;
            String modelo = (String) payload.get("modelo");
            String ambiente = (String) payload.get("ambiente");
            String servico = request.getServico(); // This is the simple service key like "STATUS", "CONSULTA"

            String xml;
            XmlBuilder builder = xmlBuilders.get(servico);
            if (builder != null) {
                xml = builder.build(payload, cnpj, emitente.getUf());
            } else {
                // For services like AUTORIZACAO, XML comes from client
                xml = request.getXml(); 
            }

            if (xml == null || xml.isEmpty()) {
                 throw new IllegalArgumentException("XML de entrada não pode ser vazio para esta operação.");
            }

            String url = ufWebService.getUrl(modelo, servico, emitente.getUf(), ambiente);
            System.out.println("URL Serviço Sefaz: " + url);

            String xmlToSend = xml;
            if (!SERVICES_WITHOUT_SIGNATURE.contains(servico)) {
                tempFile = File.createTempFile("fiscal_xml_", ".xml");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(xml);
                }
                xmlSignatureService.sign(tempFile.getAbsolutePath(), emitente.getCaminhoCertificado(), emitente.getSenha());
                xmlToSend = new String(Files.readAllBytes(tempFile.toPath()));
            }

            String responseBody = sefazService.send(url, xmlToSend, emitente, servico);

            if (SERVICES_TO_LOG.contains(servico)) {
                try {
                    TransactionLog log = new TransactionLog();
                    log.setCnpj(cnpj);
                    log.setXmlEnviado(xml);
                    log.setXmlResposta(responseBody);
                    log.setStatusHttp(200);
                    transactionLogRepository.save(log);
                    System.out.println("Transação registrada no banco: " + servico);
                } catch (Exception e) {
                    System.err.println("ERRO ao salvar log de transação: " + e.getMessage());
                }
            } else {
                System.out.println("Serviço consultivo - sem log persistente: " + servico);
            }

            return new FiscalResponse(200, responseBody, "Success", action);

        } catch (Exception e) {
            e.printStackTrace();
            return new FiscalResponse(500, null, e.getClass().getSimpleName() + ": " + e.getMessage(), request.getAction());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }
}
