package com.easygql.dao.postgres;

import com.alibaba.fastjson.JSONObject;
import com.easygql.dao.One2OneRelationCreater;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class PostgreSqlOne2OneAdd implements One2OneRelationCreater{
    private SchemaData schemaData;
    private String schemaID;
    private RelationField relationField;
    private String fromSql;
    private String fromResetSql;
    private String toSql;
    private List<String> fieldSequence;
    private String insertSql;
    private ObjectTypeMetaData fromObject;
    private ObjectTypeMetaData toObject;
    @Override
    public void Init(SchemaData schemaData, String schemaID, RelationField relationField) {
        this.schemaData = schemaData;
        this.schemaID = schemaID;
        this.relationField = relationField;
        this.fieldSequence = new ArrayList<>();
        this.insertSql = PostgreSqlInserter.insertSqlConstruct(schemaData,relationField.getToobject(),fieldSequence);
        fromObject = schemaData.getObjectMetaData().get(relationField.getFromobject());
        toObject = schemaData.getObjectMetaData().get(relationField.getToobject());
        this.fromSql = " update "+schemaData.getObjectMetaData().get(relationField.getToobject()).getTableName()+" set "+POSTGRES_COLUMNNAME_PREFIX+relationField.getTofield()+"=$1 where "+POSTGRES_COLUMNNAME_PREFIX+POSTGRES_ID_FIELD+"=$2 ";
        this.fromResetSql = " update "+schemaData.getObjectMetaData().get(relationField.getToobject()).getTableName()+" set "+POSTGRES_COLUMNNAME_PREFIX+relationField.getTofield()+"=null where "+POSTGRES_COLUMNNAME_PREFIX+relationField.getTofield()+"=$1 ";;
        this.toSql =
                " update "
                        + schemaData.getObjectMetaData().get(relationField.getToobject()).getTableName()
                        + " set  "+POSTGRES_COLUMNNAME_PREFIX+relationField.getTofield()+"= $1 where "
                        + POSTGRES_COLUMNNAME_PREFIX
                        + POSTGRES_ID_FIELD
                        + "= $2 ";
    }

    @Override
    public CompletableFuture<Object> doAdd(@NonNull String srcID, Object targetObject) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                if(null!=throwable) {
                    if(log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_FROM_ID,srcID);
                        errorMap.put(GRAPHQL_TO_OBJECT,targetObject);
                        log.error("{}", LogData.getErrorLog("E10004",errorMap,throwable));
                    }
                    future.completeExceptionally(new BusinessException("E10004"));
                } else {
                    Transaction tx=sqlConnection.begin();
                    sqlConnection.preparedQuery(fromResetSql,Tuple.of(srcID),resetHandler->{
                        if(resetHandler.succeeded()) {
                            Tuple tuple = Tuple.tuple();
                            Map targetMap = HashMap.class.cast(targetObject);
                            targetMap.put(relationField.getTofield(),srcID);
                            List idList = new ArrayList();
                            for(String field:fieldSequence) {
                                if(null==targetMap.get(field)) {
                                    tuple.addValue(null);
                                } else {
                                    Object objectValue = targetMap.get(field);
                                    String fieldType = toObject.getFields().get(field);
                                    if(null==fieldType) {
                                        throw  new BusinessException("E10092");
                                    }
                                    if(fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
                                        ScalarFieldInfo scalarFieldInfo = toObject.getScalarFieldData().get(field);
                                        if(scalarFieldInfo.getType().equals(GRAPHQL_OBJECT_TYPENAME)||scalarFieldInfo.getType().equals(GRAPHQL_JSON_TYPENAME)) {
                                            if(scalarFieldInfo.isIslist()) {
                                                List<Object> objectListData = (List<Object>) objectValue;
                                                JsonArray tmpJsonArray = new JsonArray();
                                                for(Object tmpObjData:objectListData) {
                                                    JsonObject jsonObject = new JsonObject(JSONObject.toJSONString(tmpObjData));
                                                    tmpJsonArray.add(jsonObject);
                                                }
                                                tuple.addValue(tmpJsonArray);
                                            } else {
                                                JsonObject jsonObject = new JsonObject(Json.encode(objectValue));
                                                tuple.addValue(jsonObject);
                                            }
                                        } else {
                                            if(scalarFieldInfo.isIslist()) {
                                                JsonArray tmpJsonArray = new JsonArray();
                                                List<Object> objectListData = (List<Object>) objectValue;
                                                for(Object tmpObjData:objectListData) {
                                                    tmpJsonArray.add(tmpObjData);
                                                }
                                                tuple.addValue(tmpJsonArray);
                                            } else {
                                                tuple.addValue(objectValue);
                                            }
                                        }
                                    } else if(fieldType.equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
                                        EnumField enumField = toObject.getEnumFieldData().get(field);
                                        if(enumField.isIslist()){
                                            List<String> objStrListData = (List<String>)objectValue;
                                            JsonArray tmpJsonArray = new JsonArray();
                                            for(String tmpStr:objStrListData) {
                                                tmpJsonArray.add(tmpStr);
                                            }
                                            tuple.addValue(tmpJsonArray);
                                        } else {
                                            tuple.addValue(objectValue);
                                        }
                                    } else {
                                        tuple.addValue(objectValue);
                                    }
                                }
                            }
                            idList.add(targetMap.get(GRAPHQL_ID_FIELDNAME));
                            sqlConnection.preparedQuery(insertSql,tuple,insertHandler->{
                                if(insertHandler.succeeded()) {
                                    tx.commit(transactionHandler->{
                                        if(transactionHandler.succeeded()) {
                                            HashMap resultMap = new HashMap();
                                            resultMap.put(GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME,idList);
                                            future.complete(resultMap);
                                        } else {
                                            if(log.isErrorEnabled()) {
                                                HashMap errorMap = new HashMap();
                                                errorMap.put(GRAPHQL_FROM_ID,srcID);
                                                errorMap.put(GRAPHQL_TO_OBJECT,targetObject);
                                                log.error("{}", LogData.getErrorLog("E10004",errorMap,transactionHandler.cause()));
                                            }
                                            sqlConnection.close();
                                            future.completeExceptionally(new BusinessException("E10004"));
                                        }
                                    });
                                } else {
                                    if(log.isErrorEnabled()) {
                                        HashMap errorMap = new HashMap();
                                        errorMap.put(GRAPHQL_FROM_ID,srcID);
                                        errorMap.put(GRAPHQL_TO_OBJECT,targetObject);
                                        log.error("{}", LogData.getErrorLog("E10004",errorMap,insertHandler.cause()));
                                    }
                                    sqlConnection.close();
                                    future.completeExceptionally(new BusinessException("E10004"));
                                }
                            });
                        } else {
                            if(log.isErrorEnabled()) {
                                HashMap errorMap = new HashMap();
                                errorMap.put(GRAPHQL_FROM_ID,srcID);
                                errorMap.put(GRAPHQL_TO_OBJECT,targetObject);
                                log.error("{}", LogData.getErrorLog("E10004",errorMap,resetHandler.cause()));
                            }
                            future.completeExceptionally(new BusinessException("E10004"));
                            sqlConnection.close();
                        }
                    });
                }
            });
        });
        return future;
    }

    @Override
    public CompletableFuture<Object> doAddByID(@NonNull String srcID, String destID) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                if(null!=throwable) {
                    if(log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_FROM_ID,srcID);
                        errorMap.put(GRAPHQL_TO_ID,destID);
                        log.error("{}", LogData.getErrorLog("E10004",errorMap,throwable));
                    }
                    future.completeExceptionally(new BusinessException("E10004"));
                } else {
                    Transaction tx=sqlConnection.begin();
                    sqlConnection.preparedQuery(fromResetSql,Tuple.of(srcID),resetHandler->{
                        if(resetHandler.succeeded()) {
                            List idList = new ArrayList();
                            idList.add(destID);
                            sqlConnection.preparedQuery(fromSql,Tuple.of(srcID).addString(destID),insertHandler->{
                                if(insertHandler.succeeded()) {
                                    tx.commit(tranasactionHandler->{
                                        if(tranasactionHandler.succeeded()) {
                                            HashMap resultMap = new HashMap();
                                            resultMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME,1L);
                                            resultMap.put(GRAPHQL_IDLIST_FIELDNAME,idList);
                                            future.complete(resultMap);
                                            tx.close();
                                            sqlConnection.close();
                                        } else {
                                            if(log.isErrorEnabled()) {
                                                HashMap errorMap = new HashMap();
                                                errorMap.put(GRAPHQL_FROM_ID,srcID);
                                                errorMap.put(GRAPHQL_TO_ID,destID);
                                                log.error("{}", LogData.getErrorLog("E10004",errorMap,tranasactionHandler.cause()));
                                            }
                                            tx.close();
                                            sqlConnection.close();
                                            future.completeExceptionally(new BusinessException("E10004"));
                                        }
                                    });
                                } else {
                                    if(log.isErrorEnabled()) {
                                        HashMap errorMap = new HashMap();
                                        errorMap.put(GRAPHQL_FROM_ID,srcID);
                                        errorMap.put(GRAPHQL_TO_ID,destID);
                                        log.error("{}", LogData.getErrorLog("E10004",errorMap,insertHandler.cause()));
                                    }
                                    tx.close();
                                    sqlConnection.close();
                                    future.completeExceptionally(new BusinessException("E10004"));
                                }
                            });
                        } else {
                            if(log.isErrorEnabled()) {
                                HashMap errorMap = new HashMap();
                                errorMap.put(GRAPHQL_FROM_ID,srcID);
                                errorMap.put(GRAPHQL_TO_ID,destID);
                                log.error("{}", LogData.getErrorLog("E10004",errorMap,resetHandler.cause()));
                            }
                            tx.close();
                            sqlConnection.close();
                            future.completeExceptionally(new BusinessException("E10004"));
                        }
                    });
                }
            });
        });
        return future;
    }

    @Override
    public CompletableFuture<Object> toAdd(@NonNull String destID, String srcID) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                if(null!=throwable) {
                    future.completeExceptionally(new BusinessException("E10004"));
                    if(log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_FROM_ID,srcID);
                        errorMap.put(GRAPHQL_TO_ID,destID);
                        log.error("{}", LogData.getErrorLog("E10004",errorMap,throwable));
                    } else {
                        sqlConnection.preparedQuery(toSql,Tuple.of(srcID).addString(destID),addHandler->{
                            if(addHandler.succeeded()) {
                                HashMap resultMap = new HashMap();
                                resultMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME,1L);
                                List idList = new ArrayList();
                                idList.add(srcID);
                                resultMap.put(GRAPHQL_IDLIST_FIELDNAME,idList);
                                future.complete(resultMap);
                            }  else {
                                future.completeExceptionally(new BusinessException("E10004"));
                                if(log.isErrorEnabled()) {
                                    HashMap errorMap = new HashMap();
                                    errorMap.put(GRAPHQL_FROM_ID,srcID);
                                    errorMap.put(GRAPHQL_TO_ID,destID);
                                    log.error("{}", LogData.getErrorLog("E10004",errorMap,addHandler.cause()));
                                }
                            }
                            sqlConnection.close();
                        });
                    }
                }
            });
        });
        return future;
    }
}
