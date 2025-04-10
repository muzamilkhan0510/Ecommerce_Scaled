package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

public class FulfillmentData {
    private String code;
    private String redirectUrl;
    private String partner;
    private String type;
    private String partnerFulfillmentId;
    private String expiration;
    private EventObject updated;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
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

    public String getPartnerFulfillmentId() {
        return partnerFulfillmentId;
    }

    public void setPartnerFulfillmentId(String partnerFulfillmentId) {
        this.partnerFulfillmentId = partnerFulfillmentId;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public EventObject getUpdated() {
        return updated;
    }

    public void setUpdated(EventObject updated) {
        this.updated = updated;
    }
}
