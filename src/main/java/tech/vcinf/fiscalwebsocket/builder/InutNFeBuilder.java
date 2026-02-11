package tech.vcinf.fiscalwebsocket.builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class InutNFeBuilder implements XmlBuilder {
    @Override
    public String build(Map<String, Object> data, String cnpj, String uf) {
        String ambiente = (String) data.getOrDefault("ambiente", "1");
        String ano = String.valueOf(LocalDateTime.now().getYear()).substring(2);
        String serie = (String) data.get("serie");
        String numIni = (String) data.get("numIni");
        String numFin = (String) data.get("numFin");
        String justificativa = (String) data.get("justificativa");
        String id = String.format("ID%s%s%s%s%s", uf, ano, cnpj, "55", serie, numIni);

        return String.format(
            "<inutNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">" +
            "<infInut Id=\"%s\">" +
            "<tpAmb>%s</tpAmb>" +
            "<xServ>INUTILIZAR</xServ>" +
            "<cUF>%s</cUF>" +
            "<ano>%s</ano>" +
            "<CNPJ>%s</CNPJ>" +
            "<mod>55</mod>" +
            "<serie>%s</serie>" +
            "<nNFIni>%s</nNFIni>" +
            "<nNFFin>%s</nNFFin>" +
            "<xJust>%s</xJust>" +
            "</infInut>" +
            "</inutNFe>",
            id, ambiente, uf, ano, cnpj, serie, numIni, numFin, justificativa
        );
    }
}
