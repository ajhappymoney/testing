package com.happymoney.productionobservability.helper;

import com.happymoney.productionobservability.controller.DashboardController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessTimeHelper {

    Logger logger = LoggerFactory.getLogger(ProcessTimeHelper.class);

    public Long getStartTime(){
        long startTime = System.currentTimeMillis();
        return startTime;
    }

    public void printProcessEndTime(Long startTime, String functionName){
        long endTime = System.currentTimeMillis();
        logger.info("Process:"+functionName+" took:" + (endTime - startTime)/1000 + " seconds");
    }
}