package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

public class DomainFulfillmentsObjectWithFulfillmentId {

    String fulfillmentId;
    String partner;
    String type;
    String status;
    String customerId;
    String versionKey;
    String redirectUrl;
    EventObject created;
    EventObject modified;

    public String getFulfillmentId() {
        return fulfillmentId;
    }

    public void setFulfillmentId(String fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
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

    public String getVersionKey() {
        return versionKey;
    }

    public void setVersionKey(String versionKey) {
        this.versionKey = versionKey;
    }

    public EventObject getCreated() {
        return created;
    }

    public void setCreated(EventObject created) {
        this.created = created;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public EventObject getModified() {
        return modified;
    }

    public void setModified(EventObject modified) {
        this.modified = modified;
    }
}
