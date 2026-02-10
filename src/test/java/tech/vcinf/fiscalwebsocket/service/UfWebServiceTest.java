package tech.vcinf.fiscalwebsocket.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UfWebServiceTest {

    @Test
    void getUrl() {
        UfWebService ufWebService = new UfWebService();
        String url = ufWebService.getUrl("MT", "NFE");
        assertEquals("https://nfe.sefaz.mt.gov.br/nfews/v2/services/NfeAutorizacao4?wsdl", url);
    }
}
