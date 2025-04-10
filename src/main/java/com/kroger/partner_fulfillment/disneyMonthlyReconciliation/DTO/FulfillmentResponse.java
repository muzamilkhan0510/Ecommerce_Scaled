package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

public class FulfillmentResponse {
    private FulfillmentData[] data;
    private IncludedObject included;
    private MetaObject meta;

    public FulfillmentData[] getData() {
        return data;
    }

    public void setData(FulfillmentData[] data) {
        this.data = data;
    }

    public IncludedObject getIncluded() {
        return included;
    }

    public void setIncluded(IncludedObject included) {
        this.included = included;
    }

    public MetaObject getMeta() {
        return meta;
    }

    public void setMeta(MetaObject meta) {
        this.meta = meta;
    }
}
