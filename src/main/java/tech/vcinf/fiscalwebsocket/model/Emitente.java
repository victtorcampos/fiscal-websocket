package tech.vcinf.fiscalwebsocket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
 
@Entity
public class Emitente {
    @Id
    private String cnpj;

    @Column(nullable = true)
    private String razaoSocial;

    private String uf;
    private String caminhoCertificado;
    private String senha;
    private String tipo;

    @Column(nullable = true)
    private LocalDateTime dValidate;

    // Getters and Setters

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

    public String getCaminhoCertificado() {
        return caminhoCertificado;
    }

    public void setCaminhoCertificado(String caminhoCertificado) {
        this.caminhoCertificado = caminhoCertificado;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getdValidate() {
        return dValidate;
    }

    public void setdValidate(LocalDateTime dValidate) {
        this.dValidate = dValidate;
    }
}
