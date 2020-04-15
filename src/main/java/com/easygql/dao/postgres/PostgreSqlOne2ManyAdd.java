package com.easygql.dao.postgres;

import com.easygql.dao.One2ManyRelationCreater;

import com.easygql.exception.BusinessException;
import com.easygql.util.*;
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
public class PostgreSqlOne2ManyAdd implements One2ManyRelationCreater {
  private String schemaID;
  private SchemaData schemaData;
  private RelationField relationField;
  private List<String> fieldSequence;
  private String insertSql;
  private String fromSql;
  private String fromResetSql;
  private String toSql;
  private ObjectTypeMetaData fromObject;
  private ObjectTypeMetaData toObject;
  private String resetSql;

  @Override
  public void Init(SchemaData schemaData, String schemaID, RelationField relationField) {
    this.schemaID = schemaID;
    this.schemaData = schemaData;
    this.relationField = relationField;
    this.fieldSequence = new ArrayList<>();
    this.insertSql =
        PostgreSqlInserter.insertSqlConstruct(
            schemaData, relationField.getToobject(), fieldSequence);
    this.fromSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getToobject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getTofield()
            + " = $1 where "
            + POSTGRES_COLUMNNAME_PREFIX
            + POSTGRES_ID_FIELD
            + "=$2  ; ";
    this.fromResetSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getToobject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getTofield()
            + " = null where "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField
            + "=$1 ; ";
    this.toSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getToobject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getTofield()
            + " =$1  where "
            + POSTGRES_COLUMNNAME_PREFIX
            + POSTGRES_ID_FIELD
            + " =$2 ;";
    this.resetSql =
        "delete from "
            + schemaData.getObjectMetaData().get(relationField.getToobject()).getTableName()
            + " where "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getTofield()
            + "=$1 ;";
    this.fromObject = schemaData.getObjectMetaData().get(relationField.getFromobject());
    this.toObject = schemaData.getObjectMetaData().get(relationField.getToobject());
  }

  @Override
  public CompletableFuture<Object> fromAdd(
      @NonNull String srcID, Object targetObject, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            String addFieldName = relationField.getTofield();
            List objectList = ArrayList.class.cast(targetObject);
            List idList = new ArrayList<>();
            List<Tuple> tupleList = new ArrayList<>();
            for (Object object : objectList) {
              Map map = Map.class.cast(object);
              map.put(addFieldName, srcID);
              Tuple tuple = Tuple.tuple();
              for (String field : fieldSequence) {
                if (null == map.get(field)) {
                  tuple.addValue(null);
                } else {
                  Object objectValue = map.get(field);
                  String fieldType = toObject.getFields().get(field);
                  if (null == fieldType) {
                    throw new BusinessException("E10092");
                  }
                  if (fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
                    ScalarFieldInfo scalarFieldInfo = toObject.getScalarFieldData().get(field);
                    if (scalarFieldInfo.getType().equals(GRAPHQL_OBJECT_TYPENAME)
                        || scalarFieldInfo.getType().equals(GRAPHQL_JSON_TYPENAME)) {
                      if (scalarFieldInfo.isIslist()) {
                        List<Object> objectListData = (List<Object>) objectValue;
                        JsonArray tmpJsonArray = new JsonArray();
                        for (Object tmpObjData : objectListData) {
                          JsonObject jsonObject = JsonObject.mapFrom(tmpObjData);
                          tmpJsonArray.add(jsonObject);
                        }
                        tuple.addValue(tmpJsonArray);
                      } else {
                        JsonObject jsonObject = JsonObject.mapFrom(objectValue);
                        tuple.addValue(jsonObject);
                      }
                    } else {
                      if (scalarFieldInfo.isIslist()) {
                        JsonArray tmpJsonArray = new JsonArray();
                        List<Object> objectListData = (List<Object>) objectValue;
                        for (Object tmpObjData : objectListData) {
                          tmpJsonArray.add(tmpObjData);
                        }
                        tuple.addValue(tmpJsonArray);
                      } else {
                        tuple.addValue(objectValue);
                      }
                    }
                  } else if (fieldType.equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
                    EnumField enumField = toObject.getEnumFieldData().get(field);
                    if (enumField.isIslist()) {
                      List<String> objStrListData = (List<String>) objectValue;
                      JsonArray tmpJsonArray = new JsonArray();
                      for (String tmpStr : objStrListData) {
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
              tupleList.add(tuple);
              idList.add(map.get(GRAPHQL_ID_FIELDNAME));
            }
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                if(null!=throwable) {
                    if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_FROM_ID, srcID);
                        errorMap.put(GRAPHQL_TO_OBJECT, targetObject);
                        log.error(
                                "{}",
                                LogData.getErrorLog(
                                        "E10004", errorMap, throwable));
                    }
                    future.completeExceptionally(new BusinessException("E10004"));
                } else  {
                    Transaction transaction = sqlConnection.begin();
                    if (reset) {
                        final String finalResetSql =
                                relationField.getIfcascade() ? resetSql : fromResetSql;
                        sqlConnection.preparedQuery(
                                finalResetSql,
                                Tuple.of(srcID),
                                resetHandler -> {
                                    if (resetHandler.succeeded()) {
                                        sqlConnection.preparedBatch(
                                                insertSql,
                                                tupleList,
                                                insertHandler -> {
                                                    if (insertHandler.succeeded()) {
                                                        transaction.commit(
                                                                transactionHandler -> {
                                                                    if (transactionHandler.succeeded()) {
                                                                        HashMap resultMap = new HashMap();
                                                                        resultMap.put(
                                                                                GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                                        future.complete(resultMap);
                                                                    } else {
                                                                        future.completeExceptionally(
                                                                                new BusinessException("E10007"));
                                                                    }
                                                                    transaction.close();
                                                                    sqlConnection.close();
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
                                                        future.completeExceptionally(new BusinessException("E10004"));
                                                        transaction.close();
                                                        sqlConnection.close();
                                                    }

                                                });
                                    } else {
                                        future.completeExceptionally(new BusinessException("E10100"));
                                        transaction.close();
                                        sqlConnection.close();
                                    }
                                });
                    } else {
                        sqlConnection.preparedBatch(
                                insertSql,
                                tupleList,
                                insertHandler -> {
                                    if (insertHandler.succeeded()) {
                                        transaction.commit(
                                                transactionHandler -> {
                                                    if (transactionHandler.succeeded()) {
                                                        HashMap resultMap = new HashMap();
                                                        resultMap.put(GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                        future.complete(resultMap);
                                                    } else {
                                                        future.completeExceptionally(new BusinessException("E10007"));
                                                    }
                                                    transaction.close();
                                                    sqlConnection.close();
                                                });
                                    } else {
                                        if (log.isErrorEnabled()) {
                                            HashMap errorMap = new HashMap();
                                            errorMap.put(GRAPHQL_FROM_ID, srcID);
                                            errorMap.put(GRAPHQL_TO_OBJECT, targetObject);
                                            log.error(
                                                    "{}",
                                                    LogData.getErrorLog("E10004", errorMap, insertHandler.cause()));
                                        }
                                        future.completeExceptionally(new BusinessException("E10004"));
                                        transaction.close();
                                        sqlConnection.close();
                                    }
                                });
                    }
                }
            });
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        });
    return future;
  }

  @Override
  public CompletableFuture<Object> fromByID(
      @NonNull String srcID, List<String> destID, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture();
    CompletableFuture.runAsync(
        () -> {
          try {
            List idList = destID;
            List<Tuple> tupleList = new ArrayList<>();
            for (String toId : destID) {
              Tuple tuple = Tuple.tuple();
              tuple.addString(srcID);
              tuple.addString(toId);
              tupleList.add(tuple);
            }
              PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                  if(null!=throwable) {
                      if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_FROM_ID, srcID);
                          errorMap.put(GRAPHQL_TO_OBJECT, destID);
                          log.error(
                                  "{}",
                                  LogData.getErrorLog(
                                          "E10004", errorMap, throwable));
                      }
                      future.completeExceptionally(new BusinessException("E10004"));
                  } else {
                      Transaction transaction = sqlConnection.begin();
                      if (reset) {
                          final String finalResetSql =
                                  relationField.getIfcascade() ? resetSql : fromResetSql;
                          sqlConnection.preparedQuery(
                                  finalResetSql,
                                  Tuple.of(srcID),
                                  resetHandler -> {
                                      if (resetHandler.succeeded()) {
                                          sqlConnection.preparedBatch(
                                                  fromSql,
                                                  tupleList,
                                                  insertHandler -> {
                                                      if (insertHandler.succeeded()) {
                                                          transaction.commit(
                                                                  transactionHandler -> {
                                                                      if (transactionHandler.succeeded()) {
                                                                          HashMap resultMap = new HashMap();
                                                                          resultMap.put(
                                                                                  GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                                          future.complete(resultMap);
                                                                      } else {
                                                                          future.completeExceptionally(
                                                                                  new BusinessException("E10007"));
                                                                      }
                                                                      transaction.close();
                                                                      sqlConnection.close();
                                                                  });
                                                      } else {
                                                          if (log.isErrorEnabled()) {
                                                              HashMap errorMap = new HashMap();
                                                              errorMap.put(GRAPHQL_FROM_ID, srcID);
                                                              errorMap.put(GRAPHQL_TO_OBJECT, destID);
                                                              log.error(
                                                                      "{}",
                                                                      LogData.getErrorLog(
                                                                              "E10004", errorMap, insertHandler.cause()));
                                                          }
                                                          future.completeExceptionally(new BusinessException("E10004"));
                                                          transaction.close();
                                                          sqlConnection.close();
                                                      }
                                                  });
                                      } else {
                                          future.completeExceptionally(new BusinessException("E10100"));
                                          transaction.close();
                                          sqlConnection.close();
                                      }
                                  });
                      } else {
                          sqlConnection.preparedBatch(
                                  fromSql,
                                  tupleList,
                                  insertHandler -> {
                                      if (insertHandler.succeeded()) {
                                          transaction.commit(
                                                  transactionHandler -> {
                                                      if (transactionHandler.succeeded()) {
                                                          HashMap resultMap = new HashMap();
                                                          resultMap.put(GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                          future.complete(resultMap);
                                                      } else {
                                                          future.completeExceptionally(new BusinessException("E10007"));
                                                      }
                                                      transaction.close();
                                                      sqlConnection.close();
                                                  });
                                      } else {
                                          if (log.isErrorEnabled()) {
                                              HashMap errorMap = new HashMap();
                                              errorMap.put(GRAPHQL_FROM_ID, srcID);
                                              errorMap.put(GRAPHQL_TO_OBJECT, destID);
                                              log.error(
                                                      "{}",
                                                      LogData.getErrorLog("E10004", errorMap, insertHandler.cause()));
                                          }

                                          future.completeExceptionally(new BusinessException("E10004"));
                                          transaction.close();
                                          sqlConnection.close();
                                      }
                                  });
                      }
                  }
              });
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        });
    PostgreSqlMany2ManyAdd.doAdd(fromResetSql, fromSql, srcID, destID, reset, future, schemaData);
    return future;
  }

  @Override
  public CompletableFuture<Object> toAdd(@NonNull String toID, String srcID, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
            List<String> idList = new ArrayList<>();
            idList.add(toID);
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                if(null!=throwable) {
                    if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_TO_ID, toID);
                        errorMap.put(GRAPHQL_FROM_ID, srcID);
                        log.error(
                                "{}",
                                LogData.getErrorLog("E10004", errorMap, throwable));
                    }
                    future.completeExceptionally(new BusinessException("E10004"));
                } else {
                    Transaction transaction = sqlConnection.begin();
                    if(reset) {
                        final String finalResetSql =
                                relationField.getIfcascade() ? resetSql : fromResetSql;
                        sqlConnection.preparedQuery(
                                finalResetSql,
                                Tuple.of(srcID),
                                resetHandler -> {
                                    if (resetHandler.succeeded()) {
                                        sqlConnection.preparedQuery(
                                                fromSql,
                                                Tuple.tuple().addString(srcID).addString(toID),
                                                insertHandler -> {
                                                    if (insertHandler.succeeded()) {
                                                        transaction.commit(
                                                                transactionHandler -> {
                                                                    if (transactionHandler.succeeded()) {
                                                                        HashMap resultMap = new HashMap();
                                                                        resultMap.put(
                                                                                GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                                        future.complete(resultMap);
                                                                    } else {
                                                                        future.completeExceptionally(
                                                                                new BusinessException("E10007"));
                                                                    }
                                                                    transaction.close();
                                                                    sqlConnection.close();
                                                                });
                                                    } else {
                                                        if (log.isErrorEnabled()) {
                                                            HashMap errorMap = new HashMap();
                                                            errorMap.put(GRAPHQL_FROM_ID, srcID);
                                                            errorMap.put(GRAPHQL_TO_OBJECT, toID);
                                                            log.error(
                                                                    "{}",
                                                                    LogData.getErrorLog(
                                                                            "E10004", errorMap, insertHandler.cause()));
                                                        }
                                                        future.completeExceptionally(new BusinessException("E10004"));
                                                        transaction.close();
                                                        sqlConnection.close();
                                                    }
                                                });
                                    } else {
                                        future.completeExceptionally(new BusinessException("E10100"));
                                        transaction.close();
                                        sqlConnection.close();
                                    }
                                });
                    } else {
                        sqlConnection.preparedQuery(
                                fromSql,
                                Tuple.tuple().addString(srcID).addString(toID),
                                insertHandler -> {
                                    if (insertHandler.succeeded()) {
                                        transaction.commit(
                                                transactionHandler -> {
                                                    if (transactionHandler.succeeded()) {
                                                        HashMap resultMap = new HashMap();
                                                        resultMap.put(
                                                                GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                        future.complete(resultMap);
                                                    } else {
                                                        future.completeExceptionally(
                                                                new BusinessException("E10007"));
                                                    }
                                                    transaction.close();
                                                    sqlConnection.close();
                                                });
                                    } else {
                                        if (log.isErrorEnabled()) {
                                            HashMap errorMap = new HashMap();
                                            errorMap.put(GRAPHQL_FROM_ID, srcID);
                                            errorMap.put(GRAPHQL_TO_OBJECT, toID);
                                            log.error(
                                                    "{}",
                                                    LogData.getErrorLog(
                                                            "E10004", errorMap, insertHandler.cause()));
                                        }
                                        future.completeExceptionally(new BusinessException("E10004"));
                                        transaction.close();
                                        sqlConnection.close();
                                    }
                                });
                    }
                }
            });
        });
    return future;
  }
}
