package tech.vcinf.fiscalwebsocket.dto;

public class FiscalResponse {
    private int status;
    private String body;
    private String message;

    public FiscalResponse(int status, String body, String message) {
        this.status = status;
        this.body = body;
        this.message = message;
    }

    // Getters e Setters

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
