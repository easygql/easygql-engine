package com.easygql.graphql.datafetcher;

import graphql.schema.DataFetchingEnvironment;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class DataFetcherRemoteAPI implements EasyGQLDataFetcher<Object> {
    protected  final String endPoint;
    protected  final Consumer<HttpHeaders> httpHeadersConsumer;
    public  DataFetcherRemoteAPI(String endPoint, HashMap headers) {
        this.endPoint=endPoint;
        Iterator<Map.Entry<String, String>> iterator = headers.keySet().iterator();
        httpHeadersConsumer = (httpHeaders)->{
            while(iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String headerKey = entry.getKey();
                String headerVal = entry.getValue();
                httpHeaders.add(headerKey,headerVal);
            }
        };
    }
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        return null;    
    }
}
