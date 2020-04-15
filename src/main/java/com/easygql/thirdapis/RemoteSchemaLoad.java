package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.exception.BusinessException;
import com.easygql.util.RemoteSchemaLoader;
import com.easygql.util.SchemaData;
import com.easygql.util.ThirdAPIField;
import com.easygql.util.ThirdAPIInput;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@EasyGQLThirdAPI("RemoteSchemaLoad")
@Slf4j
public class RemoteSchemaLoad extends ThirdAPI {
    @Override
    public Object doWork(ThirdAPIInput thirdAPIInput) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                String endPointURL = String.class.cast(thirdAPIInput.getRunTimeInfo().get(GRAPHQL_ENDPOINT_FIELDNAME));
                if(null==endPointURL||endPointURL.trim().equals("")) {
                    future.completeExceptionally(new BusinessException("E10084"));
                }
                Object headers = thirdAPIInput.getRunTimeInfo().get(GRAPHQL_HEADERS_FIELDNAME);
                RemoteSchemaLoader.loadRemoteAPI(endPointURL.trim(),headers).whenComplete((result,resultEx)->{
                    if(null!=resultEx) {
                        future.completeExceptionally(resultEx);
                    } else {
                        future.complete(result);
                    }
                });
            } catch(Exception e) {
                future.completeExceptionally(e);
            }
        });
        return  future;
    }

    @Override
    public HashMap<String, ThirdAPIField> inputFields() {
        HashMap inputFieldsMap = new HashMap();
        ThirdAPIField endPointField = new ThirdAPIField();
        endPointField.setName(GRAPHQL_ENDPOINT_FIELDNAME);
        endPointField.setType(GRAPHQL_STRING_TYPENAME);
        endPointField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        endPointField.setNotnull(true);
        inputFieldsMap.put(GRAPHQL_ENDPOINT_FIELDNAME,endPointField);
        ThirdAPIField inputHeaders = new ThirdAPIField();
        inputHeaders.setName(GRAPHQL_HEADERS_FIELDNAME);
        inputHeaders.setType(GRAPHQL_OBJECT_TYPENAME);
        inputHeaders.setKind(GRAPHQL_TYPEKIND_SCALAR);
        inputHeaders.setIslist(true);
        inputFieldsMap.put(GRAPHQL_HEADERS_FIELDNAME,inputHeaders);
        return inputFieldsMap;
    }

    @Override
    public HashMap<String, ThirdAPIField> outputFields() {
        HashMap outputFieldsMap = new HashMap();
        ThirdAPIField queryAPI = new ThirdAPIField();
        queryAPI.setName(GRAPHQL_QUERYAPI_FIELDNAME);
        queryAPI.setType(GRAPHQL_OBJECT_TYPENAME);
        queryAPI.setKind(GRAPHQL_TYPEKIND_SCALAR);
        queryAPI.setIslist(true);
        ThirdAPIField mutationAPI=new ThirdAPIField();
        mutationAPI.setName(GRAPHQL_MUTATIONAPI_FIELDNAME);
        mutationAPI.setType(GRAPHQL_OBJECT_TYPENAME);
        mutationAPI.setKind(GRAPHQL_TYPEKIND_SCALAR);
        mutationAPI.setIslist(true);
        ThirdAPIField subscriptionAPI = new ThirdAPIField();
        subscriptionAPI.setName(GRAPHQL_SUBSCRIPTIONAPI_FIELDNAME);
        subscriptionAPI.setType(GRAPHQL_OBJECT_TYPENAME);
        subscriptionAPI.setKind(GRAPHQL_TYPEKIND_SCALAR);
        subscriptionAPI.setIslist(true);
        ThirdAPIField inputTypes = new ThirdAPIField();
        inputTypes.setName(GRAPHQL_INPUT_FIELDNAME);
        inputTypes.setType(GRAPHQL_STRING_TYPENAME);
        inputTypes.setKind(GRAPHQL_TYPEKIND_SCALAR);
        inputTypes.setIslist(true);
        ThirdAPIField outputTypes = new ThirdAPIField();
        outputTypes.setName(GRAPHQL_OUTPUT_FIELDNAME);
        outputTypes.setType(GRAPHQL_STRING_TYPENAME);
        outputTypes.setKind(GRAPHQL_TYPEKIND_SCALAR);
        outputTypes.setIslist(true);
        ThirdAPIField enumValues = new ThirdAPIField();
        enumValues.setName(GRAPHQL_ENUM_FIELDNAME);
        enumValues.setType(GRAPHQL_STRING_TYPENAME);
        enumValues.setKind(GRAPHQL_TYPEKIND_SCALAR);
        enumValues.setIslist(true);
        outputFieldsMap.put(GRAPHQL_QUERYAPI_FIELDNAME,queryAPI);
        outputFieldsMap.put(GRAPHQL_MUTATIONAPI_FIELDNAME,mutationAPI);
        outputFieldsMap.put(GRAPHQL_SUBSCRIPTIONAPI_FIELDNAME,subscriptionAPI);
        outputFieldsMap.put(GRAPHQL_INPUT_FIELDNAME,inputTypes);
        outputFieldsMap.put(GRAPHQL_OUTPUT_FIELDNAME,outputTypes);
        outputFieldsMap.put(GRAPHQL_ENUM_FIELDNAME,enumValues);
        return outputFieldsMap;
    }
}
