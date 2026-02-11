package tech.vcinf.fiscalwebsocket.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String cnpj;

    @Column(columnDefinition = "TEXT")
    private String xmlEnviado;

    @Column(columnDefinition = "TEXT")
    private String xmlResposta;

    private int statusHttp;
    private LocalDateTime data = LocalDateTime.now();

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getXmlEnviado() {
        return xmlEnviado;
    }

    public void setXmlEnviado(String xmlEnviado) {
        this.xmlEnviado = xmlEnviado;
    }

    public String getXmlResposta() {
        return xmlResposta;
    }

    public void setXmlResposta(String xmlResposta) {
        this.xmlResposta = xmlResposta;
    }

    public int getStatusHttp() {
        return statusHttp;
    }

    public void setStatusHttp(int statusHttp) {
        this.statusHttp = statusHttp;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }
}
