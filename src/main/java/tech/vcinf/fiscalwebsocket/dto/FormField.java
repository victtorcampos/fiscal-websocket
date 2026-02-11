package tech.vcinf.fiscalwebsocket.dto;

public class FormField {
    private String name;
    private String label;
    private String type;
    private String placeholder;
    private boolean required;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
