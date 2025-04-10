package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.controller;

import com.google.gson.Gson;
import com.google.common.collect.Lists;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.CSVDataObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DataDogLoggingObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.FulfillmentObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.MetricsLoggingObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service.FileService;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service.FileServiceImpl;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service.OauthService;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service.OauthServiceImpl;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service.PartnerFulfillmentDomainService;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service.PartnerFulfillmentDomainServiceImpl;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service.PartnerFulfillmentOrchestratorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
class EventController {

    FileService fileService;
    OauthService oauthService;
    PartnerFulfillmentDomainService partnerFulfillmentDomainService;
    PartnerFulfillmentOrchestratorService partnerFulfillmentOrchestratorService;

    @Value("${batch.size}")
    private String batchSize;

    public EventController(FileServiceImpl fileRetrievalService,
                           OauthServiceImpl oauthService,
                           PartnerFulfillmentDomainServiceImpl partnerFulfillmentDomainService,
                           PartnerFulfillmentOrchestratorService partnerFulfillmentOrchestratorService) {
        this.fileService = fileRetrievalService;
        this.oauthService = oauthService;
        this.partnerFulfillmentDomainService = partnerFulfillmentDomainService;
        this.partnerFulfillmentOrchestratorService = partnerFulfillmentOrchestratorService;
    }

    @RequestMapping("/execute")
    public void executeFile() throws IOException, InterruptedException {
        processFile();
    }

    @RequestMapping("/executeResponder")
    public void executeResponderFile(@RequestParam(value = "fileName", required = true) String fileNameInput) throws IOException, InterruptedException {
        processResponderFile(fileNameInput);
    }

    @RequestMapping("/retrieveReconciliation")
    public void retrieveReconciliationFile(@RequestParam(value = "fileName", required = true) String fileNameInput) throws IOException, JSchException, SftpException {
        fileService.copyFileToSFTPLocal(fileNameInput);
    }

    @RequestMapping("/retrieveHuluResponder")
    public void retrieveResponderFile(@RequestParam(value = "fileName", required = true) String fileNameInput) throws IOException, JSchException, SftpException {
        fileService.copyResponderFileToSFTPLocal(fileNameInput);
    }

    @RequestMapping("/listFiles")
    public Map<String, String> getListOfFiles() {
        return fileService.getListOfFiles();
    }

    @RequestMapping("/listResponderFiles")
    public Map<String, String> getListOfFilesInResponderDirectory() {
        return fileService.getListOfFilesInResponderDirectory();
    }

    @RequestMapping("/getDirectoryStructure")
    public List<String> getFileStructure() throws Exception {
        return fileService.getFileStructure();
    }

    //Turning off Cron Job until after go live.  This will be turned on after the shakedown period.
    @Scheduled(cron = "#{@cronBean}", zone = "America/New_York")
    public void processFile() throws IOException, InterruptedException {
        //Get Access Token
        String accessToken = oauthService.getOauthToken();

        //Hit API to Retrieve all details
        partnerFulfillmentDomainService.writeFulfillmentsToFile(accessToken, oauthService);

        String epochTime = String.valueOf(Instant.now().getEpochSecond());

        //Write file to remote location
        fileService.writeFileToSFTPLocation(epochTime);

        //Write copy of file to local location if not already there
        fileService.writeFileToSFTPLocalLocation(epochTime);

        //remove file from container
        fileService.deleteFileAfterUse();
    }

    //Turning off Cron Job until after go live.  This will be turned on after the shakedown period.
    @Scheduled(cron = "#{@cronBeanResponder}", zone = "America/New_York")
    public void executeResponderFileCron() throws IOException, InterruptedException {
        String filename = "kroger_yearmonth.csv";
        LocalDate previousMonthDate = LocalDate.now().minusMonths(1);
        String previousMonth = previousMonthDate.format(DateTimeFormatter.ofPattern("MM"));
        String previousYear = previousMonthDate.format(DateTimeFormatter.ofPattern("yyyy"));
        filename = filename.replace("year", previousYear);
        filename = filename.replace("month", previousMonth);
        processResponderFile(filename);
    }


    private void processResponderFile(String fileName) throws IOException, InterruptedException {
        MetricsLoggingObject metricsLoggingObject = new MetricsLoggingObject();
        List<CSVDataObject> records = fileService.processFile(fileName);

        if(batchSize == null) {
            batchSize = records.size() > 0 ? String.valueOf(records.size()) : "1";
        }
        int batch = Integer.parseInt(batchSize);
        if(records != null) {
            List<List<CSVDataObject>> partitionedRecords = Lists.partition(records, batch);
            List<CompletableFuture<Void>> processedRecords = Lists.newArrayList();
            for (List<CSVDataObject> record : partitionedRecords) {
                processedRecords.add(CompletableFuture.runAsync(() -> {
                    String accessToken = null;
                    try {
                        accessToken = oauthService.getOauthToken();
                        Map<String, FulfillmentObject> fulfillmentIds = null;
                        fulfillmentIds = partnerFulfillmentDomainService.retrieveFulfillmentIds(record, accessToken, oauthService, metricsLoggingObject);
                        System.out.println("SIZE of FULFILLMENT IDs After first get Call: " + fulfillmentIds.size());
                        System.out.println("_____________________________________________");
                        for (Map.Entry<String, FulfillmentObject> entry : fulfillmentIds.entrySet()) {
                            System.out.println(entry.getKey());
                        }
                        System.out.println("_____________________________________________");
                        fulfillmentIds = partnerFulfillmentDomainService.retrieveVersionKeys(fulfillmentIds, accessToken, oauthService, metricsLoggingObject);
                        partnerFulfillmentOrchestratorService.markFulfillmentAsComplete(fulfillmentIds, accessToken, oauthService, metricsLoggingObject);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            System.out.println("Number of async requests: " + processedRecords.size());
            long startTime = System.currentTimeMillis();
            for (CompletableFuture<Void> processedRecord : processedRecords) {
                while (!processedRecord.isDone()) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    Gson objectMapper = new Gson();
                    DataDogLoggingObject dataDogLoggingObject2 = new DataDogLoggingObject("Processing Request", String.valueOf(elapsed), null, null);
                    System.out.println(objectMapper.toJson(dataDogLoggingObject2));
                    TimeUnit.SECONDS.sleep(1);
                }
            }
            fileService.deleteFileAfterUse();
            Gson objectMapper = new Gson();
            System.out.println(objectMapper.toJson(metricsLoggingObject));
        }
    }

}
