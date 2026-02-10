package tech.vcinf.fiscalwebsocket.util;

public class SoapEnvelopeUtils {

    private static final String SOAP_HEADER = "<soap:Header><nfeCabecMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/%s\"><cUF>%s</cUF><versaoDados>%s</versaoDados></nfeCabecMsg></soap:Header>";
    private static final String SOAP_BODY_START = "<soap:Body><nfeDadosMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/%s\">";
    private static final String SOAP_BODY_END = "</nfeDadosMsg></soap:Body>";
    private static final String SOAP_ENVELOPE = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">%s%s</soap:Envelope>";

    public static String createEnvelope(String xml, String service, String uf) {
        switch (service) {
            case "NFeStatusServico4":
                return buildStatusServicoEnvelope(xml, uf, service);
            case "NFeAutorizacao4":
                return buildAutorizacaoLoteEnvelope(xml, uf, service);
            // Add other services here
            default:
                throw new IllegalArgumentException("Unknown service: " + service);
        }
    }

    private static String buildStatusServicoEnvelope(String xml, String uf, String service) {
        String versaoDados = "4.00"; // Version for NFeStatusServico4
        String header = String.format(SOAP_HEADER, service, uf, versaoDados);
        String body = String.format(SOAP_BODY_START, service) + xml + SOAP_BODY_END;
        return String.format(SOAP_ENVELOPE, header, body);
    }

    private static String buildAutorizacaoLoteEnvelope(String xml, String uf, String service) {
        String versaoDados = "4.00"; // Version for NFeAutorizacao4
        String header = String.format(SOAP_HEADER, service, uf, versaoDados);
        String body = String.format(SOAP_BODY_START, service) + xml + SOAP_BODY_END;
        return String.format(SOAP_ENVELOPE, header, body);
    }
}
