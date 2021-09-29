package com.happymoney.productionobservability.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.service.DashboardService;
import com.happymoney.productionobservability.service.UserJourneyService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;

@Controller
@Component
public class UserJourneyController {

    @Autowired
    private UserJourneyService userJourneyService;

    Logger logger = LoggerFactory.getLogger(UserJourneyController.class);

    @RequestMapping(value = "/userJourney", method= RequestMethod.GET)
    public String userJourney() {
        return "userJourney";
    }

    @RequestMapping(value="/getUserJourney", method = RequestMethod.GET)
    public String getUserJourney(@RequestParam(required = true, name = "fromdt") String fromdt,
                                 @RequestParam(required = true, name = "todt") String todt,
    @RequestParam(required = true, name = "leadId") String leadId,  Model model) throws ParseException {
        logger.info("Loading user Journey information for leadID:"+leadId+" between fromdt = " + fromdt + ", todt = " + todt);

        JSONArray seriesArray = new JSONArray();
        JsonObject userJourney = userJourneyService.getUserJourneyData(fromdt, todt, leadId);
        ArrayList<ArrayList<Object>> lineSeriesData = new ArrayList<ArrayList<Object>>();
        ArrayList<ArrayList<Object>> sample = new ArrayList<ArrayList<Object>>();
        Map<String, Integer> mapval = new HashMap<>();

        Map<String,Integer> auto = new HashMap<String,Integer>();
        JSONObject sampleObj = new JSONObject();
//        ["Name","Birthday","Address","Phone","Income","HousingPayment","Account","Balance","Offer","Employment","DCP","BankAccount","Verification Documents",
//        "Offer Review","Partner Lending Page","Truth In Lending Statement","E-Sign Complete","AdverseAction",
//        "Experian Credit Freeze","KBA","KBA Failed","Autopay","Review","ContactUs","PlaidNoAuth"];
        auto.put("Name", 1);
        auto.put("Birthday", 2);
        auto.put("Address", 3);
        auto.put("Phone", 4);
        auto.put("Income", 5);
        auto.put("HousingPayment", 6);
        auto.put("Account", 7);
        auto.put("Balance", 8);
        auto.put("Offer", 9);
        auto.put("Employment", 10);
        auto.put("DCP", 11);
        auto.put("BankAccount", 12);
        auto.put("Verification Documents", 13);
        auto.put("Offer Review", 14);
        auto.put("Partner Lending Page", 15);
        auto.put("Truth In Lending Statement", 16);
        auto.put("E-Sign Complete", 17);
        auto.put("AdverseAction", 18);
        auto.put("Experian Credit Freeze", 19);
        auto.put("KBA", 20);
        auto.put("KBA Failed", 21);
        auto.put("Autopay", 22);
        auto.put("Review", 23);
        auto.put("ContactUs", 24);
        auto.put("PlaidNoAuth", 25);
        List<String> funnelPage = new ArrayList<String>();
        Set<Map.Entry<String, JsonElement>> entrySet = userJourney.entrySet();
        for(Map.Entry<String,JsonElement> entry : entrySet){
            JSONObject seriesData = new JSONObject();

            JsonObject keyvalue = userJourney.getAsJsonObject(entry.getKey());
            seriesData.put("name", entry.getKey());
            for (String key: keyvalue.keySet()
                 ) {
                ArrayList<Object> temp = new ArrayList<Object>();
                mapval.put(key, auto.get(key));
                temp.add(key);
                temp.add(Long.parseLong(keyvalue.get(key).toString()));
                lineSeriesData.add(temp);
                sampleObj.put(key, Long.parseLong(keyvalue.get(key).toString()));
            }

            List<Map.Entry<String, Integer>> list = new ArrayList<>(mapval.entrySet());
            list.sort(Map.Entry.comparingByValue());
            int count = 0;
            Map<String, Integer> result = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> mapentry : list) {
                result.put(mapentry.getKey(), mapentry.getValue());
                funnelPage.add(mapentry.getKey());
                count++;
            }
//            System.out.println("result "+result );
//            System.out.println("funnelPage " + funnelPage);

            for(int j=count-1; j>=0; j--){
                ArrayList<Object> temp = new ArrayList<Object>();
                temp.add(funnelPage.get(j));
                temp.add(sampleObj.get(funnelPage.get(j)));
                sample.add(temp);
            }
            Collections.reverse(funnelPage);
//            System.out.println("funnelPage " + funnelPage);
//            System.out.println("sample = " + sample);

            seriesData.put("data", sample);
            seriesData.put("type","scatter");

            seriesData.put("lineWidth", 1);
            seriesArray.add(seriesData);


        }
        model.addAttribute("lineSeriesData",lineSeriesData);
        model.addAttribute("fromdt", fromdt);
        model.addAttribute("todt", todt);
        model.addAttribute("seriesArray",seriesArray);
        model.addAttribute("leadId",leadId);
        model.addAttribute("funnelPage", funnelPage);

        return "userJourney";
    }
}
