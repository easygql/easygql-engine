package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.util.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
@EasyGQLThirdAPI(value = "ThirdAPIMany",type = APIType.QUERY)
@Slf4j
public class ThirdAPIManyQuery extends ThirdAPI {
    @Override
    public Object doWork(ThirdAPIInput thirdAPIInput) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                Map thirdAPIMap = ThirdAPIPool.getAllThirdAPI();
                HashMap finalResultMap = new HashMap();
                finalResultMap.put(GRAPHQL_THIRD_API_FIELDNAME,thirdAPIMap);
                future.complete(finalResultMap);
            } catch ( Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public HashMap<String, ThirdAPIField> inputFields() {
        return null;
    }

    @Override
    public HashMap<String, ThirdAPIField> outputFields() {
        HashMap<String, ThirdAPIField> outputFieldMap = new HashMap<>();
        ThirdAPIField apiResult = new ThirdAPIField();
        apiResult.setName(GRAPHQL_THIRD_API_FIELDNAME);
        apiResult.setType(GRAPHQL_OBJECT_TYPENAME);
        apiResult.setKind(GRAPHQL_TYPEKIND_SCALAR);
        outputFieldMap.put(GRAPHQL_THIRD_API_FIELDNAME,apiResult);
        return outputFieldMap;
    }
    public List<HashMap> getAPIInfo(Map<String,ThirdAPI> thirdAPIMap) {
        List<HashMap> resultList = new ArrayList<>();
        Iterator<Map.Entry<String,ThirdAPI>> iterator = thirdAPIMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String,ThirdAPI> entry = iterator.next();
            HashMap tmpMap = new HashMap();
            tmpMap.put(GRAPHQL_APINAME_FIELDNAME,entry.getKey());
            ThirdAPI thirdAPI = entry.getValue();
            HashMap<String, ThirdAPIField> inputFields =thirdAPI.inputFields();
            HashMap<String, ThirdAPIField> outputFields = thirdAPI.outputFields();
            HashMap<String,String> inputFieldsMap = new HashMap<>();
            inputFields.forEach((fieldName,scalarField)->{
                String typeName = scalarField.getType();
                if(scalarField.isIslist()) {
                    typeName="["+typeName+"]";
                }
                if(scalarField.isNotnull()) {
                    typeName=typeName+"!";
                }
                inputFieldsMap.put(fieldName,typeName);
            });
            HashMap<String,String> outputFieldsMap = new HashMap<>();
            outputFields.forEach((fieldName,scalarField)->{
                String typeName = scalarField.getType();
                if(scalarField.isIslist()) {
                    typeName="["+typeName+"]";
                }
                outputFieldsMap.put(fieldName,typeName);
            });
            tmpMap.put(GRAPHQL_INPUT_FIELDNAME,inputFieldsMap);
            tmpMap.put(GRAPHQL_OUTPUT_FIELDNAME,outputFieldsMap);
            resultList.add(tmpMap);
        }
        return resultList;
    }
}
