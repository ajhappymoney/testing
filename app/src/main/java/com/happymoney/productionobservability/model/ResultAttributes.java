package com.happymoney.productionobservability.model;

public class ResultAttributes {
    private String leadGuid;
    private long eventTime;

    public String getLeadGuid() {
        return leadGuid;
    }

    public void setLeadGuid(String leadGuid) {
        this.leadGuid = leadGuid;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public ResultAttributes(String leadGuid, long eventTime) {
        this.leadGuid = leadGuid;
        this.eventTime = eventTime;
    }

    public ResultAttributes() {
    }

    @Override
    public String toString() {
        return "ResultAttributes{" +
                "leadGuid='" + leadGuid + '\'' +
                ", eventTime=" + eventTime +
                '}';
    }
}
