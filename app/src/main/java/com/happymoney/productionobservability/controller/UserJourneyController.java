package com.happymoney.productionobservability.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.happymoney.productionobservability.helper.ProcessTimeHelper;
import com.happymoney.productionobservability.service.DashboardService;
import com.happymoney.productionobservability.service.UserJourneyService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

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

    @Autowired
    private ProcessTimeHelper processTimeHelper;

    Logger logger = LoggerFactory.getLogger(UserJourneyController.class);

    @RequestMapping(value = "/userJourney", method= RequestMethod.GET)
    public String userJourney() {
        return "userJourney";
    }

    @RequestMapping(value="/getUserJourney", method = RequestMethod.GET)
    public ResponseEntity<?> getUserJourney(@RequestParam("fromdate") String fromdate,
                                            @RequestParam("todate") String todate,
                                            @RequestParam("leadId") String leadId) throws ParseException {
        Long startTime = processTimeHelper.getStartTime();
        String requestName = "getUserJourney";
        logger.info("requestName:"+requestName+" Loading user Journey information for leadID:"+leadId+" between fromdt = " + fromdate + ", todt = " + todate);

        OffsetDateTime fromOffsetDateTime;
        OffsetDateTime toOffsetDateTime;

        Long fromEpoch = Long.parseLong(fromdate);
        fromOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(fromEpoch).toInstant(), ZoneOffset.UTC);

        Long toEPoch = Long.parseLong(todate);
        toOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(toEPoch).toInstant(), ZoneOffset.UTC);

        JSONArray seriesArray = new JSONArray();
        JSONObject userJourney = userJourneyService.getUserJourneyData(fromOffsetDateTime, toOffsetDateTime, leadId, requestName);

        processTimeHelper.printProcessEndTime(startTime, requestName);
        return ResponseEntity.ok(userJourney);
    }

    @RequestMapping(value = "/datadogLogs", method = RequestMethod.GET, params = {"leaduid","errorlog", "fromdt","todt"})
    public ResponseEntity<?> datadogLogs(@RequestParam(value="leaduid", required = true) String leadGuid, @RequestParam(value="errorlog", required = true) Boolean errorLog,
                                    @RequestParam(required = true, name = "fromdt") String fromDate, @RequestParam(required = true, name = "todt") String toDate) {

        Long startTime = processTimeHelper.getStartTime();

        String requestName = "datadogLogs";

        OffsetDateTime fromOffsetDateTime;
        OffsetDateTime toOffsetDateTime;

        Long fromEpoch = Long.parseLong(fromDate);
        fromOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(fromEpoch).toInstant(), ZoneOffset.UTC);

        Long toEpoch = Long.parseLong(toDate);
        toOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(toEpoch).toInstant(), ZoneOffset.UTC);

        JSONArray datadogLogData= userJourneyService.getDatadogDataTableEntries(leadGuid, errorLog, fromOffsetDateTime, toOffsetDateTime);
//        logger.info("Generated Datadog Url = " + datadogUrl);
        return ResponseEntity.ok(datadogLogData);

    }
}
