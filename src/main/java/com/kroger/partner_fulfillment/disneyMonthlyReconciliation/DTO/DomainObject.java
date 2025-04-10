package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

import java.util.List;

public class DomainObject {

    List<DomainFulfillmentsObject> data;
    MetaObject metaObject;
    IncludedObject included;

    public List<DomainFulfillmentsObject> getData() {
        return data;
    }

    public void setData(List<DomainFulfillmentsObject> data) {
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
