package tech.vcinf.fiscalwebsocket.dto;

public class FiscalResponse {
    private int status;
    private Object body;
    private String message;
    private String originalAction;

    public FiscalResponse(int status, Object body, String message, String originalAction) {
        this.status = status;
        this.body = body;
        this.message = message;
        this.originalAction = originalAction;
    }

    public FiscalResponse(int status, Object body, String message) {
        this(status, body, message, null);
    }

    public FiscalResponse(String originalAction, Object body) {
        this(200, body, "Success", originalAction);
    }

    // Getters e Setters

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOriginalAction() {
        return originalAction;
    }

    public void setOriginalAction(String originalAction) {
        this.originalAction = originalAction;
    }
}
