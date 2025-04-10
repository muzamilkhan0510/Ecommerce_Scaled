package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

import java.util.List;

public class DomainObjectWithFulfillmentId {

    List<DomainFulfillmentsObjectWithFulfillmentId> data;
    MetaObject metaObject;
    IncludedObject included;

    public List<DomainFulfillmentsObjectWithFulfillmentId> getData() {
        return data;
    }

    public void setData(List<DomainFulfillmentsObjectWithFulfillmentId> data) {
        this.data = data;
    }

    public MetaObject getMetaObject() {
        return metaObject;
    }

    public void setMetaObject(MetaObject metaObject) {
        this.metaObject = metaObject;
    }

    public IncludedObject getIncluded() {
        return included;
    }

    public void setIncluded(IncludedObject included) {
        this.included = included;
    }
}
