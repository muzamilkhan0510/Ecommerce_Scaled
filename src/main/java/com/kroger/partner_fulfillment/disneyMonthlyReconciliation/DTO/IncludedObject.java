package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

public class IncludedObject {
    private PartnerFulfillmentsObject[] partnerFulfillments;

    public PartnerFulfillmentsObject[] getPartnerFulfillments() {
        return partnerFulfillments;
    }

    public void setPartnerFulfillments(PartnerFulfillmentsObject[] partnerFulfillments) {
        this.partnerFulfillments = partnerFulfillments;
    }
}
