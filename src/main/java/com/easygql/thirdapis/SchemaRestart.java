package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.DataSelecter;
import com.easygql.exception.BusinessException;
import com.easygql.util.GraphQLCache;
import com.easygql.util.LogData;
import com.easygql.util.ThirdAPIField;
import com.easygql.util.ThirdAPIInput;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.thirdapis.SchemaStart.startSchema;

@EasyGQLThirdAPI("SchemaRestart")
@Slf4j
public class SchemaRestart extends ThirdAPI {
    private static HashMap publishedSchemaSelecter = new HashMap();
    static {
        publishedSchemaSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
        HashMap publishedSchema = new HashMap();
        publishedSchemaSelecter.put(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME, publishedSchema);
        publishedSchema.put(GRAPHQL_ID_FIELDNAME, 1);
        publishedSchema.put(GRAPHQL_SCHEMAOBJECT_FIELDNAME, 1);
    }


    @Override
  public Object doWork(ThirdAPIInput thirdAPIInput) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
            try {
                String schemaID =
                        String.class.cast(thirdAPIInput.getRunTimeInfo().get(GRAPHQL_SCHEMAID_FIELDNAME));
                DataSelecter schemaSelecter = GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT).getObjectDaoMap().get(GRAPHQL_SCHEMA_TYPENAME).getDataselecter();
                GraphQLCache.remove(schemaID);
                HashMap eqMap = new HashMap();
                eqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,schemaID);
                HashMap idMap = new HashMap();
                idMap.put(GRAPHQL_ID_FIELDNAME,eqMap);
                HashMap filterMap = new HashMap();
                filterMap.put(GRAPHQL_FILTER_FILTER_OPERATOR,idMap);
                schemaSelecter.getSingleDoc(filterMap,publishedSchemaSelecter)
                        .whenComplete(
                                (schemaInfo, ex) -> {
                                    if (null != ex ) {
                                        if (log.isErrorEnabled()) {
                                            HashMap errorMap = new HashMap();
                                            errorMap.put(GRAPHQL_ARGUMENTS_FIELDNAME, thirdAPIInput.getRunTimeInfo());
                                            log.error("{}", LogData.getErrorLog("E10045", errorMap, (Throwable) ex));
                                        }
                                        future.completeExceptionally(new BusinessException("E10045"));
                                    } if(null==schemaInfo) {
                                        future.completeExceptionally(new BusinessException("E10045"));
                                    } else {
                                        try {
                                            HashMap schemaInfoMap = (HashMap) schemaInfo;
                                            HashMap publishedSchemaInfo =
                                                    (HashMap) schemaInfoMap.get(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME);
                                            if (null == publishedSchemaInfo) {
                                                future.completeExceptionally(new BusinessException("E10087"));
                                            } else {
                                                Object schemaDataJson =
                                                        publishedSchemaInfo.get(GRAPHQL_SCHEMAOBJECT_FIELDNAME);
                                                if (null == schemaDataJson) {
                                                    future.completeExceptionally(new BusinessException("E10088"));
                                                } else {
                                                    startSchema(schemaID)
                                                            .whenComplete(
                                                                    (startResult, startEx) -> {
                                                                        if (null != startEx) {
                                                                            if (log.isErrorEnabled()) {
                                                                                HashMap errorMap = new HashMap();
                                                                                errorMap.put(
                                                                                        GRAPHQL_ARGUMENTS_FIELDNAME,
                                                                                        thirdAPIInput.getRunTimeInfo());
                                                                                log.error(
                                                                                        "{}", LogData.getErrorLog("E10046", errorMap, startEx));
                                                                            }
                                                                            future.completeExceptionally(startEx);
                                                                        } else {
                                                                            HashMap resultMap = new HashMap();
                                                                            resultMap.put(GRAPHQL_OPERATION_RESULT_NAME, startResult);
                                                                            future.complete(resultMap);
                                                                        }
                                                                    });
                                                }
                                            }
                                        } catch (Exception e) {
                                            future.completeExceptionally(e);
                                        }
                                    }
                                });
            } catch ( Exception e) {
                future.completeExceptionally(e);
            }
        });
    return future;
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
    inputFieldsMap.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaIDField);
    return inputFieldsMap;
  }

  @Override
  public HashMap<String, ThirdAPIField> outputFields() {
    HashMap<String, ThirdAPIField> outputFieldsMap = new HashMap<>();
    ThirdAPIField operationResultField = new ThirdAPIField();
    operationResultField.setDescription("Operation Result");
    operationResultField.setNotnull(true);
    operationResultField.setName(GRAPHQL_OPERATION_RESULT_NAME);
    operationResultField.setType(GRAPHQL_STRING_TYPENAME);
    operationResultField.setKind(GRAPHQL_TYPEKIND_SCALAR);
    outputFieldsMap.put(GRAPHQL_OPERATION_RESULT_NAME, operationResultField);
    return outputFieldsMap;
  }
}
