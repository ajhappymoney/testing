package com.happymoney.productionobservability.service;

import com.datadog.api.v2.client.model.Log;
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.adaptor.DatadogAdaptor;
import com.happymoney.productionobservability.adaptor.FullStoryAdapter;
import com.happymoney.productionobservability.controller.DashboardController;
import com.happymoney.productionobservability.helper.DashboardHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Autowired
    private DashboardHelper dashboardHelper;

    Logger logger = LoggerFactory.getLogger(DashboardService.class);

    public JsonObject getPageCount(OffsetDateTime fromDate, OffsetDateTime toDate, Boolean newCustomers) {
        List<Log> datadogRes = datadogAdaptor.getDatadogLogData(fromDate, toDate, newCustomers);
        return dashboardHelper.processDashboardData(fromDate, toDate, datadogRes);
    }

    public String getFullStoryLink(String leadGUid, String memberId, String fromDate, String toDate){
//        FullStoryAdapter fullStoryAdapter = new FullStoryAdapter();

        String sessionString = fullStoryAdapter.getSession(leadGUid, memberId, fromDate, toDate);

        return sessionString;
    }

    public String getDatadogLink(String leadGUid, Boolean errorLog, String fromDate, String toDate){
        StringBuffer datalogLink = new StringBuffer("https://app.datadoghq.com/logs?query=");
        datalogLink.append(leadGUid);
        if(errorLog){
            datalogLink.append("%20error");
        }
        Long fromEpoch = Long.parseLong(fromDate);
        Long toEpoch = Long.parseLong(toDate);
        datalogLink.append("&from_ts="+fromEpoch+"&to_ts="+toEpoch+"&live=false");
        return datalogLink.toString();
    }

}
