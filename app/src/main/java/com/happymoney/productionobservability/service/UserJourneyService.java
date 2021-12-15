package com.happymoney.productionobservability.service;

import com.google.gson.JsonObject;
import com.happymoney.productionobservability.adaptor.DatadogAdaptor;
import com.happymoney.productionobservability.helper.UserJourneyHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;

@Service
public class UserJourneyService {

    @Autowired
    private DatadogAdaptor datadogAdaptor;

    @Autowired
    private UserJourneyHelper userJourneyHelper;

    public JSONObject getUserJourneyData(OffsetDateTime fromDate, OffsetDateTime toDate, String leadId, String requestName){
        try {
            StringBuffer leadsQuery = new StringBuffer("(");
            if(leadId.contains(",")){
                String[] leadGuids = leadId.split(",");

                for(int i=0 ; i<leadGuids.length;i++){
                    leadsQuery.append("@lead_guid:"+leadGuids[i]);
                    if(!(i==leadGuids.length-1)){
                        leadsQuery.append(" OR ");
                    }
                }
            }else{
                leadsQuery.append("@lead_guid:"+leadId);
            }
            leadsQuery.append(")");

            JsonObject datadogRes = datadogAdaptor.extractUserJourneyInfo(fromDate, toDate, leadsQuery, requestName);
            JSONObject userJourneyModelRes = userJourneyHelper.getUserJourneyModelAttributes(datadogRes, requestName);
            return userJourneyModelRes;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public JSONArray getDatadogDataTableEntries(String leadGUid, Boolean errorLog, OffsetDateTime fromDate, OffsetDateTime toDate, String requestName){
        JSONArray datadogRes = datadogAdaptor.getDatadogLogExplorerData(leadGUid, errorLog, fromDate, toDate, requestName);
        return datadogRes;
    }


}
