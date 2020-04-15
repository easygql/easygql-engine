package com.easygql.util;


import com.easygql.thirdapis.ThirdAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class ThirdAPIPool {
    private static Map<String,ThirdAPI> thirdApiQueryPool = new HashMap<>();
    private static Map<String,ThirdAPI> thirdApiMutationPool = new HashMap<>();
    private static Map<String,ThirdAPI> thirdApiSubscriptionPool = new HashMap<>();
    public static synchronized void resetPool(HashMap resetPool) {
        thirdApiQueryPool.clear();
        thirdApiMutationPool.clear();
        thirdApiSubscriptionPool.clear();
        thirdApiQueryPool.putAll((HashMap)resetPool.get(GRAPHQL_QUERYAPI_FIELDNAME));
        thirdApiMutationPool.putAll((HashMap)resetPool.get(GRAPHQL_MUTATIONAPI_FIELDNAME));
        thirdApiSubscriptionPool.putAll((HashMap)resetPool.get(GRAPHQL_SUBSCRIPTIONAPI_FIELDNAME));
    }
    public static  ThirdAPI getThirdAPI(String apiName) {
        if(null!=thirdApiQueryPool.get(apiName)) {
            return thirdApiQueryPool.get(apiName);
        } else if(null != thirdApiMutationPool.get(apiName)) {
            return thirdApiMutationPool.get(apiName);
        } else {
            return  thirdApiSubscriptionPool.get(apiName);
        }
    }
    public static ThirdAPI getQueryAPI(String apiName) {
        return thirdApiQueryPool.get(apiName);
    }
    public static ThirdAPI getMutationAPI(String apiName) {
        return thirdApiQueryPool.get(apiName);
    }
    public static ThirdAPI getSubscriptionAPI(String apiName) {
        return thirdApiQueryPool.get(apiName);
    }
    public static Map getAllThirdAPI(){
        HashMap resultMap = new HashMap();
        List<String> queryAPIList = new ArrayList<>();
        List<String> mutationAPIList = new ArrayList<>();
        List<String> subscriptionAPIList = new ArrayList<>();
        queryAPIList.addAll(thirdApiQueryPool.keySet());
        mutationAPIList.addAll(thirdApiMutationPool.keySet());
        subscriptionAPIList.addAll(thirdApiSubscriptionPool.keySet());
        resultMap.put(GRAPHQL_QUERYAPI_FIELDNAME,queryAPIList);
        resultMap.put(GRAPHQL_MUTATIONAPI_FIELDNAME,mutationAPIList);
        resultMap.put(GRAPHQL_SUBSCRIPTIONAPI_FIELDNAME,subscriptionAPIList);
        return resultMap;
    }
    public static String getAPIKind(String apiName) {
        if(null!=thirdApiQueryPool.get(apiName)) {
            return GRAPHQL_QUERYAPI_FIELDNAME;
        } else if(null!=thirdApiMutationPool.get(apiName)) {
            return GRAPHQL_MUTATIONAPI_FIELDNAME;
        } else if(null!=thirdApiSubscriptionPool.get(apiName)) {
            return GRAPHQL_SUBSCRIPTIONAPI_FIELDNAME;
        } else {
            return null;
        }
    }
}
