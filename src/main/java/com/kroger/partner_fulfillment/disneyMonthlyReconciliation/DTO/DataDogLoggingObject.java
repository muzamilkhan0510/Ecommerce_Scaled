package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

public class DataDogLoggingObject {
    private String message;
    private String duration;
    private String status;
    private String stackTrace;
    private String correlationId;

    public DataDogLoggingObject(String message, String duration, String status, String correlationId) {
        this.message = message;
        this.duration = duration;
        this.status = status;
        this.correlationId = correlationId;
    }

    public DataDogLoggingObject(String message, String duration, String status, String stackTrace, String correlationId) {
        this.message = message;
        this.duration = duration;
        this.status = status;
        this.stackTrace = stackTrace;
        this.correlationId = correlationId;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public DataDogLoggingObject() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
