package tech.vcinf.fiscalwebsocket.dto;

public class EmitenteInfo {
    private String cnpj;
    private String razaoSocial;
    private String uf;
    private String tipo;

    public EmitenteInfo(String cnpj, String razaoSocial, String uf, String tipo) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.uf = uf;
        this.tipo = tipo;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
