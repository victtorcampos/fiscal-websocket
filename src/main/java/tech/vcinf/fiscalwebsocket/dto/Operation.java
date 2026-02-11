package tech.vcinf.fiscalwebsocket.dto;

import java.util.List;

public class Operation {
    private String chave;
    private String label;
    private String descricao;
    private boolean requerAssinatura;
    private List<FormField> camposFormulario;

    // Getters and Setters
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isRequerAssinatura() { return requerAssinatura; }
    public void setRequerAssinatura(boolean requerAssinatura) { this.requerAssinatura = requerAssinatura; }
    public List<FormField> getCamposFormulario() { return camposFormulario; }
    public void setCamposFormulario(List<FormField> camposFormulario) { this.camposFormulario = camposFormulario; }
}
