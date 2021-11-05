package com.happymoney.productionobservability.controller;


import com.happymoney.productionobservability.helper.ProcessTimeHelper;
import com.happymoney.productionobservability.service.NormalJourneyService;
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

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Controller
public class NormalJourneyController {

    @Autowired
    private NormalJourneyService normalJourneyService;

    @Autowired
    private ProcessTimeHelper processTimeHelper;

    Logger logger = LoggerFactory.getLogger(NormalJourneyController.class);

    @RequestMapping(value = "/unusualJourney", method= RequestMethod.GET)
    public String userJourney() {
        return "normalJourney";
    }

    @RequestMapping(value="/getUnusualJourney", method = RequestMethod.GET)
    public String getNormalJourney(@RequestParam("fromdate") String fromdate,
                                 @RequestParam("todate") String todate, Model model) throws ParseException {
        String requestName = "getUnusualJourney";

        Long startTime = processTimeHelper.getStartTime();

        OffsetDateTime fromOffsetDateTime;
        OffsetDateTime toOffsetDateTime;

        Long fromEpoch = Long.parseLong(fromdate);
        fromOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(fromEpoch).toInstant(), ZoneOffset.UTC);

        Long toEPoch = Long.parseLong(todate);
        toOffsetDateTime = OffsetDateTime.ofInstant(new Timestamp(toEPoch).toInstant(), ZoneOffset.UTC);

        logger.info("requestName:"+requestName+" Loading atypical journey information between fromDateTime = " + fromOffsetDateTime + ", toDateTime = " + toOffsetDateTime);


        JSONObject normalUserJourney = normalJourneyService.fetchUserJourneyData(fromOffsetDateTime,
                toOffsetDateTime, requestName);

        model.addAttribute("fromdt", fromOffsetDateTime);
        model.addAttribute("todt", toOffsetDateTime);
        model.addAttribute("seriesObj",normalUserJourney.get("seriesObj"));
        model.addAttribute("funnelPage", normalUserJourney.get("funnelPage"));
        model.addAttribute("leadsList", normalUserJourney.get("leadsList"));

        processTimeHelper.printProcessEndTime(startTime, "Unusual Journey" + " FromDateTime:"+fromOffsetDateTime +" ToDateTime:"+toOffsetDateTime);
        return "normalJourney";

    }
}
