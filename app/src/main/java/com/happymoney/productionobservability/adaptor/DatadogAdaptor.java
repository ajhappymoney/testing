package com.happymoney.productionobservability.adaptor;

import com.datadog.api.v2.client.ApiClient;
import com.datadog.api.v2.client.ApiException;
import com.datadog.api.v2.client.Configuration;
import com.datadog.api.v2.client.api.LogsApi;
import com.datadog.api.v2.client.model.Log;
import com.datadog.api.v2.client.model.LogAttributes;
import com.datadog.api.v2.client.model.LogsListResponse;
import com.datadog.api.v2.client.model.LogsSort;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.happymoney.productionobservability.helper.SortDataHelper;
import com.happymoney.productionobservability.model.TableResponse;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@EnableConfigurationProperties
public class DatadogAdaptor {
    @Autowired
    private DatadogConnectionAdaptor datadogConnectionAdaptor;

    @Autowired
    private SortDataHelper sortDataHelper;

    public JsonObject getDatadogLogData(OffsetDateTime fromDate, OffsetDateTime toDate){

        Map<String,Integer> auto = sortDataHelper.getPriorityMap();
        HashMap<String, HashMap<String, Long>> recentEvents = new HashMap<String, HashMap<String, Long>>();

        JsonObject charData = new JsonObject();

        String filterQuery = "service:doppio-apply task_family:doppio-apply_prod (@profile.path:* AND lead_guid)";
        Integer pageLimit = 5000;
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.

        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromDate, toDate, pageLimit, pageCursor);
        Set<String> allGuids = new HashSet<String>();
        if(!(queryRes==null)) {
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

                String guids2 = "";
                for (String guid :
                        allGuids) {
                    if(guid.length()<10) continue;
                    guids2 += guid + " OR ";
                }
                guids2 = guids2.substring(0, guids2.length() - 4);
                JsonObject members = getMemberId(fromDate, toDate, guids2);

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
                        if(members.has(id)){
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


    public Integer getMemberIdValue(OffsetDateTime fromDate, OffsetDateTime toDate, String guid){

        String filterQuery = "task_family:*_prod lead_guid member_id " + guid;
        Integer pageLimit = 50;
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.

        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromDate.minusDays(7), toDate, pageLimit, pageCursor);
        if(!(queryRes==null)) {
            String response = queryRes.get(0).toString();
            Pattern pattern3 = Pattern.compile("member_id(\\p{Punct})*(\\p{Space})*(\\d{3,11})(\\p{Space})*(\\p{Punct})?");
            Matcher matcher3 = pattern3.matcher(response);
            String match = "";
            while (matcher3.find()) {
                match = matcher3.group();
            }
            int memberId = Integer.parseInt(match.replaceAll("[\\D]", ""));
            return memberId;
        }
        return null;
    }

    public JsonObject getMemberId(OffsetDateTime fromDate, OffsetDateTime toDate, String guids){

        JsonObject resJsonObject = new JsonObject();
        String filterQuery = "service:doppio-apply task_family:*prod @scrubbedMsgLogCopy.member_id:* (" + guids + ")";
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.
        Integer pageLimit = 5000; // Integer | Maximum number of logs in the response.
        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromDate.minusDays(7), toDate, pageLimit, pageCursor);
        if(!(queryRes==null)) {
            for (Log logmodel :
                    queryRes) {
                HashMap<String, Object> logAttributesHashMap = new HashMap<String, Object>();
                logAttributesHashMap = (HashMap<String, Object>) ((LogAttributes) logmodel.getAttributes()).getAttributes().get("scrubbedMsgLogCopy");
                String leadGUid = (String) logAttributesHashMap.get("lead_guid");
                String memberId = logAttributesHashMap.get("member_id").toString();
                if (leadGUid == null || memberId == null || resJsonObject.has(leadGUid)) continue;
                resJsonObject.addProperty(leadGUid, memberId);

            }
            return resJsonObject;
        }
        return null;
    }

    public JsonObject extractUserJourneyInfo(OffsetDateTime fromOffsetDateTime, OffsetDateTime toOffsetDateTime, StringBuffer leadId){
        HashMap<String, HashMap<String, Long>> recentEvents = new HashMap<String, HashMap<String, Long>>();
        String filterQuery = "service:doppio-apply task_family:doppio-apply_prod (@profile.path:* AND "+leadId.toString()+")";
        System.out.println("filterQuery ="  + filterQuery);
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.
        Integer pageLimit = 5000; // Integer | Maximum number of logs in the response.
        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromOffsetDateTime, toOffsetDateTime, pageLimit, pageCursor);
//        System.out.println("queryRes "+queryRes);
        if(!(queryRes==null)) {
            for (Log logmodel :
                    queryRes) {
                HashMap<String, Object> logAttributesHashMap = new HashMap<String, Object>();
                HashMap<String, Object> profileAttribute = new HashMap<String, Object>();

                logAttributesHashMap = (HashMap<String, Object>) ((LogAttributes) logmodel.getAttributes()).getAttributes();
                String leadGUid = (String) logAttributesHashMap.get("lead_guid");

                Long eventTime = (Long) logAttributesHashMap.get("timestamp");

                profileAttribute = (HashMap<String, Object>) logAttributesHashMap.get("profile");

                String page = (String) profileAttribute.get("page");

                if (recentEvents.containsKey(leadGUid)) {
                    HashMap<String, Long> leadJourney = recentEvents.get(leadGUid);
                    if (leadJourney.containsKey(page)) {
                        if (leadJourney.get(page) > eventTime) {
                            leadJourney.replace(page, eventTime);
                        }
                    } else {
                        leadJourney.put(page, eventTime);
                    }
                    recentEvents.replace(leadGUid, leadJourney);
                }else {
                    HashMap<String, Long> leadJourney = new HashMap<String, Long>();
                    leadJourney.put(page, eventTime);
                    recentEvents.put(leadGUid, leadJourney);
                }



            }
        }
        Gson gson = new Gson();
        System.out.println("recentEvents "+recentEvents.toString());
        String json = gson.toJson(recentEvents);
        JsonObject resJsonObject = JsonParser.parseString(json).getAsJsonObject();
        return resJsonObject;
    }

    private List<Log> getDatadogResultData(String filterQuery, OffsetDateTime fromDate, OffsetDateTime toDate, Integer pageLimit, String pageCursor ){
        try{

            ApiClient datadogClient = datadogConnectionAdaptor.getDatadogApiClient();
            if(!(datadogClient==null)) {
                LogsApi apiInstance = new LogsApi(datadogClient);
                LogsSort sort = LogsSort.fromValue("timestamp"); // LogsSort | Order of logs in results.
                try {
                    LogsListResponse result = apiInstance.listLogsGet(new LogsApi.ListLogsGetOptionalParameters()
                            .filterQuery(filterQuery)
                            .filterFrom(fromDate)
                            .filterTo(toDate)
                            .sort(sort)
                            .pageLimit(pageLimit));
                    return result.getData();
                } catch (ApiException e) {
                    System.err.println("Exception when calling LogsApi#listLogsGet");
                    System.err.println("Status code: " + e.getCode());
                    System.err.println("Reason: " + e.getResponseBody());
                    System.err.println("Response headers: " + e.getResponseHeaders());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
        return null;
    }
}