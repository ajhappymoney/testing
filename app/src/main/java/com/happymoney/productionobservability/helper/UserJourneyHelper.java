package com.happymoney.productionobservability.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ognl.BooleanExpression;
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
        JSONArray seriesArray = new JSONArray();

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
            JsonObject keyvalue = userJourneyResult.getAsJsonObject(entry.getKey());
            seriesData.put("name", entry.getKey());
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
            // Display the TreeMap which is naturally sorted
            for (Map.Entry<Long, String> entrytest : sorted.entrySet()){
                ArrayList<Object> temp = new ArrayList<Object>();
                temp.add(entrytest.getKey());
                temp.add(funnelPage.indexOf(entrytest.getValue()));
                sample.add(temp);
            }

            Random obj = new Random();
            int rand_num = obj.nextInt(0xffffff + 1);
            String colorCode = String.format("#%06x", rand_num);

            seriesData.put("data", sample);
            seriesData.put("type","scatter");
            seriesData.put("lineWidth", 1);
            seriesData.put("color", colorCode);
            seriesArray.add(seriesData);

        }
        userJourneyResObject.put("seriesArray",seriesArray);
        userJourneyResObject.put("funnelPage",funnelPage);

        return userJourneyResObject;
    }

    public JSONObject getNormalJourneyModelAttributes(HashMap<String, HashMap<String, Long>> datadogLogData) {

        JSONObject normalJourneyResObject = new JSONObject();

        JSONArray seriesArray = new JSONArray();

        List<String> funnelPage = new ArrayList<String>();

        Map<String,Integer> auto = sortDataHelper.getPriorityMap();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(auto.entrySet());
        list.sort(Map.Entry.comparingByValue());

        for (Map.Entry<String, Integer> entry : list) {
            funnelPage.add(entry.getKey());
        }


        for(Map.Entry<String, HashMap<String, Long>> entry : datadogLogData.entrySet()) {

            String key = entry.getKey();
            JSONObject seriesData = new JSONObject();
            seriesData.put("name", entry.getKey());
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
            Boolean isNotNormal = false;
            // put data from sorted list to hashmap
            ArrayList<ArrayList<Object>> sample = new ArrayList<ArrayList<Object>>();
            for (Map.Entry<String, Long> aa : eventList) {
                if (funnelPage.indexOf(aa.getKey())<previousPage){
                    isNotNormal = true;
                }
                previousPage = funnelPage.indexOf(aa.getKey());
                ArrayList<Object> temp = new ArrayList<Object>();
                temp.add(aa.getValue());
                temp.add(funnelPage.indexOf(aa.getKey()));
                sample.add(temp);

            }

            if(isNotNormal) {
                Random obj = new Random();
                int rand_num = obj.nextInt(0xffffff + 1);
                String colorCode = String.format("#%06x", rand_num);

                seriesData.put("data", sample);
                seriesData.put("type","scatter");
                seriesData.put("lineWidth", 1);
                seriesData.put("color", colorCode);
                seriesArray.add(seriesData);
            }


        }
        normalJourneyResObject.put("seriesArray",seriesArray);
        normalJourneyResObject.put("funnelPage",funnelPage);

        System.out.println("normalJourneyResObject = " + normalJourneyResObject);
        return normalJourneyResObject;
    }

}
