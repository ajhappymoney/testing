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
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.helper.SortDataHelper;
import com.happymoney.productionobservability.model.TableResponse;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

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
        Gson gson = new Gson();
        MultiKeyMap multiKeyMap = MultiKeyMap.multiKeyMap(new LinkedMap());


        JsonObject charData = new JsonObject();
        HashMap<Integer, HashMap<String, Object>> beforeSort = new HashMap<Integer, HashMap<String, Object>>();

        TableResponse tableResponse = new TableResponse();

        ApiClient datadogClient = datadogConnectionAdaptor.getDatadogApiClient();
        if(!(datadogClient==null)) {
            LogsApi apiInstance = new LogsApi(datadogClient);
            String filterQuery = "service:doppio-apply task_family:doppio-apply_prod (@profile.path:* AND lead_guid)";

            LogsSort sort = LogsSort.fromValue("timestamp"); // LogsSort | Order of logs in results.
            String pageCursor = ""; // String | List following results with a cursor provided in the previous query.
            Integer pageLimit = 5000; // Integer | Maximum number of logs in the response.
            try {
                LogsListResponse result = apiInstance.listLogsGet(new LogsApi.ListLogsGetOptionalParameters()
                        .filterQuery(filterQuery)
                        .filterFrom(fromDate)
                        .filterTo(toDate)
                        .sort(sort)
                        .pageLimit(pageLimit));

                for (Log logmodel :
                        result.getData()) {
                    HashMap<String, Object> logAttributesHashMap = new HashMap<String, Object>();
                    HashMap<String, Object> profileAttribute = new HashMap<String, Object>();

                    logAttributesHashMap = (HashMap<String, Object>) ((LogAttributes) logmodel.getAttributes()).getAttributes();
                    String leadGUid = (String) logAttributesHashMap.get("lead_guid");
                    Long eventTime = (Long) logAttributesHashMap.get("timestamp");

                    profileAttribute = (HashMap<String, Object>) logAttributesHashMap.get("profile");

                    String page = (String) profileAttribute.get("page");
                    Integer priorityNumber = auto.get(page);

                    HashMap<String, Object> profileInfo = new HashMap<>();
                    HashMap<String, Object> tempData = new HashMap<String, Object>();
                    tempData.put("lead_guid", leadGUid);
                    tempData.put("eventTime", eventTime);

                    HashMap<String, HashMap<String, Object>> tempHashMap = new HashMap<String, HashMap<String, Object>>();
                    if (!(leadGUid == null || leadGUid.length() == 0)) {
                        if (!beforeSort.containsKey(priorityNumber)) {
                            profileInfo.put("count", 1);

                            ArrayList<HashMap<String, Object>> storeData = new ArrayList<HashMap<String, Object>>();
                            storeData.add(tempData);

                            profileInfo.put("resultAttributes", storeData);
                            profileInfo.put("page", page);
                            beforeSort.put(priorityNumber, profileInfo);
                        } else {
                            ArrayList<HashMap<String, Object>> storeData = (ArrayList<HashMap<String, Object>>) (beforeSort.get(priorityNumber)).get("resultAttributes");
                            Integer counter = (Integer) (beforeSort.get(priorityNumber)).get("count");
                            storeData.add(tempData);
                            profileInfo = beforeSort.get(priorityNumber);
                            profileInfo.replace("count", counter + 1);
                            //                        profileInfo.replace("lead_guid", storeData);
                            profileInfo.replace("resultAttributes", storeData);
                            beforeSort.replace(priorityNumber, profileInfo);

                        }
                    }
                }
                charData = sortDataHelper.getSortedData(beforeSort);


            } catch (ApiException e) {
                System.err.println("Exception when calling LogsApi#listLogsGet");
                System.err.println("Status code: " + e.getCode());
                System.err.println("Reason: " + e.getResponseBody());
                System.err.println("Response headers: " + e.getResponseHeaders());
                e.printStackTrace();
            }
            return charData;
        }
        return null;
    }




    public Integer getMemberId(String guid){

        ApiClient datadogClient = datadogConnectionAdaptor.getDatadogApiClient();
        if(!(datadogClient==null)) {
            LogsApi apiInstance = new LogsApi(datadogClient);
            String filterQuery = "task_family:*_prod lead_guid member_id " + guid;

            OffsetDateTime filterFrom = OffsetDateTime.of(2021, 9, 7, 20, 15, 45, 345875000, ZoneOffset.of("+07:00")); // OffsetDateTime | Minimum timestamp for requested logs.
            OffsetDateTime filterTo = OffsetDateTime.now(); // OffsetDateTime | Maximum timestamp for requested logs.
            LogsSort sort = LogsSort.fromValue("timestamp"); // LogsSort | Order of logs in results.
            Integer pageLimit = 50; // Integer | Maximum number of logs in the response.
            try {
                LogsListResponse result = apiInstance.listLogsGet(new LogsApi.ListLogsGetOptionalParameters()
                        .filterQuery(filterQuery)
                        .filterFrom(filterFrom)
                        .filterTo(filterTo)
                        .sort(sort)
                        .pageLimit(pageLimit));
                String response = result.getData().get(0).toString();
                Pattern pattern3 = Pattern.compile("member_id(\\p{Punct})*(\\p{Space})*(\\d{3,11})(\\p{Space})*(\\p{Punct})?");
                Matcher matcher3 = pattern3.matcher(response);
                String match = "";
                while (matcher3.find()) {
                    match = matcher3.group();
                }
                int memberId = Integer.parseInt(match.replaceAll("[\\D]", ""));
                return memberId;
            } catch (ApiException e) {
                System.err.println("Exception when calling LogsApi#listLogsGet");
                System.err.println("Status code: " + e.getCode());
                System.err.println("Reason: " + e.getResponseBody());
                System.err.println("Response headers: " + e.getResponseHeaders());
                e.printStackTrace();
            }
        }
        return null;
    }

}
