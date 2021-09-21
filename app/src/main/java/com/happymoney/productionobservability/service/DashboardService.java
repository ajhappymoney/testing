package com.happymoney.productionobservability.service;

import com.google.gson.JsonObject;
import com.happymoney.productionobservability.adaptor.DatadogAdaptor;
import com.happymoney.productionobservability.adaptor.FullStoryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DashboardService
{
    @Autowired
    private DatadogAdaptor datadogAdaptor;

    @Autowired
    private FullStoryAdapter fullStoryAdapter;

    public JsonObject getPageCount(OffsetDateTime fromDate, OffsetDateTime toDate) {
//        DatadogAdaptor datadogAdaptor = new DatadogAdaptor();
        return datadogAdaptor.getDatadogLogData(fromDate, toDate);
    }

    public String getFullStoryLink(String leadGUid){
//        FullStoryAdapter fullStoryAdapter = new FullStoryAdapter();
        String sessionString = fullStoryAdapter.getSession(leadGUid);

        return sessionString;
    }

    public String getDatadogLink(String leadGUid){
        StringBuffer datalogLink = new StringBuffer("https://app.datadoghq.com/logs?query=");
        datalogLink.append(leadGUid);
        Date date = new Date();
        Timestamp timestampfrom = new Timestamp(date.getTime());
//        datalogLink.append("&from_ts="+timestampfrom+"&live=true");
        return datalogLink.toString();
    }

}
