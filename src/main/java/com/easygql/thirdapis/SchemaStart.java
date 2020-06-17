package com.easygql.thirdapis;

import com.alibaba.fastjson.JSONObject;
import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.*;
import com.easygql.exception.BusinessException;
import com.easygql.graphql.datafetcher.GetTypeRuntimeWiring;
import com.easygql.util.*;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019/12/18 21:26
 */
@Slf4j
@EasyGQLThirdAPI("SchemaStart")
public class SchemaStart extends  ThirdAPI {
    private static final SchemaParser schemaParser = new SchemaParser();
    private static final SchemaGenerator schemagenerator = new SchemaGenerator();
    private static HashMap publishedSchemaSelecter = new HashMap();
    static {
        publishedSchemaSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
        HashMap publishedSchema = new HashMap();
        publishedSchemaSelecter.put(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME, publishedSchema);
        publishedSchema.put(GRAPHQL_ID_FIELDNAME, 1);
        publishedSchema.put(GRAPHQL_SCHEMAOBJECT_FIELDNAME, 1);
        HashMap triggerInfo = new HashMap();
        triggerInfo.put(GRAPHQL_ID_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_NAME_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_TYPENAME_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_HEADERS_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_OK_STATUS_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_PAYLOADFORMATTER_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_PAYLOADARGS_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_RETRY_TIMES_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_WEBHOOK_URL_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_STARTDATE_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_EXPIREDATE_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_DESCRIPTION_FIELDNAME,1);
        triggerInfo.put(GRAPHQL_EVENTTYPE_FIELDNAME,1);
        publishedSchemaSelecter.put(GRAPHQL_TRIGGERS_FIELDNAME,triggerInfo);
    }
    public static CompletableFuture<Boolean> startSchema(SchemaData schemaData, String schemaID,List<Trigger> triggerList) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        EasyGQL easyGQL = new EasyGQL();
                        easyGQL.setSchemaData(schemaData);
                        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schemaData.getIdl());
                        RuntimeWiring schemaWireing =
                                GetTypeRuntimeWiring.getSchemaWiring(schemaData, schemaID);
                        GraphQLSchema graphQLSchema =
                                schemagenerator.makeExecutableSchema(typeDefinitionRegistry, schemaWireing);
                        GraphQL graphQL =
                                GraphQL.newGraphQL(graphQLSchema)
                                        .queryExecutionStrategy(new AsyncExecutionStrategy())
                                        .mutationExecutionStrategy(new AsyncExecutionStrategy())
                                        .subscriptionExecutionStrategy(new AsyncExecutionStrategy())
                                        .build();
                        easyGQL.setGraphQL(graphQL);
                        String databaseKind = schemaData.getDatabasekind();
                        HashMap<String,ObjectDao> daoHashMap = new HashMap<>();
                        HashMap<String,ObjectTypeMetaData> typeMetaDataHashMap = easyGQL.getSchemaData().getObjectMetaData();
                        typeMetaDataHashMap.forEach((typeName,typeMetaData)->{
                            ObjectDao objectDao = new ObjectDao();
                            DataInserter dataInserter = DaoFactory.getInsertDao(databaseKind);
                            dataInserter.Init(typeName,schemaData,schemaID);
                            objectDao.setDatainserter(dataInserter);
                            DataDeleter dataDeleter = DaoFactory.getDeleteDao(databaseKind);
                            dataDeleter.Init(typeName,schemaData,schemaID);
                            objectDao.setDatadeleter(dataDeleter);
                            DataUpdater dataUpdater = DaoFactory.getUpdaterDao(databaseKind);
                            dataUpdater.Init(typeName,schemaData,schemaID);
                            objectDao.setDataupdater(dataUpdater);
                            DataSelecter dataSelecter = DaoFactory.getSelecterDao(databaseKind);
                            dataSelecter.Init(typeName,schemaData,schemaID);
                            objectDao.setDataselecter(dataSelecter);
                            DataSub dataSub = DaoFactory.getDataSub(databaseKind);
                            dataSub.Init(typeName,schemaData,schemaID);
                            objectDao.setDatasub(dataSub);
                            HashMap<String,RelationObjectCreator> relationAddHashMap = new HashMap<>();
                            HashMap<String,RelationRemover> relationRemoverHashMap = new HashMap<>();
                            if(null!=typeMetaData.getFromRelationFieldData()) {
                                typeMetaData.getFromRelationFieldData().forEach((fieldName,relationField)->{
                                    if(GRAPHQL_ONE2ONE_NAME.equals(relationField.getRelationType())) {
                                        One2OneRelationCreater one2OneRelationCreater = DaoFactory.getOne2OneRelationCreator(databaseKind);
                                        one2OneRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,one2OneRelationCreater);
                                        One2OneRelationRemover one2OneRelationRemover = DaoFactory.getOne2OneRelationRemover(databaseKind);
                                        one2OneRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,one2OneRelationRemover);
                                    } else if(GRAPHQL_ONE2MANY_NAME.equals(relationField.getRelationType())){
                                        One2ManyRelationCreater one2ManyRelationCreater = DaoFactory.getOne2ManyRelationCreator(databaseKind);
                                        one2ManyRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,one2ManyRelationCreater);
                                        One2ManyRelationRemover one2ManyRelationRemover = DaoFactory.getOne2ManyRelationRemover(databaseKind);
                                        one2ManyRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,one2ManyRelationRemover);
                                    } else if(GRAPHQL_MANY2ONE_NAME.equals(relationField.getRelationType())) {
                                        Many2OneRelationCreater many2OneRelationCreater = DaoFactory.getMany2OneRelationCreator(databaseKind);
                                        many2OneRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,many2OneRelationCreater);
                                        Many2OneRelationRemover many2OneRelationRemover = DaoFactory.getMany2OneRelationRemover(databaseKind);
                                        many2OneRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,many2OneRelationRemover);
                                    } else {
                                        Many2ManyRelationCreater many2ManyRelationCreater = DaoFactory.getMany2ManyRelationCreator(databaseKind);
                                        many2ManyRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,many2ManyRelationCreater);
                                        Many2ManyRelationRemover many2ManyRelationRemover = DaoFactory.getMany2ManyRelationRemover(databaseKind);
                                        many2ManyRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,many2ManyRelationRemover);
                                    }
                                });
                            }
                            if(null!=typeMetaData.getToRelationFieldData()) {
                                typeMetaData.getToRelationFieldData().forEach((fieldName,relationField)->{
                                    if(GRAPHQL_ONE2ONE_NAME.equals(relationField.getRelationType())) {
                                        One2OneRelationCreater one2OneRelationCreater = DaoFactory.getOne2OneRelationCreator(databaseKind);
                                        one2OneRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,one2OneRelationCreater);
                                        One2OneRelationRemover one2OneRelationRemover = DaoFactory.getOne2OneRelationRemover(databaseKind);
                                        one2OneRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,one2OneRelationRemover);
                                    } else if(GRAPHQL_ONE2MANY_NAME.equals(relationField.getRelationType())){
                                        One2ManyRelationCreater one2ManyRelationCreater = DaoFactory.getOne2ManyRelationCreator(databaseKind);
                                        one2ManyRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,one2ManyRelationCreater);
                                        One2ManyRelationRemover one2ManyRelationRemover = DaoFactory.getOne2ManyRelationRemover(databaseKind);
                                        one2ManyRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,one2ManyRelationRemover);
                                    } else if(GRAPHQL_MANY2ONE_NAME.equals(relationField.getRelationType())) {
                                        Many2OneRelationCreater many2OneRelationCreater = DaoFactory.getMany2OneRelationCreator(databaseKind);
                                        many2OneRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,many2OneRelationCreater);
                                        Many2OneRelationRemover many2OneRelationRemover = DaoFactory.getMany2OneRelationRemover(databaseKind);
                                        many2OneRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,many2OneRelationRemover);
                                    } else {
                                        Many2ManyRelationCreater many2ManyRelationCreater = DaoFactory.getMany2ManyRelationCreator(databaseKind);
                                        many2ManyRelationCreater.Init(schemaData,schemaID,relationField);
                                        relationAddHashMap.put(fieldName,many2ManyRelationCreater);
                                        Many2ManyRelationRemover many2ManyRelationRemover = DaoFactory.getMany2ManyRelationRemover(databaseKind);
                                        many2ManyRelationRemover.Init(schemaData,schemaID,relationField);
                                        relationRemoverHashMap.put(fieldName,many2ManyRelationRemover);
                                    }
                                });
                            }
                            objectDao.setRelation_add_Fields(relationAddHashMap);
                            objectDao.setRelation_remove_Fields(relationRemoverHashMap);
                            daoHashMap.put(typeName,objectDao);
                        });
                        easyGQL.setObjectDaoMap(daoHashMap);
                        TriggerDao triggerDao = DaoFactory.getTriggerDao(schemaData.getDatabasekind());
                        triggerDao.init(schemaData,schemaID);
                        easyGQL.setTriggerDao(triggerDao);
                        GraphQLCache.addGraphQL(schemaID, easyGQL);
                        if(null!=triggerList) {
                            for (Trigger trigger:triggerList) {
                                TriggerCache.addTrigger(schemaID,schemaData,trigger);
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        if(log.isErrorEnabled()) {
                            HashMap errorMap = new HashMap();
                            errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME,schemaID);
                            log.error("{}",LogData.getErrorLog("E10046",errorMap,e));
                        }
                        return false;
                    }
                });
    }

    public static CompletableFuture<Boolean> startSchema(String schemaID) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            HashMap eqMap = new HashMap();
            eqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,schemaID);
            HashMap idMap = new HashMap();
            idMap.put(GRAPHQL_ID_FIELDNAME,eqMap);
            HashMap filterMap = new HashMap();
            filterMap.put(GRAPHQL_FILTER_FILTER_OPERATOR,idMap);
            DataSelecter schemaSelecter = GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT).getObjectDaoMap().get(GRAPHQL_SCHEMA_TYPENAME).getDataselecter();
            schemaSelecter
                    .getSingleDoc(filterMap, publishedSchemaSelecter)
                    .whenComplete(
                            (schemaInfo, ex) -> {
                                if (null != ex || null == schemaInfo) {
                                    future.completeExceptionally(new BusinessException("E010086"));
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
                                                SchemaData schemaData =
                                                        JSONObject.parseObject(
                                                                JSONObject.toJSONString(schemaDataJson), SchemaData.class);
                                                List<Trigger> triggerList = JSONObject.parseArray(JSONObject.toJSONString(schemaInfoMap.get(GRAPHQL_TRIGGERS_FIELDNAME)),Trigger.class);
                                                startSchema(schemaData, schemaID,triggerList)
                                                        .whenComplete(
                                                                (result, startEx) -> {
                                                                    if (null != startEx) {
                                                                        future.completeExceptionally(startEx);
                                                                    } else {
                                                                        if (result) {
                                                                            HashMap idEq = new HashMap();
                                                                            idEq.put(GRAPHQL_FILTER_EQ_OPERATOR, schemaID);
                                                                            HashMap idField = new HashMap();
                                                                            idField.put(GRAPHQL_ID_FIELDNAME, idEq);
                                                                            HashMap whereInput = new HashMap();
                                                                            whereInput.put(GRAPHQL_FILTER_FILTER_OPERATOR, idField);
                                                                            HashMap updateField = new HashMap();
                                                                            updateField.put(
                                                                                    GRAPHQL_SCHEMASTATUS_FIELDNAME, SCHEMA_STATUS_RUNNING);
                                                                            GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT)
                                                                                    .getObjectDaoMap()
                                                                                    .get(GRAPHQL_SCHEMA_TYPENAME)
                                                                                    .getDataupdater()
                                                                                    .updateWhere(
                                                                                            whereInput, updateField, "update");
                                                                            future.complete(true);
                                                                        } else {
                                                                            future.complete(false);
                                                                        }
                                                                    }
                                                                });
                                            }
                                        }
                                    } catch (Exception e) {
                                        future.completeExceptionally(e);
                                    }
                                }
                            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
    @Override
    public Object doWork(ThirdAPIInput thirdAPIInput) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        try {
            String schemaID = String.class.cast(thirdAPIInput.getRunTimeInfo().get(GRAPHQL_SCHEMAID_FIELDNAME));
            startSchema(schemaID).whenComplete((startResult, startEx) -> {
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
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
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
        operationResultField.setType(GRAPHQL_STRING_TYPENAME);
        operationResultField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        outputFieldsMap.put(GRAPHQL_OPERATION_RESULT_NAME,operationResultField);
        return outputFieldsMap;
    }

}
