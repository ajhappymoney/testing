package com.happymoney.productionobservability.helper;

import com.datadog.api.v2.client.model.Log;
import com.datadog.api.v2.client.model.LogAttributes;
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.adaptor.DatadogAdaptor;
import com.happymoney.productionobservability.service.DashboardService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
public class DashboardHelper {

    @Autowired
    private SortDataHelper sortDataHelper;

    @Autowired
    private DatadogAdaptor datadogAdaptor;

    Logger logger = LoggerFactory.getLogger(DashboardHelper.class);

    public JsonObject processDashboardData(OffsetDateTime fromDate, OffsetDateTime toDate, List<Log> queryRes, String requestName)
    {
        logger.info("requestName:"+requestName+ " Processing Dashboard data");
         Map<String,Integer> auto = sortDataHelper.getPriorityMap();
         HashMap<String, HashMap<String, Long>> recentEvents = new HashMap<String, HashMap<String, Long>>();

         JsonObject charData = new JsonObject();
         JsonObject members = new JsonObject();
         Set<String> allGuids = new HashSet<String>();

         if(!(queryRes==null || queryRes.size()==0)) {
             for (Log logmodel :
                     queryRes) {
                 HashMap<String, Object> logAttributesHashMap = new HashMap<String, Object>();
                 HashMap<String, Object> profileAttribute = new HashMap<String, Object>();

                 logAttributesHashMap = (HashMap<String, Object>) ((LogAttributes) logmodel.getAttributes()).getAttributes();
                 String leadGUid = (String) logAttributesHashMap.get("lead_guid");
                 if (!(leadGUid == null || leadGUid.length() == 0)) {
                     allGuids.add(leadGUid);
                     Long eventTime = (Long) logAttributesHashMap.get("timestamp");

                     profileAttribute = (HashMap<String, Object>) logAttributesHashMap.get("profile");

                     String page = (String) profileAttribute.get("page");

                     if (recentEvents.containsKey(page)) {
                         HashMap<String, Long> leadEvents = recentEvents.get(page);
                         if (recentEvents.get(page).containsKey(leadGUid)) {
                             if (recentEvents.get(page).get(leadGUid) < eventTime) {
                                 leadEvents.replace(leadGUid, eventTime);
                             }
                         } else {
                             leadEvents.put(leadGUid, eventTime);
                         }
                         recentEvents.replace(page, leadEvents);
                     } else {
                         recentEvents.put(page, new HashMap<String, Long>() {{
                             put(leadGUid, eventTime);
                         }});

                     }
                 }
             }

             if(allGuids!=null && allGuids.size()>0) {
                 String guids2 = "";
                 for (String guid :
                         allGuids) {
                     if (guid.length() < 10) continue;
                     guids2 += guid + " OR ";
                 }
                 guids2 = guids2.substring(0, guids2.length() - 4);
                 members = datadogAdaptor.getMemberId(fromDate, toDate, guids2, requestName);
             }else{
                 logger.info("No valid lead IDs found");
             }

             HashMap<Integer, HashMap<String, Object>> resHashMap = new HashMap<Integer, HashMap<String, Object>>();

             for (String key:recentEvents.keySet()
             ) {
                 HashMap<String, Object> eachPageEvents = new HashMap<String, Object>();

                 ArrayList<HashMap<String, Object>> resAttr = new ArrayList<HashMap<String, Object>>();
                 for (String id: recentEvents.get(key).keySet()
                 ) {
                     HashMap<String, Object> eachEvent = new HashMap<String, Object>();
                     eachEvent.put("eventTime", recentEvents.get(key).get(id));
                     eachEvent.put("lead_guid", id);
                     if(members!=null && members.has(id)){
                         eachEvent.put("member_id", members.get(id));
                     }else{
                         eachEvent.put("member_id", "");
                     }
                     resAttr.add(eachEvent);
                 }
                 eachPageEvents.put("count", resAttr.size());
                 eachPageEvents.put("page", key);
                 eachPageEvents.put("resultAttributes", resAttr);
                 resHashMap.put(auto.get(key), eachPageEvents);
             }
             charData = sortDataHelper.getSortedData(resHashMap);
             return charData;
         }
         return null;
     }

}
