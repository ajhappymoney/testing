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
import com.happymoney.productionobservability.service.DashboardService;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
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

    Logger logger = LoggerFactory.getLogger(DatadogAdaptor.class);

    public JsonObject getDatadogLogData(OffsetDateTime fromDate, OffsetDateTime toDate){

        logger.info("Extracting Loan journey data from datadog between from = "+fromDate+" to = "+toDate);

        Map<String,Integer> auto = sortDataHelper.getPriorityMap();
        HashMap<String, HashMap<String, Long>> recentEvents = new HashMap<String, HashMap<String, Long>>();

        JsonObject charData = new JsonObject();

        String filterQuery = "service:doppio-apply task_family:doppio-apply_prod (@profile.path:* AND lead_guid)";
        Integer pageLimit = 5000;
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.

        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromDate, toDate, pageLimit, pageCursor);
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


    public Integer getMemberIdValue(OffsetDateTime fromDate, OffsetDateTime toDate, String guid){

        String filterQuery = "task_family:*_prod lead_guid member_id " + guid;
        Integer pageLimit = 50;
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.

        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromDate.minusDays(7), toDate, pageLimit, pageCursor);
        if(!(queryRes==null || queryRes.size()==0)) {
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

        logger.info("requestName:"+requestName+" Extracting memeber id");
        JsonObject resJsonObject = new JsonObject();
        String filterQuery = "service:doppio-apply task_family:*prod @scrubbedMsgLogCopy.member_id:* (" + guids + ")";
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.
        Integer pageLimit = 5000; // Integer | Maximum number of logs in the response.
        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromDate.minusDays(7), toDate, pageLimit, pageCursor);
        if(!(queryRes==null || queryRes.size()==0)) {
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
        HashMap<String, HashMap<String, ArrayList<Long>>> recentEvents = new HashMap<String, HashMap<String, ArrayList<Long>>>();
        String filterQuery = "service:doppio-apply task_family:doppio-apply_prod (@profile.path:* AND "+leadId.toString()+")";
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.
        Integer pageLimit = 5000; // Integer | Maximum number of logs in the response.
        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromOffsetDateTime, toOffsetDateTime, pageLimit, pageCursor, false, requestName);
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
                    HashMap<String, ArrayList<Long>> leadJourney = recentEvents.get(leadGUid);
                    if (leadJourney.containsKey(page)) {
                        leadJourney.get(page).add(eventTime);
                    } else {
                        leadJourney.put(page, new ArrayList<Long>(Collections.singleton(eventTime)));
                    }
                    recentEvents.replace(leadGUid, leadJourney);
                }else {
                    HashMap<String, ArrayList<Long>> leadJourney = new HashMap<String, ArrayList<Long>>();
                    leadJourney.put(page, new ArrayList<Long>(Collections.singleton(eventTime)));
                    recentEvents.put(leadGUid, leadJourney);
                }
            }
        }
        Gson gson = new Gson();
        String json = gson.toJson(recentEvents);
        JsonObject resJsonObject = JsonParser.parseString(json).getAsJsonObject();
        return resJsonObject;
    }

    public HashMap<String, HashMap<String, Long>> getNormalCheckLogData(OffsetDateTime fromDate, OffsetDateTime toDate, String requestName) {

        Map<String, Integer> auto = sortDataHelper.getPriorityMap();
        HashMap<String, HashMap<String, Long>> recentEvents = new HashMap<String, HashMap<String, Long>>();

        JsonObject charData = new JsonObject();

        String filterQuery = "service:doppio-apply task_family:doppio-apply_prod (@profile.path:* AND lead_guid)";
        Integer pageLimit = 5000;
        String pageCursor = ""; // String | List following results with a cursor provided in the previous query.

        List<Log> queryRes = this.getDatadogResultData(filterQuery, fromDate, toDate, pageLimit, pageCursor, true, requestName);

        if(!(queryRes==null || queryRes.size()==0)) {
            for (Log logmodel :
                    queryRes) {
                HashMap<String, Object> logAttributesHashMap = new HashMap<String, Object>();
                HashMap<String, Object> profileAttribute = new HashMap<String, Object>();

                logAttributesHashMap = (HashMap<String, Object>) ((LogAttributes) logmodel.getAttributes()).getAttributes();
                String leadGUid = (String) logAttributesHashMap.get("lead_guid");
                if (!(leadGUid == null || leadGUid.length() == 0)) {
                    Long eventTime = (Long) logAttributesHashMap.get("timestamp");

                    profileAttribute = (HashMap<String, Object>) logAttributesHashMap.get("profile");

                    String page = (String) profileAttribute.get("page");

                    if (recentEvents.containsKey(leadGUid)) {
                        HashMap<String, Long> leadEvents = recentEvents.get(leadGUid);
                        if (recentEvents.get(leadGUid).containsKey(page)) {
                            if (recentEvents.get(leadGUid).get(page) < eventTime) {
                                leadEvents.replace(page, eventTime);
                            }
                        } else {
                            leadEvents.put(page, eventTime);
                        }
                        recentEvents.replace(leadGUid, leadEvents);
                    } else {
                        recentEvents.put(leadGUid, new HashMap<String, Long>() {{
                            put(page, eventTime);
                        }});

                    }
                }
            }
        }
        return recentEvents;
    }
    private List<Log> getDatadogResultData(String filterQuery, OffsetDateTime fromDate, OffsetDateTime toDate, Integer pageLimit, String pageCursor, Boolean pagination, String requestName ){
        try{
            ApiClient datadogClient = datadogConnectionAdaptor.getDatadogApiClient();
            if(!(datadogClient==null)) {
                LogsApi apiInstance = new LogsApi(datadogClient);
                LogsSort sort = LogsSort.fromValue("timestamp"); // LogsSort | Order of logs in results.
                logger.info("Calling listLogsGet api call filterFrom:"+fromDate+
                        " filterTo:"+toDate+" sort:"+sort.toString()+" pageLimit:"+pageLimit);
                try {
                    LogsListResponse result = apiInstance.listLogsGet(new LogsApi.ListLogsGetOptionalParameters()
                            .filterQuery(filterQuery)
                            .filterFrom(fromDate)
                            .filterTo(toDate)
                            .sort(sort)
                            .pageLimit(pageLimit));
                    finalList = result.getData();
                    if(pagination) {
                        Integer count = 1;
                        String afterPage = "";
                        if (result.getMeta() != null) {
                            afterPage = result.getMeta().getPage().getAfter();
                        }

                        while (!afterPage.isEmpty()) {
                            count+=1;
                            LogsListResponse tempRes = apiInstance.listLogsGet(new LogsApi.ListLogsGetOptionalParameters()
                                    .filterQuery(filterQuery)
                                    .filterFrom(fromDate)
                                    .filterTo(toDate)
                                    .sort(sort)
                                    .pageLimit(pageLimit).pageCursor(afterPage));
                            if (tempRes.getData().size() > 0) {
                                finalList.addAll(tempRes.getData());
                                afterPage = tempRes.getMeta().getPage().getAfter();
                            } else {
                                afterPage = "";
                            }
                        }
                        logger.info("requestName:"+requestName+" Number of datadog api calls(pagination):"+ count);
                    }
                    return finalList;

                } catch (ApiException e) {
                    logger.error("Exception when calling LogsApi#listLogsGet, Status code:"+e.getCode()+" reason:"+e.getResponseBody());
                }
            }else{
                logger.info("Unable to setup datadog client");
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }
        return null;
    }
}