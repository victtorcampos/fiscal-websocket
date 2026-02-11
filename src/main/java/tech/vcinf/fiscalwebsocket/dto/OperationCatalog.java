package tech.vcinf.fiscalwebsocket.dto;

import java.util.List;

public class OperationCatalog {
    private String modelo;
    private String uf;
    private String ambiente;
    private List<OperationCategory> categorias;

    // Getters and Setters
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public List<OperationCategory> getCategorias() { return categorias; }
    public void setCategorias(List<OperationCategory> categorias) { this.categorias = categorias; }
}
