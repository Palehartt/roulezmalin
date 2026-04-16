package com.roulezmalin.backend.model;

public class MessageReponse {
    private String requestId;
    private String message;
    private int status;

    public MessageReponse() {}  

    public MessageReponse(String message, String requestId, int status) {
        this.message = message;
        this.requestId = requestId;
        this.status = status;
    }

    public void setRequestId(String requestId) { this.requestId = requestId; } 
    public void setMessage(String message) { this.message = message; }           
    public void setStatus(int status) { this.status = status; }                  

    public String getMessage() { return this.message; }
    public String getRequestId() { return this.requestId; }
    public int getStatus() { return this.status; }
}