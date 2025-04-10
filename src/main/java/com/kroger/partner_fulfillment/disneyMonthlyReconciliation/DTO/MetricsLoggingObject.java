package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO;

import java.util.List;

public class MetricsLoggingObject {

    private String numberOfGetCallsMade;
    private String numberOfGetCallsWithNoData;
    private String numberOfGetCallErrors;
    private String numberOfUpdateCalls;
    private String numberOfUpdateErrors;
    private String numberOfGetCallsNotInPendingState;
    private String outputMessage;
    private List<String> codesWithNoDataFound;
    private List<String> codesWithNoEffectiveDate;
    private List<String> codesNotInPendingState;

    public String getNumberOfGetCallsMade() {
        return numberOfGetCallsMade;
    }

    public void setNumberOfGetCallsMade(String numberOfGetCallsMade) {
        this.numberOfGetCallsMade = numberOfGetCallsMade;
    }

    public String getNumberOfGetCallsWithNoData() {
        return numberOfGetCallsWithNoData;
    }

    public void setNumberOfGetCallsWithNoData(String numberOfGetCallsWithNoData) {
        this.numberOfGetCallsWithNoData = numberOfGetCallsWithNoData;
    }

    public String getNumberOfGetCallErrors() {
        return numberOfGetCallErrors;
    }

    public void setNumberOfGetCallErrors(String numberOfGetCallErrors) {
        this.numberOfGetCallErrors = numberOfGetCallErrors;
    }

    public String getNumberOfUpdateCalls() {
        return numberOfUpdateCalls;
    }

    public void setNumberOfUpdateCalls(String numberOfUpdateCalls) {
        this.numberOfUpdateCalls = numberOfUpdateCalls;
    }

    public String getNumberOfUpdateErrors() {
        return numberOfUpdateErrors;
    }

    public void setNumberOfUpdateErrors(String numberOfUpdateErrors) {
        this.numberOfUpdateErrors = numberOfUpdateErrors;
    }

    public String getOutputMessage() {
        return outputMessage;
    }

    public void setOutputMessage(String outputMessage) {
        this.outputMessage = outputMessage;
    }

    public List<String> getCodesWithNoDataFound() {
        return codesWithNoDataFound;
    }

    public void setCodesWithNoDataFound(List<String> codesWithNoDataFound) {
        this.codesWithNoDataFound = codesWithNoDataFound;
    }

    public String getNumberOfGetCallsNotInPendingState() {
        return numberOfGetCallsNotInPendingState;
    }

    public void setNumberOfGetCallsNotInPendingState(String numberOfGetCallsNotInPendingState) {
        this.numberOfGetCallsNotInPendingState = numberOfGetCallsNotInPendingState;
    }

    public List<String> getCodesWithNoEffectiveDate() {
        return codesWithNoEffectiveDate;
    }

    public void setCodesWithNoEffectiveDate(List<String> codesWithNoEffectiveDate) {
        this.codesWithNoEffectiveDate = codesWithNoEffectiveDate;
    }

    public List<String> getCodesNotInPendingState() {
        return codesNotInPendingState;
    }

    public void setCodesNotInPendingState(List<String> codesNotInPendingState) {
        this.codesNotInPendingState = codesNotInPendingState;
    }
}
