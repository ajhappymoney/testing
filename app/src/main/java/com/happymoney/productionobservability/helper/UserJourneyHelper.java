package com.happymoney.productionobservability.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UserJourneyHelper {

    @Autowired
    private SortDataHelper sortDataHelper;

    public JSONObject getUserJourneyModelAttributes(JsonObject userJourneyResult){
        JSONObject userJourneyResObject = new JSONObject();
        ArrayList<ArrayList<Object>> lineSeriesData = new ArrayList<ArrayList<Object>>();
        ArrayList<ArrayList<Object>> sample = new ArrayList<ArrayList<Object>>();
        Map<String, Integer> mapval = new HashMap<>();
        JSONArray seriesArray = new JSONArray();


        Map<String,Integer> auto = sortDataHelper.getPriorityMap();
        HashMap<Long, String> sampleObj = new HashMap<Long, String>();
//        ["Name","Birthday","Address","Phone","Income","HousingPayment","Account","Balance","Offer","Employment","DCP","BankAccount","Verification Documents",
//        "Offer Review","Partner Lending Page","Truth In Lending Statement","E-Sign Complete","AdverseAction",
//        "Experian Credit Freeze","KBA","KBA Failed","Autopay","Review","ContactUs","PlaidNoAuth"];
        List<String> funnelPage = new ArrayList<String>();
        Set<Map.Entry<String, JsonElement>> entrySet = userJourneyResult.entrySet();
        for(Map.Entry<String,JsonElement> entry : entrySet){
            JSONObject seriesData = new JSONObject();

            JsonObject keyvalue = userJourneyResult.getAsJsonObject(entry.getKey());
            seriesData.put("name", entry.getKey());
            for (String key: keyvalue.keySet()
            ) {
                if(!(mapval.containsKey(key)) ){
                    mapval.put(key, auto.get(key));
                }
                JsonArray paymentsArray = keyvalue.getAsJsonArray(key);
                for (JsonElement eventTime: paymentsArray
                ) {
                    sampleObj.put(eventTime.getAsLong(), key);
                }
            }

            List<Map.Entry<String, Integer>> list = new ArrayList<>(mapval.entrySet());
            list.sort(Map.Entry.comparingByValue());

            Map<String, Integer> result = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> mapentry : list) {
                result.put(mapentry.getKey(), mapentry.getValue());
                funnelPage.add(mapentry.getKey());
            }


            TreeMap<Long, String> sorted = new TreeMap<>();

            // Copy all data from hashMap into TreeMap
            sorted.putAll(sampleObj);

            // Display the TreeMap which is naturally sorted
            for (Map.Entry<Long, String> entrytest : sorted.entrySet()){
                ArrayList<Object> temp = new ArrayList<Object>();
                if(entrytest.getKey()==1632937492459L){
                    temp.add(1632937874111L);
                }else {
                    temp.add(entrytest.getKey());
                }
                temp.add(funnelPage.indexOf(entrytest.getValue()));

                sample.add(temp);
            }

            seriesData.put("data", sample);
            seriesData.put("lineWidth", 1);
            seriesArray.add(seriesData);


        }
        userJourneyResObject.put("seriesArray",seriesArray);
        userJourneyResObject.put("funnelPage",funnelPage);
//        System.out.println("userJourneyResObject = " + userJourneyResObject);

        return userJourneyResObject;
    }

}
