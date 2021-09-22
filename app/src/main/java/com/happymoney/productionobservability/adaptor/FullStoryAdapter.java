package com.happymoney.productionobservability.adaptor;

import com.happymoney.productionobservability.helper.FullStoryHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@Component
@EnableConfigurationProperties
public class FullStoryAdapter {

    @Autowired
    private FullStoryHelper fullStoryHelper;
    @Autowired
    private DatadogAdaptor dd;

    public String getSession(String guid){
        try{
            //DatadogAdaptor dd = new DatadogAdaptor();
            int memberId = dd.getMemberId(guid);
            ResponseEntity<Object> status = exchangeRest("/api/v1/sessions?uid=" + memberId);
            ArrayList<LinkedHashMap> sessions = (ArrayList<LinkedHashMap>)status.getBody();
//            System.out.println((((LinkedHashMap) sessions.get(0)).get("FsUrl")).toString());
//            System.out.println("LinkedHashMap = " + sessions.toString());
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
