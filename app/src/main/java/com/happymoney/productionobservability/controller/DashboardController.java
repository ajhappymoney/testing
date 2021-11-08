package com.happymoney.productionobservability.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.helper.ProcessTimeHelper;
import com.happymoney.productionobservability.service.DashboardService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Autowired
    private ProcessTimeHelper processTimeHelper;

    Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @RequestMapping(value = "/chart", method=RequestMethod.GET)
    public String chart(@RequestParam(required = false, name = "fromdt") String fromdt, @RequestParam(required = false, name = "todt") String todt,
            @RequestParam(required = false, name = "reloadCheck") Boolean reloadCheck,
                        @RequestParam(required = false, name = "newCustomers") Boolean newCustomers,
                        Model model) throws ParseException {

        Long startTime = processTimeHelper.getStartTime();

        String requestName = "chart";
        OffsetDateTime fromDate;
        OffsetDateTime toDate;

        if(reloadCheck==null){
            reloadCheck = false;
        }

        if(newCustomers==null){
            newCustomers = false;
        }

        if(fromdt==null) {
            fromDate = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2);
        }else{
            Long fromEpoch = Long.parseLong(fromdt);
            fromDate = OffsetDateTime.ofInstant(new Timestamp(fromEpoch).toInstant(), ZoneOffset.UTC);
        }

        if(todt==null || reloadCheck) {
            toDate = OffsetDateTime.now(ZoneOffset.UTC);
            if(reloadCheck) {
                logger.info("requestName:"+requestName+" AutoReload page. Setting toDate to now");
            }
        }else {
            Long toEPoch = Long.parseLong(todt);
            toDate = OffsetDateTime.ofInstant(new Timestamp(toEPoch).toInstant(), ZoneOffset.UTC);
        }
        logger.info("requestName:"+requestName+" Loading dashboard for loan journey from = "+fromDate+" to = "+toDate + " loading new customer data ="+newCustomers);
        JsonObject result = dashboardService.getPageCount(fromDate, toDate, newCustomers, requestName);
        if(!(result==null)) {

            ArrayList<String> pageNames = new ArrayList<String>();
            ArrayList<Integer> counterValue = new ArrayList<Integer>();
            JSONObject simpleJsonObject = new JSONObject();

            ArrayList<HashMap<String, Object>> seriesDataArray = new ArrayList<HashMap<String, Object>>();
            HashMap<String, Object> seriesDataObject = new HashMap<String, Object>();
            JsonObject leadidsObject = new JsonObject();
            JSONParser parser = new JSONParser();

            Set<Map.Entry<String, JsonElement>> entrySet = result.entrySet();
            for (Map.Entry<String, JsonElement> entry : entrySet) {

                pageNames.add(entry.getKey().toString());

                JsonObject keyvalue = result.getAsJsonObject(entry.getKey());

                counterValue.add(keyvalue.get("count").getAsInt());
                leadidsObject.add(entry.getKey(), keyvalue.get("resultAttributes"));
                String JSONBody2 = new Gson().toJson(keyvalue.get("resultAttributes"));

                JSONArray resultAttributeArray = (JSONArray) parser.parse(JSONBody2);

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
            model.addAttribute("reloadCheck", reloadCheck);
            model.addAttribute("newCustomers", newCustomers);
        }
        processTimeHelper.printProcessEndTime(startTime, "Load Dashboard");
        if(fromdt==null && todt==null) {
            return "chart";
        }else {
            return "dashboard";
        }
    }

    @RequestMapping(value = "/datadog", method = RequestMethod.GET, params = {"leaduid","errorlog", "fromdt","todt"})
    public ModelAndView getDatadogLink(@RequestParam(value="leaduid", required = true) String leadGuid, @RequestParam(value="errorlog", required = true) Boolean errorLog,
                                       @RequestParam(required = true, name = "fromdt") String fromDate, @RequestParam(required = true, name = "todt") String toDate) {

        String datadogUrl= dashboardService.getDatadogLink(leadGuid, errorLog, fromDate, toDate);
        logger.info("Generated Datadog Url = " + datadogUrl);
        return new ModelAndView(new RedirectView(datadogUrl));

    }

    @RequestMapping(value = "/fullstory", method = RequestMethod.GET, params = {"leaduid","memberid","fromdt","todt"})
    public ModelAndView getFullStoryLink(@RequestParam(value="leaduid", required = true) String leadGuid,
                                         @RequestParam(required = true, name = "fromdt") String fromDate, @RequestParam(required = true, name = "memberid") String memberid,
    @RequestParam(required = true, name = "todt") String toDate){
        Long startTime = processTimeHelper.getStartTime();
        String requestName = "fullstory";
        logger.info("requestName:"+requestName+" Extracting full story link for leadGuid = " + leadGuid +", memberid = " + memberid +", fromDate = " + fromDate + ", toDate = " + toDate);
        String sessionString = dashboardService.getFullStoryLink(leadGuid, memberid, fromDate, toDate, requestName);
        if(sessionString==null){
            return new ModelAndView("noFullStoryPage");
        }
        logger.info("requestName:"+requestName+" Generated FullStory Url = " + sessionString);
        processTimeHelper.printProcessEndTime(startTime, "requestName:"+requestName );
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
