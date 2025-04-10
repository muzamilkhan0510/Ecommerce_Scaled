package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;

import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.CSVDataObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.FulfillmentObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.MetricsLoggingObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PartnerFulfillmentDomainService {

    public void writeFulfillmentsToFile(String accessToken, OauthService oauthService) throws IOException, InterruptedException;
    public Map<String, FulfillmentObject> retrieveFulfillmentIds(List<CSVDataObject> input, String accessToken, OauthService oauthService, MetricsLoggingObject metricsLoggingObject) throws IOException, InterruptedException;
    public Map<String, FulfillmentObject> retrieveVersionKeys(Map<String, FulfillmentObject> input, String accessToken, OauthService oauthService, MetricsLoggingObject metricsLoggingObject) throws IOException, InterruptedException;
}
