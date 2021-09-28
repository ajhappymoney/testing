package com.happymoney.productionobservability.adaptor;

import com.happymoney.productionobservability.helper.FullStoryHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@Component
@EnableConfigurationProperties
public class FullStoryAdapter {

    @Autowired
    private FullStoryHelper fullStoryHelper;
    @Autowired
    private DatadogAdaptor datadogAdaptor;

    public String getSession(String leadGuid, String memberId, String fromDate, String toDate){
        try{
            //DatadogAdaptor dd = new DatadogAdaptor();
            if(!(memberId == null || memberId.trim().length() == 0)){

                Long fromEPoch = Long.parseLong(fromDate);
                OffsetDateTime fromOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(fromEPoch).toInstant(), ZoneOffset.UTC);

                Long toEPoch = Long.parseLong(toDate);
                OffsetDateTime toOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(toEPoch).toInstant(), ZoneOffset.UTC);

                memberId = Integer.toString(datadogAdaptor.getMemberIdValue(fromOffsetDateTime, toOffsetDateTime, leadGuid));
            }

            ResponseEntity<Object> status = exchangeRest("/api/v1/sessions?uid=" + memberId);
            ArrayList<LinkedHashMap> sessions = (ArrayList<LinkedHashMap>)status.getBody();
            return (((LinkedHashMap) sessions.get(0)).get("FsUrl")).toString();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private ResponseEntity<Object> exchangeRest(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", fullStoryHelper.getFullstoryToken());
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        String leadsHost = "https://www.fullstory.com";
        return restTemplate.exchange(leadsHost + endpoint, HttpMethod.GET, entity, Object.class);
    }
}
