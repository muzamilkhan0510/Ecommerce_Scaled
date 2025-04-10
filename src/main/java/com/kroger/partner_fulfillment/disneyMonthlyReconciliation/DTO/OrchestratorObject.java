package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

public class OrchestratorObject {
    private String status;
    private String versionKey;
    private EventObject effectiveDate;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersionKey() {
        return versionKey;
    }

    public void setVersionKey(String versionKey) {
        this.versionKey = versionKey;
    }

    public EventObject getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(EventObject effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
