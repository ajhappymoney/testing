package com.happymoney.productionobservability.helper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SortDataHelper {

    public Map<String,Integer> getPriorityMap(){
        /***
         * /apply/prequal:
         * 	name
         * 	birthday
         * 	address
         * 	phone
         * 	income
         * 	housing_payment
         * 	account
         * 	balance
         * 	offer
         * /apply/verification:
         * 	employment
         * 	direct_card_payoff
         * 	bank_account
         * 	documents
         * /apply/esign/
         * 	offer_review
         * 	partner
         * 	truth_in_lending
         * 	esign_complete
        ***/
        Map<String,Integer> auto = new HashMap<String,Integer>();
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
        auto.put("Experian Credit Freeze", 11);
        auto.put("KBA", 12);
        auto.put("KBA Failed", 13);
        auto.put("DCP", 14);
        auto.put("BankAccount", 15);
        auto.put("PlaidNoAuth", 16);
        auto.put("Autopay", 17);
        auto.put("Verification Documents", 18);
        auto.put("Offer Review", 19);
        auto.put("Partner Lending Page", 20);
        auto.put("Truth In Lending Statement", 21);
        auto.put("E-Sign Complete", 22);
        auto.put("AdverseAction", 23);
        auto.put("Review", 24);
        auto.put("ContactUs", 25);

        return auto;
    }

    public JsonObject getSortedData(HashMap<Integer, HashMap<String, Object>> beforeData){
        Gson gson = new Gson();
        LinkedHashMap<String, HashMap<String, Object>> result = new LinkedHashMap<String, HashMap<String, Object>>();
        Map<Integer, HashMap<String, Object>> afterData = new TreeMap<Integer, HashMap<String, Object>> (beforeData);
        Set set2 = afterData.entrySet();
        Iterator iterator2 = set2.iterator();
        while(iterator2.hasNext()) {
            Map.Entry me2 = (Map.Entry)iterator2.next();

            HashMap<String, Object> newVal = new HashMap<String, Object>();
            HashMap<String, Object> values = (HashMap<String, Object>) me2.getValue();
            newVal.put("count", values.get("count"));
            newVal.put("resultAttributes", values.get("resultAttributes"));
            result.put(values.get("page").toString(), newVal);
        }
        String json = gson.toJson(result);
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return jsonObject;

    }
}
