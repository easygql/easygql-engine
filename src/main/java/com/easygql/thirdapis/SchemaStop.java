package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.util.GraphQLCache;
import com.easygql.util.ThirdAPIField;
import com.easygql.util.ThirdAPIInput;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019/12/18 21:26
 */
@EasyGQLThirdAPI("SchemaStop")
@Slf4j
public class SchemaStop extends  ThirdAPI {


    @Override
    public Object doWork(ThirdAPIInput thirdAPIInput) {
        return CompletableFuture.supplyAsync(
                () -> {
                    GraphQLCache.remove(String.class.cast(thirdAPIInput.getRunTimeInfo().get(GRAPHQL_SCHEMAID_FIELDNAME)));
                    HashMap idEq = new HashMap();
                    idEq.put(GRAPHQL_FILTER_EQ_OPERATOR,thirdAPIInput.getRunTimeInfo().get(GRAPHQL_SCHEMAID_FIELDNAME));
                    HashMap idField = new HashMap();
                    idField.put(GRAPHQL_ID_FIELDNAME,idEq);
                    HashMap whereInput = new HashMap();
                    whereInput.put(GRAPHQL_FILTER_FILTER_OPERATOR,idField);
                    HashMap updateField = new HashMap();
                    updateField.put(GRAPHQL_SCHEMASTATUS_FIELDNAME,SCHEMA_STATUS_STOPPED);
                    GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT).getObjectDaoMap().get(GRAPHQL_SCHEMA_TYPENAME).getDataupdater().updateWhere(whereInput,updateField,"update",null);
                    HashMap resultMap = new HashMap();
                    resultMap.put(GRAPHQL_OPERATION_RESULT_NAME,true);
                    return resultMap;
                });
    }

    @Override
    public HashMap<String, ThirdAPIField> inputFields() {
        HashMap<String, ThirdAPIField> inputFieldsMap = new HashMap<>();
        ThirdAPIField schemaIDField = new ThirdAPIField();
        schemaIDField.setDescription("Schema ID");
        schemaIDField.setNotnull(true);
        schemaIDField.setName(GRAPHQL_SCHEMAID_FIELDNAME);
        schemaIDField.setType(GRAPHQL_STRING_TYPENAME);
        schemaIDField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        inputFieldsMap.put(GRAPHQL_SCHEMAID_FIELDNAME,schemaIDField);
        return inputFieldsMap;
    }

    @Override
    public HashMap<String, ThirdAPIField> outputFields() {
        HashMap<String, ThirdAPIField> outputFieldsMap = new HashMap<>();
        ThirdAPIField operationResultField = new ThirdAPIField();
        operationResultField.setDescription("Operation Result");
        operationResultField.setNotnull(true);
        operationResultField.setName(GRAPHQL_OPERATION_RESULT_NAME);
        operationResultField.setType(GRAPHQL_BOOLEAN_TYPENAME);
        operationResultField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        outputFieldsMap.put(GRAPHQL_OPERATION_RESULT_NAME,operationResultField);
        return outputFieldsMap;
    }
}
