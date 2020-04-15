package com.easygql.dao.postgres;

import com.alibaba.fastjson.JSON;
import com.easygql.dao.DataInserter;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * PostgreSQL的数据插入工具类
 *
 * @author guofen
 * @date 2019/11/3 14:59
 */
@Slf4j
public class PostgreSqlInserter implements DataInserter {
    private String objectName;
    private SchemaData schemaData;
    private String schemaId;
    private String insertSQL;
    private List<String> fieldSequence;
    /**
     *
     * @param objectName
     * @param schemaData
     * @param schemaID
     */
    @Override
    public void Init(String objectName, SchemaData schemaData, String schemaID) {
        this.objectName = objectName;
        this.schemaData = schemaData;
        this.schemaId = schemaID;
        this.fieldSequence=new ArrayList<>();
        this.insertSQL= insertSqlConstruct(schemaData,objectName,this.fieldSequence);

    }


    /**
     * 插入doc对象
     *
     * @param resultInfo
     * @param conflictStrategy
     * @param selectionFields
     * @return
     */
    @Override
    public CompletableFuture<HashMap> insertDoc(@NonNull HashMap<String,HashMap> resultInfo, String conflictStrategy,HashMap<String,Object> selectionFields) {
        CompletableFuture<HashMap> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try{
                List<Tuple> tuples = new ArrayList<>();
                for(HashMap resultData:resultInfo.values()) {
                    Tuple tuple = Tuple.tuple();
                    for(String field:fieldSequence) {
                        if(null==resultData.get(field)) {
                            tuple.addValue(null);
                        } else {
                            Object objectValue = resultData.get(field);
                            if(objectValue instanceof  List ) {
                                tuple.addValue(JSON.toJSONString(objectValue));
                            } else {
                                tuple.addValue(objectValue);
                            }
                        }
                    }
                    tuples.add(tuple);
                }
                PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync(((connection, throwable) -> {
                    if(null!=throwable) {
                        if(log.isErrorEnabled()) {
                            HashMap detailMap = new HashMap();
                            detailMap.put(GRAPHQL_OBJECTS_ARGUMENT,resultInfo);
                            detailMap.put(GRAPHQL_CONFLICT_ARGUMENT,conflictStrategy);
                            log.error("{}",LogData.getErrorLog("E10014",detailMap,throwable));
                        }
                        future.completeExceptionally(new BusinessException("E10014"));
                    } else {
                        Transaction transaction = connection.begin();
                        connection.preparedBatch(insertSQL,tuples, handler->{
                            if(handler.succeeded()) {
                                transaction.commit(transactionHandler->{
                                    if(transactionHandler.succeeded()) {
                                        HashMap resultMap = new HashMap(1);
                                        List<String> idList = new ArrayList<>();
                                        idList.addAll(resultInfo.keySet());
                                        resultMap.put(GRAPHQL_IDLIST_FIELDNAME,idList);
                                        resultMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME,idList.size());
                                        future.complete(resultMap);
                                    } else {
                                        if(log.isErrorEnabled()) {
                                            HashMap detailMap = new HashMap();
                                            detailMap.put(GRAPHQL_OBJECTS_ARGUMENT,resultInfo);
                                            detailMap.put(GRAPHQL_CONFLICT_ARGUMENT,conflictStrategy);
                                            log.error("{}",LogData.getErrorLog("E10007",detailMap,transactionHandler.cause()));
                                        }
                                        future.completeExceptionally(new BusinessException("E10007"));
                                    }
                                    transaction.close();
                                    connection.close();
                                });
                            }  else {
                                if(log.isErrorEnabled()) {
                                    HashMap detailMap = new HashMap();
                                    detailMap.put(GRAPHQL_OBJECTS_ARGUMENT,resultInfo);
                                    detailMap.put(GRAPHQL_CONFLICT_ARGUMENT,conflictStrategy);
                                    log.error("{}",LogData.getErrorLog("E10014",detailMap,handler.cause()));
                                }
                                future.completeExceptionally(new BusinessException("E10014"));
                                transaction.close();
                                connection.close();
                            }
                        });
                    }
                }));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }



    public static String insertSqlConstruct(SchemaData schemaData, String objectName, List<String> insertFields) {
        ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
        StringBuilder insertSQLTmp = new StringBuilder();
        StringBuilder valueSQLTmp = new StringBuilder();
        insertSQLTmp.append(" insert into ").append(objectTypeMetaData.getTableName()).append(" ( ");
        valueSQLTmp.append(" values ( ");
        int fieldLoc = 0;
        Iterator<Map.Entry<String,String>> iterator = objectTypeMetaData.getFields().entrySet().iterator();
        while(iterator.hasNext()) {
            boolean ifAdd=false;
            Map.Entry<String,String> entry=iterator.next();
            String fieldName = entry.getKey();
            String fieldType = entry.getValue();
            if(fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)||fieldType.equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
                ifAdd=true;
            } else if(fieldType.equals(GRAPHQL_FROMRELATION_TYPENAME)) {
                RelationField relationField = objectTypeMetaData.getFromRelationFieldData().get(fieldName);
                if (relationField.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)) {
                    ifAdd=true;
                }
            } else {
                RelationField relationField = objectTypeMetaData.getToRelationFieldData().get(fieldName);
                if (relationField.getRelationtype().equals(GRAPHQL_ONE2ONE_NAME)||relationField.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)) {
                    ifAdd=true;
                }
            }
            if(ifAdd) {
                if (0 != fieldLoc) {
                    insertSQLTmp.append(",");
                    valueSQLTmp.append(",");
                }
                fieldLoc++;
                insertSQLTmp.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldName);
                valueSQLTmp.append("$").append(fieldLoc);
                insertFields.add(fieldName);
            }
        }
        insertSQLTmp.append(" ) ");
        valueSQLTmp.append(") ");
        return insertSQLTmp.append(valueSQLTmp).toString();
    }


}

