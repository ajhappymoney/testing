package com.happymoney.productionobservability.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class FullStoryHelper {

    @Value("${fullstory.token}")
    private String fullstoryToken;

    public String getFullstoryToken() {
        return fullstoryToken;
    }
}
