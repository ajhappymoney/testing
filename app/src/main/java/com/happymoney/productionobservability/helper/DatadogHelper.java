package com.happymoney.productionobservability.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class DatadogHelper {

    @Value("${datadog.key.api}")
    private String datadogApiKey;

    @Value("${datadog.key.application}")
    private String datadogApplicationKey;

    @Value("${datadog.site}")
    private String datadogSite;

    public String getDatadogApiKey() {
        return datadogApiKey;
    }

    public String getDatadogApplicationKey() {
        return datadogApplicationKey;
    }

    public String getDatadogSite() {
        return datadogSite;
    }
}
