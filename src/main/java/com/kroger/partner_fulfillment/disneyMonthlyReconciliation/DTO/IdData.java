package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

public class IdData {
    private String id;
    private String partner;
    private String type;
    private String status;
    private String customerId;
    private EventObject created;
    private EventObject modified;
    private String versionKey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public EventObject getCreated() {
        return created;
    }

    public void setCreated(EventObject created) {
        this.created = created;
    }

    public EventObject getModified() {
        return modified;
    }

    public void setModified(EventObject modified) {
        this.modified = modified;
    }

    public String getVersionKey() {
        return versionKey;
    }

    public void setVersionKey(String versionKey) {
        this.versionKey = versionKey;
    }
}
