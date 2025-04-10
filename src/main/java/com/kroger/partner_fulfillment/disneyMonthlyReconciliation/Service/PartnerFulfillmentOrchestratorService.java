package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;


import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.FulfillmentObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.MetricsLoggingObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public interface PartnerFulfillmentOrchestratorService {
    public void markFulfillmentAsComplete(Map<String, FulfillmentObject> fulfillments, String accessToken, OauthService oauthService, MetricsLoggingObject metricsLoggingObject) throws IOException, InterruptedException, URISyntaxException;
}
