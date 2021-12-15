package com.happymoney.productionobservability.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.controller.NormalJourneyController;
import ognl.BooleanExpression;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.util.*;

@Component
public class UserJourneyHelper {

    @Autowired
    private SortDataHelper sortDataHelper;

    Logger logger = LoggerFactory.getLogger(UserJourneyHelper.class);

    public JSONObject getUserJourneyModelAttributes(JsonObject userJourneyResult, String requestName){
        JSONObject userJourneyResObject = new JSONObject();
        JSONArray seriesArray = new JSONArray();
        JSONArray UserJourneySeriesArray = new JSONArray();
        JSONObject funnelPageValues = new JSONObject();

        List<String> funnelPage = new ArrayList<String>();

        Map<String,Integer> auto = sortDataHelper.getPriorityMap();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(auto.entrySet());
        list.sort(Map.Entry.comparingByValue());

        for (Map.Entry<String, Integer> entry : list) {
            funnelPage.add(entry.getKey());
        }

        Set<Map.Entry<String, JsonElement>> entrySet = userJourneyResult.entrySet();

        for(Map.Entry<String,JsonElement> entry : entrySet){
            JSONObject seriesData = new JSONObject();
            JSONObject userJourneySeriesData = new JSONObject();
            JsonObject keyvalue = userJourneyResult.getAsJsonObject(entry.getKey());
            seriesData.put("name", entry.getKey());
            userJourneySeriesData.put("name", entry.getKey());
            HashMap<Long, String> sampleObj = new HashMap<Long, String>();
            for (String key: keyvalue.keySet()
            ) {
                JsonArray eventsArray = keyvalue.getAsJsonArray(key);
                for (JsonElement eventTime: eventsArray
                ) {
                    sampleObj.put(eventTime.getAsLong(), key);
                }
            }

            TreeMap<Long, String> sorted = new TreeMap<>();

            // Copy all data from hashMap into TreeMap
            sorted.putAll(sampleObj);
            ArrayList<ArrayList<Object>> sample = new ArrayList<ArrayList<Object>>();
            ArrayList<ArrayList<Object>> userJourneySample = new ArrayList<ArrayList<Object>>();
            // Display the TreeMap which is naturally sorted
            Long previousEventTime = 0L;
            JSONObject funnelPageNames = new JSONObject();
            for (Map.Entry<Long, String> entrytest : sorted.entrySet()){
                ArrayList<Object> temp = new ArrayList<Object>();
                ArrayList<Object> userJourneyTemp = new ArrayList<Object>();
                if(previousEventTime.equals(0L)){
                    previousEventTime = entrytest.getKey();
                }

                temp.add(entrytest.getKey());
                userJourneyTemp.add(entrytest.getKey());
                userJourneyTemp.add(funnelPage.indexOf(entrytest.getValue()));

                Long eventDiff = (entrytest.getKey() - previousEventTime) / 1000;
                temp.add(eventDiff);

                if(funnelPageNames.containsKey(eventDiff)){
                    JSONObject funnelData = (JSONObject) funnelPageNames.get(eventDiff);
                    JSONObject pageInfoObj = new JSONObject();
                    pageInfoObj.put("pageName", entrytest.getValue());
                    pageInfoObj.put("previousTimeStamp", previousEventTime);

//                    funnelData.putIfAbsent(entrytest.getKey(), entrytest.getValue());
                    funnelData.putIfAbsent(entrytest.getKey(), pageInfoObj);
                    funnelPageNames.put(eventDiff, funnelData);
                }else{
                    JSONObject pageInfo = new JSONObject();
                    JSONObject pageInfoObj = new JSONObject();
                    pageInfoObj.put("pageName", entrytest.getValue());
                    pageInfoObj.put("previousTimeStamp", previousEventTime);
//                    pageInfo.put(entrytest.getKey(), entrytest.getValue());
                    pageInfo.put(entrytest.getKey(), pageInfoObj);
                    funnelPageNames.put(eventDiff, pageInfo);
                }

                previousEventTime = entrytest.getKey();

                sample.add(temp);
                userJourneySample.add(userJourneyTemp);
            }

            funnelPageValues.put(entry.getKey(),funnelPageNames );

            Random obj = new Random();
            int randNum = obj.nextInt(0xffffff + 1);
            String colorCode = String.format("#%06x", randNum);

            Random intervalObj = new Random();
            int intervalRandNum = intervalObj.nextInt(0xffffff + 1);
            String intervalColorCode = String.format("#%06x", intervalRandNum);

            seriesData.put("data", sample);
            seriesData.put("type","scatter");
            seriesData.put("lineWidth", 1);
            seriesData.put("color", colorCode);

            userJourneySeriesData.put("data", userJourneySample);
            userJourneySeriesData.put("type","scatter");
            userJourneySeriesData.put("lineWidth", 1);
            userJourneySeriesData.put("color", intervalColorCode);

            seriesArray.add(seriesData);
            UserJourneySeriesArray.add(userJourneySeriesData);

        }
        userJourneyResObject.put("seriesArray",seriesArray);
        userJourneyResObject.put("userJourneySeriesData",UserJourneySeriesArray);
        userJourneyResObject.put("funnelPage",funnelPage);
        userJourneyResObject.put("funnelPageValues", funnelPageValues);
        return userJourneyResObject;
    }

    public JSONObject getNormalJourneyModelAttributes(HashMap<String, HashMap<String, Long>> datadogLogData, String requestName) {

        JSONObject normalJourneyResObject = new JSONObject();
        JSONArray tableJsonDataArray = new JSONArray();

        HashMap<String, HashMap<String, HashMap<String, Object>>> newtable = new HashMap<String, HashMap<String, HashMap<String, Object>>>();

        HashMap<String, Integer> funnelLeadsCounter = new HashMap<String, Integer>();
        List<String> funnelPage = new ArrayList<String>();

        Map<String,Integer> auto = sortDataHelper.getPriorityMap();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(auto.entrySet());
        list.sort(Map.Entry.comparingByValue());

        for (Map.Entry<String, Integer> entry : list) {
            funnelPage.add(entry.getKey());
        }

        for(Map.Entry<String, HashMap<String, Long>> entry : datadogLogData.entrySet()) {

            HashMap<String, Long> value = entry.getValue();
            List<Map.Entry<String, Long> > eventList =
                    new LinkedList<Map.Entry<String, Long> >(value.entrySet());

            // Sort the list
            Collections.sort(eventList, new Comparator<Map.Entry<String, Long> >() {
                public int compare(Map.Entry<String, Long> o1,
                                   Map.Entry<String, Long> o2)
                {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
            });

            Integer previousPage = 0;
            String previousPageName = "";
            // put data from sorted list to hashmap
            ArrayList<ArrayList<Object>> sample = new ArrayList<ArrayList<Object>>();
            for (Map.Entry<String, Long> aa : eventList) {
                if(previousPageName!=""){
                    if(funnelLeadsCounter.containsKey(previousPageName)){
                        funnelLeadsCounter.replace(previousPageName, funnelLeadsCounter.get(previousPageName)+1);
                    }else{
                        funnelLeadsCounter.put(previousPageName, 1);
                    }
                    JSONObject leadsObj = new JSONObject();
                    leadsObj.put("leadID", entry.getKey());
                    if(newtable.containsKey(previousPageName)){
                        if(newtable.get(previousPageName).containsKey(aa.getKey())){
                            Integer counterValue = (Integer) newtable.get(previousPageName).get(aa.getKey()).get("count") +1;
                            JSONArray leadsArrayList = (JSONArray) newtable.get(previousPageName).get(aa.getKey()).get("leadsArrayList");
                            leadsArrayList.add(leadsObj);
                            newtable.get(previousPageName).get(aa.getKey()).replace("count", counterValue);
                            newtable.get(previousPageName).get(aa.getKey()).replace("leadsArrayList", leadsArrayList);
                        }else{
                            JSONArray leadsArrayList = new JSONArray();
                            leadsArrayList.add(leadsObj);
                            HashMap<String, Object> tempCounter = new HashMap<String, Object>();
                            if(previousPageName.contains("ContactUs") || aa.getKey().contains("ContactUs")) {
                                tempCounter.put("color", "red");
                            }else if(funnelPage.indexOf(aa.getKey())<previousPage){
                                tempCounter.put("color", "red" );
                            }
                            else{
                                tempCounter.put("color", "green");
                            }
                            tempCounter.put("count" , 1);
                            tempCounter.put("leadsArrayList", leadsArrayList);
                            newtable.get(previousPageName).put(aa.getKey(), tempCounter);
                        }
                    }else{
                        HashMap<String, HashMap<String, Object>> tempHashMap = new HashMap<String, HashMap<String, Object>>();
                        HashMap<String, Object> tempCounter = new HashMap<String, Object>();
                        JSONArray leadsArrayList = new JSONArray();
                        leadsArrayList.add(leadsObj);
                        if(previousPageName.contains("ContactUs") || aa.getKey().contains("ContactUs")) {
                            tempCounter.put("color", "red");
                        }else if(funnelPage.indexOf(aa.getKey())<previousPage){
                            tempCounter.put("color", "red" );
                        }else{
                            tempCounter.put("color", "green");
                        }
                        tempCounter.put("count" , 1);
                        tempCounter.put("leadsArrayList", leadsArrayList);
                        tempHashMap.put(aa.getKey(), tempCounter);
                        newtable.put(previousPageName, tempHashMap);
                    }
                }

                previousPage = funnelPage.indexOf(aa.getKey());
                previousPageName = aa.getKey();
                ArrayList<Object> temp = new ArrayList<Object>();
                temp.add(aa.getValue());
                temp.add(funnelPage.indexOf(aa.getKey()));
                sample.add(temp);
            }

        }

        for(Map.Entry<String, HashMap<String, HashMap<String, Object>>> eachTableEntry : newtable.entrySet()) {

            HashMap<String, HashMap<String, Object>> eachTableEntryValue = eachTableEntry.getValue();
            for(Map.Entry<String, HashMap<String, Object>> eachLandingPage : eachTableEntryValue.entrySet()) {
                JSONObject eachTableRow = new JSONObject();
                eachTableRow.put("Percantage", String.format("%.2f",((float)((Integer) eachLandingPage.getValue().get("count"))/funnelLeadsCounter.get(eachTableEntry.getKey()))*100));
                eachTableRow.put("From", eachTableEntry.getKey());
                eachTableRow.put("To", eachLandingPage.getKey());
                eachTableRow.put("fromPriority", auto.get(eachTableEntry.getKey()));
                eachTableRow.put("toPriority", auto.get(eachLandingPage.getKey()));
                eachTableRow.put("Count", eachLandingPage.getValue().get("count"));
                eachTableRow.put("color", eachLandingPage.getValue().get("color"));
                eachTableRow.put("leadsArrayList", eachLandingPage.getValue().get("leadsArrayList"));
                tableJsonDataArray.add(eachTableRow);
            }
        }

        normalJourneyResObject.put("tableJsonDataArray", tableJsonDataArray);

        return normalJourneyResObject;
    }

}
