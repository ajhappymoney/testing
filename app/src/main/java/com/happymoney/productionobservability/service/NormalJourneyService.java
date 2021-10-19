package com.happymoney.productionobservability.service;

import com.google.gson.JsonObject;
import com.happymoney.productionobservability.adaptor.DatadogAdaptor;
import com.happymoney.productionobservability.helper.UserJourneyHelper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;

@Service
public class NormalJourneyService {

    @Autowired
    private DatadogAdaptor datadogAdaptor;

    @Autowired
    private UserJourneyHelper userJourneyHelper;

    public JSONObject fetchUserJourneyData(OffsetDateTime fromDate, OffsetDateTime toDate) {

        HashMap<String, HashMap<String, Long>> datadogRes = datadogAdaptor.getNormalCheckLogData(fromDate, toDate);
        JSONObject normalUserJourneyModelRes = userJourneyHelper.getNormalJourneyModelAttributes(datadogRes);
        return normalUserJourneyModelRes;
    }

}
