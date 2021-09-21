package com.happymoney.productionobservability.adaptor;

import com.datadog.api.v2.client.ApiClient;
import com.datadog.api.v2.client.Configuration;
import com.happymoney.productionobservability.helper.DatadogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@EnableConfigurationProperties
public class DatadogConnectionAdaptor {

    @Autowired
    private DatadogHelper datadogHelper;

    public ApiClient getDatadogApiClient(){
        try {
            ApiClient defaultClient = Configuration.getDefaultApiClient();

            // Configure the Datadog site to send API calls to
            HashMap<String, String> serverVariables = new HashMap<String, String>();
            String site = datadogHelper.getDatadogSite();
            if (site != null) {
                serverVariables.put("site", site);
                defaultClient.setServerVariables(serverVariables);
            }

            // Configure API key authorization:
            HashMap<String, String> secrets = new HashMap<String, String>();
            secrets.put("apiKeyAuth", datadogHelper.getDatadogApiKey());
            secrets.put("appKeyAuth", datadogHelper.getDatadogApplicationKey());
            defaultClient.configureApiKeys(secrets);

            return defaultClient;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
