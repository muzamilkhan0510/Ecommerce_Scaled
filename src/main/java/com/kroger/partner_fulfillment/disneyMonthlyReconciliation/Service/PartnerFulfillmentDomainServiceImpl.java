package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;

import com.google.gson.Gson;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.CSVDataObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DataDogLoggingObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DataDogOutboundRequestLog;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DomainFulfillmentsObjectWithFulfillmentId;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DomainObjectWithFulfillmentId;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.FulfillmentObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.FulfillmentResponse;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.IdDataObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.MetricsLoggingObject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Service
public class PartnerFulfillmentDomainServiceImpl implements PartnerFulfillmentDomainService {

    @Value("${fulfillment.url}")
    private String fulfillmentUrl;

    @Value("${domain.batchSize}")
    private String batchSize;

    @Value("${domain.file.name}")
    private String csvFileName;

    @Value("${domain.retry.attempts}")
    private String domainRetryAttempts;

    @Override
    public void writeFulfillmentsToFile(String accessToken, OauthService oauthService) throws IOException, InterruptedException {
        // Make call to api and loop until no records left
        // Create new oauth token if expired returns 401
        int offset = 0;
        int pageSize = Integer.parseInt(batchSize);
        PrintWriter pw = new PrintWriter(new FileWriter(csvFileName, true), true);
        String correlationId = null;
        int retryCount = 5;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            long startTime = System.currentTimeMillis();
            try {
                LocalDate currentDate = LocalDate.now();
                LocalDate startDate = LocalDate.of(2024, 7, 30);
                while (!startDate.isAfter(currentDate)) {
                    LocalDate endDate = startDate.plusDays(59).isAfter(currentDate) ? currentDate : startDate.plusDays(59);
                    correlationId = "MonthlyBatch-" + UUID.randomUUID();
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + accessToken);
                    headers.set("accept", "application/json");
                    headers.set("x-correlation-id", correlationId);
                    HttpEntity entity = new HttpEntity<>(headers);
                    while(true) {
                        String formattedStartDate = startDate.format(dateFormatter);
                        String formattedEndDate = endDate.format(dateFormatter);
                        URI uri = UriComponentsBuilder.fromHttpUrl(fulfillmentUrl + "/partner-fulfillment-domain/v1/audit/fulfillments")
                                .queryParam("page.offset", offset)
                                .queryParam("page.size", pageSize)
                                .queryParam("filter.startDate", formattedStartDate)
                                .queryParam("filter.endDate", formattedEndDate)
                                .build()
                                .toUri();
                        ResponseEntity<DomainObjectWithFulfillmentId> response = restTemplate.exchange(uri, HttpMethod.GET, entity, DomainObjectWithFulfillmentId.class);
                        if (response.getStatusCode() == HttpStatusCode.valueOf(200)) {
                            List<DomainFulfillmentsObjectWithFulfillmentId> fulfillmentObjects = response.getBody().getData();
                            if (fulfillmentObjects.size() == 0) {
                                logSuccess("Complete: Ending at " + offset, startTime, correlationId);
                                break;
                            }
                            logSuccess("Success: Writing from " + offset + " Size of: " + pageSize, startTime, correlationId);
                            System.out.println(uri);
                            writeToFile(fulfillmentObjects, pw);
                            offset += Integer.parseInt(batchSize);
                        }
                    }
                    offset=0;
                    startDate = endDate.plusDays(1);
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    accessToken = oauthService.getOauthToken();
                } else if (e.getStatusCode() == HttpStatusCode.valueOf(404)) {
                    logError("404 Not Found Hitting domain service", e, startTime, correlationId);
                } else {
                    logError("Error During Fulfillment retrieval: ", e, startTime, correlationId);
                }
            } catch (Exception e) {
                logError("Error During Fulfillment retrieval: ", e, startTime, correlationId);
                retryCount--;
                TimeUnit.SECONDS.sleep(5);
        }
        pw.close();
    }

    private void writeToFile(List<DomainFulfillmentsObjectWithFulfillmentId> fulfillmentObjects, PrintWriter printWriter) {
        //write data to file
        for(DomainFulfillmentsObjectWithFulfillmentId domainFulfillmentsObject : fulfillmentObjects) {
            if(!domainFulfillmentsObject.getStatus().equalsIgnoreCase("CANCELLED") && !domainFulfillmentsObject.getStatus().equalsIgnoreCase("CANCELED") && isNotBasic(domainFulfillmentsObject)) {
                String dateUpdated = domainFulfillmentsObject.getModified() != null ? domainFulfillmentsObject.getModified().getValue() : null;
                String status = domainFulfillmentsObject.getStatus().equalsIgnoreCase("COMPLETED") ? "TRUE" : "FALSE";
                printWriter.println(domainFulfillmentsObject.getCustomerId() + ":" + domainFulfillmentsObject.getFulfillmentId() + ",Active," + dateUpdated + "," + mapSku(domainFulfillmentsObject.getType())+ "," + status);
            }
        }
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private boolean isNotBasic(DomainFulfillmentsObjectWithFulfillmentId object) {
        if (Stream.of("DIS", "HULU", "ESPN").anyMatch(type -> type.equalsIgnoreCase(object.getType()))) {
            return true;
        }        
        return false;
    }

    private String mapSku(String type) {
        if(type.equalsIgnoreCase("DIS") || type.equalsIgnoreCase("DIS_BASIC")) {
            return "com.disney.kroger.us.bundle.annual.dplusbasicwads";
        } else if(type.equalsIgnoreCase("HULU") || type.equalsIgnoreCase("HULU_BASIC")) {
            return "com.hulu.kroger.us.bundle.annual.huluwads";
        } else if(type.equalsIgnoreCase("ESPN") || type.equalsIgnoreCase("ESPN_BASIC")) {
            return "com.espn.kroger.us.bundle.annual.espnplus";
        }
        return "";
    }

    @Override
    public Map<String, FulfillmentObject> retrieveFulfillmentIds(List<CSVDataObject> input, String accessToken, OauthService oauthService, MetricsLoggingObject metricsLoggingObject) throws IOException, InterruptedException {
        Map<String, FulfillmentObject> output = new HashMap<>();
        int metricsCounterGetSuccess = 0;
        int metricsCounterGetFailure = 0;
        int metricsCounterGetCallWithNoData = 0;
        List<String> metricsGetCallCodesWithNoDate = new ArrayList<>();
        List<String> metricCodesWithNoDate = new ArrayList<>();
        for (CSVDataObject map : input) {
            String correlationId = "MonthlyBatch-" + UUID.randomUUID().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("x-correlation-id", correlationId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String uri = URI.create("/partner-fulfillment-domain/v1/codes?filter.code=" + map.getPromotionCode().trim()).toString();

            RestTemplateBuilder builder = new RestTemplateBuilder().rootUri(fulfillmentUrl);
            RestTemplate domainRestTemplate = builder.build();

            ResponseEntity response = null;

            long startTime = System.currentTimeMillis();
            int attempts = (domainRetryAttempts != null && Integer.parseInt(domainRetryAttempts) > 0) ? Integer.parseInt(domainRetryAttempts) : 1;
            while (response == null && attempts > 0) {
                try {
                    metricsCounterGetSuccess++;
                    response = domainRestTemplate.exchange(uri, HttpMethod.GET, entity, FulfillmentResponse.class);
                    DataDogOutboundRequestLog dataDogOutboundRequestLog = new DataDogOutboundRequestLog();
                    dataDogOutboundRequestLog.setCorrelationId(correlationId);
                    dataDogOutboundRequestLog.setURI(uri);
                    long elapsed = System.currentTimeMillis() - startTime;
                    dataDogOutboundRequestLog.setDuration(String.valueOf(elapsed));
                    dataDogOutboundRequestLog.setMethod("GET");
                    dataDogOutboundRequestLog.setFulfillmentId(map.getPromotionCode());
                    dataDogOutboundRequestLog.setHeaders(entity.getHeaders().toSingleValueMap());
                    dataDogOutboundRequestLog.setResponseCode("200");
                    Gson objectMapper = new Gson();
                    System.out.println(objectMapper.toJson(dataDogOutboundRequestLog));
                } catch (HttpClientErrorException e) {
                    metricsCounterGetFailure++;
                    if(e.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))) {
                        accessToken = oauthService.getOauthToken();
                        headers.set("Authorization", "Bearer " + accessToken);
                        entity = new HttpEntity<>(headers);
                        attempts--;
                    } else {
                        logError("Error During Domain Call: Attempt " + ((Integer.parseInt(domainRetryAttempts) + 1) - attempts), e, startTime, correlationId);
                        if(attempts == 1) {
                            logError("Value Not processed calling domain service for CODE: " + map.getPromotionCode(), e, startTime, correlationId);
                        }
                        attempts--;
                    }
                }
            }
            if (response != null && response.getStatusCode() == HttpStatusCode.valueOf(200) && response.getBody() != null) {
                FulfillmentResponse fulfillmentResponse = (FulfillmentResponse) response.getBody();

                if(fulfillmentResponse.getData() != null && fulfillmentResponse.getData().length > 0) {
                    FulfillmentObject fulfillmentObject = new FulfillmentObject();
                    fulfillmentObject.setFulfillmentId(fulfillmentResponse.getData()[0].getPartnerFulfillmentId());
                    if(map.getRedeemedAt() != null) {
                        fulfillmentObject.setEffectiveDate(map.getRedeemedAt());
                    } else {
                        metricCodesWithNoDate.add(map.getPromotionCode());
                    }
                    output.put(map.getPromotionCode().trim(), fulfillmentObject);
                } else {
                    metricsCounterGetCallWithNoData++;
                    metricsGetCallCodesWithNoDate.add(map.getPromotionCode());
                    logNoResponse("No Data found for redemption code: " + map.getPromotionCode(), response, correlationId);
                }

            } else {
                metricsCounterGetFailure++;
                logNoResponse("Non 200 response from upstream for CODE: " + map.getPromotionCode(), response, correlationId);
            }
        }
        metricsLoggingObject.setNumberOfGetCallErrors(String.valueOf(metricsCounterGetFailure));
        metricsLoggingObject.setNumberOfGetCallsMade(String.valueOf(metricsCounterGetSuccess));
        metricsLoggingObject.setNumberOfGetCallsWithNoData(String.valueOf(metricsCounterGetCallWithNoData));
        metricsLoggingObject.setCodesWithNoDataFound(metricsGetCallCodesWithNoDate);
        metricsLoggingObject.setCodesWithNoEffectiveDate(metricCodesWithNoDate);
        return output;
    }

    @Override
    public Map<String, FulfillmentObject> retrieveVersionKeys(Map<String, FulfillmentObject> input, String accessToken, OauthService oauthService, MetricsLoggingObject metricsLoggingObject) throws IOException, InterruptedException {
        Map<String, FulfillmentObject> output = new HashMap<>();
        int numberOfCallsNotInPendingState = 0;
        List<String> codesNotInPendingState = new ArrayList<>();
        for (Map.Entry<String, FulfillmentObject> entry : input.entrySet()) {
            String correlationId = "MonthlyBatch-" + UUID.randomUUID().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("x-correlation-id", correlationId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String uri = URI.create("/partner-fulfillment-domain/v1/fulfillments/" + entry.getValue().getFulfillmentId()).toString();

            RestTemplateBuilder builder = new RestTemplateBuilder().rootUri(fulfillmentUrl);
            RestTemplate domainRestTemplate = builder.build();

            ResponseEntity response = null;

            long startTime = System.currentTimeMillis();
            int attempts = (domainRetryAttempts != null && Integer.parseInt(domainRetryAttempts) > 0) ? Integer.parseInt(domainRetryAttempts) : 1;
            while (response == null && attempts > 0) {
                try {
                    response = domainRestTemplate.exchange(uri, HttpMethod.GET, entity, IdDataObject.class);
                    DataDogOutboundRequestLog dataDogOutboundRequestLog = new DataDogOutboundRequestLog();
                    dataDogOutboundRequestLog.setCorrelationId(correlationId);
                    dataDogOutboundRequestLog.setURI(uri);
                    long elapsed = System.currentTimeMillis() - startTime;
                    dataDogOutboundRequestLog.setDuration(String.valueOf(elapsed));
                    dataDogOutboundRequestLog.setMethod("GET");
                    dataDogOutboundRequestLog.setFulfillmentId(input.get(entry.getKey()).getFulfillmentId());
                    dataDogOutboundRequestLog.setHeaders(entity.getHeaders().toSingleValueMap());
                    dataDogOutboundRequestLog.setResponseCode("200");
                    Gson objectMapper = new Gson();
                    System.out.println(objectMapper.toJson(dataDogOutboundRequestLog));
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(401))) {
                        accessToken = oauthService.getOauthToken();
                        headers.set("Authorization", "Bearer " + accessToken);
                        entity = new HttpEntity<>(headers);
                        attempts--;
                    } else {
                        logError("Error During Domain Call: Attempt " + ((Integer.parseInt(domainRetryAttempts) + 1) - attempts), e, startTime, correlationId);
                        if (attempts == 1) {                       
                            logError("Value Not processed calling domain service for Fulfillment id: " + entry.getValue().getFulfillmentId(), e, startTime, correlationId);
                        }
                        attempts--;
                    }
                }
            }
            if (response != null && response.getStatusCode() == HttpStatusCode.valueOf(200) && response.getBody() != null) {
                IdDataObject idDataObject = (IdDataObject) response.getBody();
                if(idDataObject.getData().getStatus().equalsIgnoreCase("PENDING")) {
                    FulfillmentObject fulfillmentObject1 = new FulfillmentObject();
                    fulfillmentObject1.setVersionKey(idDataObject.getData().getVersionKey());
                    fulfillmentObject1.setFulfillmentId(entry.getValue().getFulfillmentId());
                    fulfillmentObject1.setEffectiveDate(entry.getValue().getEffectiveDate());
                    output.put(entry.getKey(), fulfillmentObject1);
                } else {
                    numberOfCallsNotInPendingState++;
                    codesNotInPendingState.add(entry.getKey());
                }
            } else {
                codesNotInPendingState.add(entry.getKey());
            }
        }
        metricsLoggingObject.setNumberOfGetCallsNotInPendingState(String.valueOf(numberOfCallsNotInPendingState));
        metricsLoggingObject.setCodesNotInPendingState(codesNotInPendingState);
        return output;
    }

    private void logSuccess(String message, long startTime, String correlationId) {
        long elapsed = System.currentTimeMillis() - startTime;
        DataDogLoggingObject logObject = new DataDogLoggingObject( message, String.valueOf(elapsed), "200", null,correlationId);
        System.out.println(new Gson().toJson(logObject));
    }

    private void logError(String message, Exception e, long startTime, String correlationId) {
        long elapsed = System.currentTimeMillis() - startTime;
        DataDogLoggingObject logObject = new DataDogLoggingObject(  message + e.getMessage(),
                String.valueOf(elapsed), "500", e.getCause() != null ? getStackTraceAsString(e.getCause()) : null, correlationId);
        System.out.println(new Gson().toJson(logObject));
    }

    private void logNoResponse(String message, ResponseEntity response, String correlationId) {
        DataDogLoggingObject dataDogLoggingObject = new DataDogLoggingObject(message, null, response != null ? response.getStatusCode().toString() : null, null, correlationId);
        System.out.println(new Gson().toJson(dataDogLoggingObject));
    }
   
    

}
