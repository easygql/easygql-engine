package com.easygql.dao.postgres;

import com.easygql.dao.One2OneRelationCreater;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        this.insertSql = PostgreSqlInserter.insertSqlConstruct(schemaData,relationField.getToObject(),fieldSequence);
        fromObject = schemaData.getObjectMetaData().get(relationField.getFromObject());
        toObject = schemaData.getObjectMetaData().get(relationField.getToObject());
        this.fromSql = " update "+schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()+" set "+POSTGRES_COLUMNNAME_PREFIX+relationField.getToField()+"=$1 where "+POSTGRES_COLUMNNAME_PREFIX+POSTGRES_ID_FIELD+"=$2 ";
        this.fromResetSql = " update "+schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()+" set "+POSTGRES_COLUMNNAME_PREFIX+relationField.getToField()+"=null where "+POSTGRES_COLUMNNAME_PREFIX+relationField.getToField()+"=$1 ";;
        this.toSql =
                " update "
                        + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
                        + " set  "+POSTGRES_COLUMNNAME_PREFIX+relationField.getToField()+"= $1 where "
                        + POSTGRES_COLUMNNAME_PREFIX
                        + POSTGRES_ID_FIELD
                        + "= $2 ";
    }

    @Override
    public CompletableFuture<Object> doAdd(@NonNull String srcID, Object targetObject) {
        CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid())
              .whenCompleteAsync(
                  (sqlConnection, throwable) -> {
                    if (null != throwable) {
                      if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_FROM_ID, srcID);
                        errorMap.put(GRAPHQL_TO_OBJECT, targetObject);
                        log.error("{}", LogData.getErrorLog("E10004", errorMap, throwable));
                      }
                      future.completeExceptionally(new BusinessException("E10004"));
                    } else {
                      Transaction tx = sqlConnection.begin();
                      sqlConnection.preparedQuery(
                          fromResetSql,
                          Tuple.of(srcID),
                          resetHandler -> {
                            if (resetHandler.succeeded()) {
                              Tuple tuple = Tuple.tuple();
                              Map targetMap = HashMap.class.cast(targetObject);
                              targetMap.put(relationField.getToField(), srcID);
                              List idList = new ArrayList();
                              for (String field : fieldSequence) {
                                if (null == targetMap.get(field)) {
                                  tuple.addValue(null);
                                } else {
                                  Object objectValue = targetMap.get(field);
                                  if (toObject
                                      .getFields()
                                      .get(field)
                                      .equals(GRAPHQL_ENUMFIELD_TYPENAME)) {
                                    if (toObject
                                        .getEnumFieldData()
                                        .get(field)
                                        .isList()) {
                                      List<String> tmpEnumList = (List<String>) objectValue;
                                      tuple.addStringArray(
                                          tmpEnumList.toArray(new String[tmpEnumList.size()]));
                                    } else {
                                      tuple.addValue(objectValue);
                                    }
                                  } else if (toObject
                                      .getFields()
                                      .get(field)
                                      .equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
                                    ScalarFieldInfo scalarFieldInfo =
                                        toObject.getScalarFieldData().get(field);
                                    if (scalarFieldInfo.isList()) {
                                      switch (scalarFieldInfo.getType()) {
                                        case GRAPHQL_ID_TYPENAME:
                                        case GRAPHQL_CHAR_TYPENAME:
                                        case GRAPHQL_STRING_TYPENAME:
                                        case GRAPHQL_URL_TYPENAME:
                                        case GRAPHQL_EMAIL_TYPENAME:
                                        case GRAPHQL_BYTE_TYPENAME:
                                          List<String> tmpScalarStringList =
                                              (List<String>) objectValue;
                                          tuple.addStringArray(
                                              tmpScalarStringList.toArray(
                                                  new String[tmpScalarStringList.size()]));
                                          break;
                                        case GRAPHQL_INT_TYPENAME:
                                          List<Integer> tmpScalarIntegerList =
                                              (List<Integer>) objectValue;
                                          tuple.addIntegerArray(
                                              tmpScalarIntegerList.toArray(
                                                  new Integer[tmpScalarIntegerList.size()]));
                                          break;
                                        case GRAPHQL_BOOLEAN_TYPENAME:
                                          List<Boolean> tmpScalarBooleanList =
                                              (List<Boolean>) objectValue;
                                          tuple.addBooleanArray(
                                              tmpScalarBooleanList.toArray(
                                                  new Boolean[tmpScalarBooleanList.size()]));
                                          break;
                                        case GRAPHQL_LONG_TYPENAME:
                                        case GRAPHQL_BIGINTEGER_TYPENAME:
                                          List<Long> tmpScalarLongList = (List<Long>) objectValue;
                                          tuple.addLongArray(
                                              tmpScalarLongList.toArray(
                                                  new Long[tmpScalarLongList.size()]));
                                          break;
                                        case GRAPHQL_SHORT_TYPENAME:
                                          List<Short> tmpScalarShortList =
                                              (List<Short>) objectValue;
                                          tuple.addShortArray(
                                              tmpScalarShortList.toArray(
                                                  new Short[tmpScalarShortList.size()]));
                                          break;
                                        case GRAPHQL_DATE_TYPENAME:
                                        case GRAPHQL_DATETIME_TYPENAME:
                                        case GRAPHQL_CREATEDAT_TYPENAME:
                                        case GRAPHQL_LASTUPDATE_TYPENAME:
                                          List<OffsetDateTime> tmpScalarDateList =
                                              (List<OffsetDateTime>) objectValue;
                                          tuple.addOffsetDateTimeArray(
                                              tmpScalarDateList.toArray(
                                                  new OffsetDateTime[tmpScalarDateList.size()]));
                                          break;
                                        case GRAPHQL_FLOAT_TYPENAME:
                                          List<Float> tmpScalarFloatList =
                                              (List<Float>) objectValue;
                                          tuple.addFloatArray(
                                              tmpScalarFloatList.toArray(
                                                  new Float[tmpScalarFloatList.size()]));
                                          break;
                                        case GRAPHQL_BIGDECIMAL_TYPENAME:
                                          List<BigDecimal> tmpScalarBigDecimalist =
                                              (List<BigDecimal>) objectValue;
                                          tuple.addValues(
                                              tmpScalarBigDecimalist.toArray(
                                                  new Float[tmpScalarBigDecimalist.size()]));
                                          break;
                                        default:
                                          List tmpObjectValue = (List) objectValue;
                                          tuple.addValues(tmpObjectValue.toArray());
                                      }
                                    } else {
                                      tuple.addValue(objectValue);
                                    }
                                  }
                                }
                              }
                              idList.add(targetMap.get(GRAPHQL_ID_FIELDNAME));
                              sqlConnection.preparedQuery(
                                  insertSql,
                                  tuple,
                                  insertHandler -> {
                                    if (insertHandler.succeeded()) {
                                      tx.commit(
                                          transactionHandler -> {
                                            if (transactionHandler.succeeded()) {
                                              HashMap resultMap = new HashMap();
                                              resultMap.put(
                                                  GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                              future.complete(resultMap);
                                            } else {
                                              if (log.isErrorEnabled()) {
                                                HashMap errorMap = new HashMap();
                                                errorMap.put(GRAPHQL_FROM_ID, srcID);
                                                errorMap.put(GRAPHQL_TO_OBJECT, targetObject);
                                                log.error(
                                                    "{}",
                                                    LogData.getErrorLog(
                                                        "E10004",
                                                        errorMap,
                                                        transactionHandler.cause()));
                                              }
                                              sqlConnection.close();
                                              future.completeExceptionally(
                                                  new BusinessException("E10004"));
                                            }
                                          });
                                    } else {
                                      if (log.isErrorEnabled()) {
                                        HashMap errorMap = new HashMap();
                                        errorMap.put(GRAPHQL_FROM_ID, srcID);
                                        errorMap.put(GRAPHQL_TO_OBJECT, targetObject);
                                        log.error(
                                            "{}",
                                            LogData.getErrorLog(
                                                "E10004", errorMap, insertHandler.cause()));
                                      }
                                      sqlConnection.close();
                                      future.completeExceptionally(new BusinessException("E10004"));
                                    }
                                  });
                            } else {
                              if (log.isErrorEnabled()) {
                                HashMap errorMap = new HashMap();
                                errorMap.put(GRAPHQL_FROM_ID, srcID);
                                errorMap.put(GRAPHQL_TO_OBJECT, targetObject);
                                log.error(
                                    "{}",
                                    LogData.getErrorLog("E10004", errorMap, resetHandler.cause()));
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
