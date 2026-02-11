package tech.vcinf.fiscalwebsocket.dto;

import java.util.List;

public class OperationCategory {
    private String nome;
    private String icone;
    private List<Operation> operacoes;

    // Getters and Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
    public List<Operation> getOperacoes() { return operacoes; }
    public void setOperacoes(List<Operation> operacoes) { this.operacoes = operacoes; }
}
