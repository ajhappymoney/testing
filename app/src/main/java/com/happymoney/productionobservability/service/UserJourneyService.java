package com.happymoney.productionobservability.service;

import com.google.gson.JsonObject;
import com.happymoney.productionobservability.adaptor.DatadogAdaptor;
import org.springframework.beans.factory.annotation.Autowired;
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

    public JsonObject getUserJourneyData(String fromDate, String toDate, String leadId){
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
            System.out.println("leadsQuery = " + leadsQuery + ", leadId = " + leadId);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
            Date fromparsedDate = dateFormat.parse(fromDate.replace("T", " ") + ":00:000");
            Date toparsedDate = dateFormat.parse(toDate.replace("T", " ") + ":00:000");
            Timestamp fromtimestamp = new java.sql.Timestamp(fromparsedDate.getTime());
            Timestamp totimestamp = new java.sql.Timestamp(toparsedDate.getTime());


            OffsetDateTime fromOffsetDateTime = OffsetDateTime.ofInstant(fromtimestamp.toInstant(), ZoneOffset.UTC);

            OffsetDateTime toOffsetDateTime = OffsetDateTime.ofInstant(totimestamp.toInstant(), ZoneOffset.UTC);

            return datadogAdaptor.extractUserJourneyInfo(fromOffsetDateTime, toOffsetDateTime, leadsQuery);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }
}
