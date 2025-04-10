package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;

import com.google.gson.Gson;

import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DataDogLoggingObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DataDogOutboundRequestLog;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.EventObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.FulfillmentObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.MetricsLoggingObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.OrchestratorObject;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

@Service
public class PartnerFulfillmentOrchestratorServiceImpl implements PartnerFulfillmentOrchestratorService {

    @Value("${benefit.url}")
    private String benefitUrl;

    @Value("${orchestrator.retry}")
    private String orchestratorRetryAttempts;

    @Override
    public void markFulfillmentAsComplete(Map<String, FulfillmentObject> fulfillments, String accessToken, OauthService oauthService, MetricsLoggingObject metricsLoggingObject) throws IOException, InterruptedException, URISyntaxException {
        int numberOfSuccess = 0;
        int numberOfFailures = 0;
        List<String> fulfillmentsWithNoDate = new ArrayList<>();
        for (Map.Entry<String, FulfillmentObject> entry : fulfillments.entrySet()) {
            FulfillmentObject fulfillment = entry.getValue();
            OrchestratorObject orchestratorObject = new OrchestratorObject();
            orchestratorObject.setStatus("COMPLETED");
            orchestratorObject.setVersionKey(fulfillment.getVersionKey());
            if(entry.getValue() != null && entry.getValue().getEffectiveDate() != null && convertTimeStringToCorrectFormat(entry.getValue().getEffectiveDate()) != null) {
                EventObject eventObject = new EventObject();
                eventObject.setValue(convertTimeStringToCorrectFormat(entry.getValue().getEffectiveDate()).replace(".000", ".999"));
                eventObject.setTimezone("UTC");
                orchestratorObject.setEffectiveDate(eventObject);
            } else {
                fulfillmentsWithNoDate.add(entry.getValue().getFulfillmentId());
            }

            String uri = URI.create("/partner-fulfillment-orchestrator/fulfillments/" + fulfillment.getFulfillmentId()).toString();

            long startTime = System.currentTimeMillis();
            CloseableHttpResponse response = null;
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPatch httpPatch = new HttpPatch(new URI(benefitUrl + uri));
            httpPatch.setHeader("Content-Type", "application/json");
            httpPatch.setHeader("Authorization", "Bearer " + accessToken);
            String correlationId = "MonthlyBatch-" + UUID.randomUUID().toString();
            Gson objectMapper = new Gson();
            httpPatch.setHeader("x-correlation-id", correlationId);
            try {
                StringEntity params = new StringEntity(objectMapper.toJson(orchestratorObject), ContentType.APPLICATION_JSON);
                httpPatch.setEntity(params);
                response = httpClient.execute(httpPatch);
                if(response.getStatusLine().getStatusCode() == 401) {
                    accessToken = oauthService.getOauthToken();
                    httpPatch.setHeader("Authorization", "Bearer " + accessToken);
                    httpPatch.setEntity(params);
                    response = httpClient.execute(httpPatch);
                }
                if(response.getStatusLine().getStatusCode() != 200) {
                    numberOfFailures++;
                    System.out.println(objectMapper.toJson(orchestratorObject));
                    long elapsed = System.currentTimeMillis() - startTime;
                    DataDogLoggingObject dataDogLoggingObject = new DataDogLoggingObject("Error During Domain Call", String.valueOf(elapsed), String.valueOf(response.getStatusLine().getStatusCode()), null, correlationId);
                    System.out.println(objectMapper.toJson(dataDogLoggingObject));
                } else {
                    numberOfSuccess++;
                    DataDogOutboundRequestLog dataDogOutboundRequestLog = new DataDogOutboundRequestLog();
                    dataDogOutboundRequestLog.setCorrelationId(correlationId);
                    dataDogOutboundRequestLog.setURI(uri);
                    long elapsed = System.currentTimeMillis() - startTime;
                    dataDogOutboundRequestLog.setDuration(String.valueOf(elapsed));
                    dataDogOutboundRequestLog.setMethod("PATCH");
                    dataDogOutboundRequestLog.setFulfillmentId(entry.getValue().getFulfillmentId());
                    Map<String, String> headers = new HashMap<>();
                    for(Header header : httpPatch.getAllHeaders()) {
                        headers.put(header.getName(), header.getValue());
                    }
                    dataDogOutboundRequestLog.setResponseCode("200");
                    dataDogOutboundRequestLog.setHeaders(headers);
                    System.out.println(objectMapper.toJson(dataDogOutboundRequestLog));
                }
            } catch (HttpClientErrorException e) {
                numberOfFailures++;
                if (e.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))) {
                    httpPatch.setHeader("Authorization", "Bearer " + oauthService.getOauthToken());
                } else {
                    long elapsed = System.currentTimeMillis() - startTime;
                    DataDogLoggingObject dataDogLoggingObject = new DataDogLoggingObject("Error During Domain Call", String.valueOf(elapsed), "500", e.getCause() != null ? getStackTraceAsString(e.getCause()) : null, correlationId);
                    System.out.println(objectMapper.toJson(dataDogLoggingObject));
                    DataDogLoggingObject dataDogLoggingObject2 = new DataDogLoggingObject("Value Not processed calling orchestrator service for Fulfillment id: " + fulfillment.getFulfillmentId(), String.valueOf(elapsed), "500", e.getCause() != null ? getStackTraceAsString(e.getCause()) : null, correlationId);
                    System.out.println(objectMapper.toJson(dataDogLoggingObject2));
                }
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                DataDogLoggingObject dataDogLoggingObject2 = new DataDogLoggingObject("Value Not processed calling orchestrator service for Fulfillment id: " + fulfillment.getFulfillmentId(), String.valueOf(elapsed), "500", e.getCause() != null ? getStackTraceAsString(e.getCause()) : null, correlationId);
                System.out.println(objectMapper.toJson(dataDogLoggingObject2));
            }
            metricsLoggingObject.setNumberOfUpdateCalls(String.valueOf(numberOfSuccess));
            metricsLoggingObject.setNumberOfUpdateErrors(String.valueOf(numberOfFailures));
            metricsLoggingObject.setCodesWithNoEffectiveDate(fulfillmentsWithNoDate);
        }
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private String convertTimeStringToCorrectFormat(String dateTime) {
        try {
            dateTime = dateTime.replace(" UTC", "");
            dateTime = dateTime.replace("Z", "");

            if (dateTime.contains(".")) {
                String[] parts = dateTime.split("\\.");
                if (parts.length > 1) {
                    String milliseconds = parts[1];
                    if (milliseconds.length() > 3) {
                        milliseconds = milliseconds.substring(0, 3);
                    } else if (milliseconds.length() < 3) {
                        milliseconds = String.format("%-3s", milliseconds).replace(' ', '0');
                    }
                    dateTime = parts[0] + "." + milliseconds;
                }
            }

            dateTime = dateTime + "Z";

            List<SimpleDateFormat> knownPatterns = new ArrayList<>();
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss'Z'"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
            knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            knownPatterns.add(new SimpleDateFormat("MM/dd/yyyy' 'HH:mm"));
            knownPatterns.add(new SimpleDateFormat("MM/d/yyyy' 'HH:mm"));
            knownPatterns.add(new SimpleDateFormat("MM/d/yy' 'HH:mm"));

            Date date = null;

            for (SimpleDateFormat pattern : knownPatterns) {
                try {
                    // Take a try
                    pattern.setTimeZone(TimeZone.getTimeZone("UTC"));
                    date = new Date(pattern.parse(dateTime).getTime());

                } catch (ParseException pe) {
                    // Loop on
                }
            }

            if(date == null) {
                return null;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(date);
        } catch (Exception e) {
            return null;
        }
    }
}
