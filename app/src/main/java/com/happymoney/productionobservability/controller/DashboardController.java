package com.happymoney.productionobservability.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.service.DashboardService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
@Controller
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @RequestMapping(value = "/chart", method=RequestMethod.GET)
    public String chart(@RequestParam(required = false, name = "fromdt") String fromdt, @RequestParam(required = false, name = "todt") String todt , Model model) throws ParseException {
        OffsetDateTime fromDate;
        OffsetDateTime toDate;

        if(fromdt==null) {
            fromDate = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2);
        }else{
            Long fromEpoch = Long.parseLong(fromdt);
            fromDate = OffsetDateTime.ofInstant(new Timestamp(fromEpoch).toInstant(), ZoneOffset.UTC);
        }

        if(todt==null) {
            toDate = OffsetDateTime.now(ZoneOffset.UTC);
        }else {
            Long toEPoch = Long.parseLong(todt);
            toDate = OffsetDateTime.ofInstant(new Timestamp(toEPoch).toInstant(), ZoneOffset.UTC);
        }
        JsonObject result = dashboardService.getPageCount(fromDate, toDate);

        ArrayList<String> pageNames = new ArrayList<String>();
        ArrayList<Integer> counterValue = new ArrayList<Integer>();
        JSONObject simpleJsonObject = new JSONObject();

        ArrayList<HashMap<String, Object>> seriesDataArray = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> seriesDataObject = new HashMap<String, Object>();
        JsonObject leadidsObject = new JsonObject();
        JSONParser parser = new JSONParser();

        Set<Map.Entry<String, JsonElement>> entrySet = result.entrySet();
        for(Map.Entry<String,JsonElement> entry : entrySet){

            pageNames.add(entry.getKey().toString());

            JsonObject keyvalue = result.getAsJsonObject(entry.getKey());

            counterValue.add(keyvalue.get("count").getAsInt());
            leadidsObject.add(entry.getKey(), keyvalue.get("resultAttributes"));
            String JSONBody2 = new Gson().toJson(keyvalue.get("resultAttributes"));

            JSONArray resultAttributeArray = (JSONArray)parser.parse(JSONBody2);

            simpleJsonObject.put(entry.getKey(), resultAttributeArray);

        }

        seriesDataObject.put("data", counterValue);
        seriesDataObject.put("name", "Funnel page");

        seriesDataArray.add(seriesDataObject);

        model.addAttribute("counterValue", counterValue);
        model.addAttribute("simpleJsonObject", simpleJsonObject);
        model.addAttribute("pageNames", pageNames);
        model.addAttribute("fromdt", fromDate);
        model.addAttribute("todt", toDate);
        if(fromdt==null && todt==null) {
            return "chart";
        }else {
            return "dashboard";
        }
    }

    @RequestMapping(value = "/datadog", method = RequestMethod.GET, params = {"leaduid", "fromdt","todt"})
    public ModelAndView getDatadogLink(@RequestParam(value="leaduid", required = true) String leadGuid,
                                       @RequestParam(required = true, name = "fromdt") String fromDate, @RequestParam(required = true, name = "todt") String toDate) {

        String datadogUrl= dashboardService.getDatadogLink(leadGuid, fromDate, toDate);
        System.out.println("datadogUrl = " + datadogUrl);
        return new ModelAndView(new RedirectView(datadogUrl));

    }

    @RequestMapping(value = "/fullstory", method = RequestMethod.GET, params = {"leaduid","memberid","fromdt","todt"})
    public ModelAndView getFullStoryLink(@RequestParam(value="leaduid", required = true) String leadGuid,
                                         @RequestParam(required = true, name = "fromdt") String fromDate, @RequestParam(required = true, name = "memberid") String memberid,
    @RequestParam(required = true, name = "todt") String toDate){
        String sessionString = dashboardService.getFullStoryLink(leadGuid, memberid, fromDate, toDate);
        return new ModelAndView(new RedirectView(sessionString));

    }

    @RequestMapping(value = "/load", method = RequestMethod.GET)
    public String getUserDetails(@RequestParam("fromdt") @DateTimeFormat(pattern="yyyy-MM-ddTHH:MM") Date fromDate) {
        return "chart";
    }

    //redirect to demo if user hits the root
    @RequestMapping("/")
    public String home(Model model) {
        return "redirect:chart";
    }
}
