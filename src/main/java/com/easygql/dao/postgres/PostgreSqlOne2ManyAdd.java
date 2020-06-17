package com.easygql.dao.postgres;

import com.easygql.dao.One2ManyRelationCreater;
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
            schemaData, relationField.getToObject(), fieldSequence);
    this.fromSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getToField()
            + " = $1 where "
            + POSTGRES_COLUMNNAME_PREFIX
            + POSTGRES_ID_FIELD
            + "=$2  ; ";
    this.fromResetSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getToField()
            + " = null where "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField
            + "=$1 ; ";
    this.toSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getToField()
            + " =$1  where "
            + POSTGRES_COLUMNNAME_PREFIX
            + POSTGRES_ID_FIELD
            + " =$2 ;";
    this.resetSql =
        "delete from "
            + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
            + " where "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getToField()
            + "=$1 ;";
    this.fromObject = schemaData.getObjectMetaData().get(relationField.getFromObject());
    this.toObject = schemaData.getObjectMetaData().get(relationField.getToObject());
  }

  @Override
  public CompletableFuture<Object> fromAdd(
      @NonNull String srcID, Object targetObject, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            String addFieldName = relationField.getToField();
            List objectList = ArrayList.class.cast(targetObject);
            List idList = new ArrayList<>();
            List<Tuple> tupleList = new ArrayList<>();
            for (Object object : objectList) {
              Map resultData = Map.class.cast(object);
              resultData.put(addFieldName, srcID);
              Tuple tuple = Tuple.tuple();
                for (String field : fieldSequence) {
                    if (null == resultData.get(field)) {
                        tuple.addValue(null);
                    } else {
                        Object objectValue = resultData.get(field);
                        if (toObject
                                .getFields()
                                .get(field)
                                .equals(GRAPHQL_ENUMFIELD_TYPENAME)) {
                            if (toObject.getEnumFieldData().get(field).isList()) {
                                List<String> tmpEnumList = (List<String>) objectValue;
                                tuple.addStringArray(tmpEnumList.toArray(new String[tmpEnumList.size()]));
                            } else {
                                tuple.addValue(objectValue);
                            }
                        } else if (toObject.getFields().get(field).equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
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
                                        List<String> tmpScalarStringList = (List<String>) objectValue;
                                        tuple.addStringArray(
                                                tmpScalarStringList.toArray(new String[tmpScalarStringList.size()]));
                                        break;
                                    case GRAPHQL_INT_TYPENAME:
                                        List<Integer> tmpScalarIntegerList = (List<Integer>) objectValue;
                                        tuple.addIntegerArray(
                                                tmpScalarIntegerList.toArray(
                                                        new Integer[tmpScalarIntegerList.size()]));
                                        break;
                                    case GRAPHQL_BOOLEAN_TYPENAME:
                                        List<Boolean> tmpScalarBooleanList = (List<Boolean>) objectValue;
                                        tuple.addBooleanArray(
                                                tmpScalarBooleanList.toArray(
                                                        new Boolean[tmpScalarBooleanList.size()]));
                                        break;
                                    case GRAPHQL_LONG_TYPENAME:
                                    case GRAPHQL_BIGINTEGER_TYPENAME:
                                        List<Long> tmpScalarLongList = (List<Long>) objectValue;
                                        tuple.addLongArray(
                                                tmpScalarLongList.toArray(new Long[tmpScalarLongList.size()]));
                                        break;
                                    case GRAPHQL_SHORT_TYPENAME:
                                        List<Short> tmpScalarShortList = (List<Short>) objectValue;
                                        tuple.addShortArray(
                                                tmpScalarShortList.toArray(new Short[tmpScalarShortList.size()]));
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
                                        List<Float> tmpScalarFloatList = (List<Float>) objectValue;
                                        tuple.addFloatArray(
                                                tmpScalarFloatList.toArray(new Float[tmpScalarFloatList.size()]));
                                        break;
                                    case GRAPHQL_BIGDECIMAL_TYPENAME:
                                        List<BigDecimal> tmpScalarBigDecimalist = (List<BigDecimal>) objectValue;
                                        tuple.addValues(tmpScalarBigDecimalist.toArray(new Float[tmpScalarBigDecimalist.size()]));
                                        break;
                                    default:
                                        List tmpObjectValue = (List) objectValue;
                                        tuple.addValues(tmpObjectValue.toArray());
                                }
                            } else {
                                tuple.addValue(objectValue);
                            }
                        }
                        else {
                            tuple.addValue(objectValue);
                        }
                    }
                }
              tupleList.add(tuple);
              idList.add(resultData.get(GRAPHQL_ID_FIELDNAME));
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
                                relationField.getIfCascade() ? resetSql : fromResetSql;
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
                                  relationField.getIfCascade() ? resetSql : fromResetSql;
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
                                relationField.getIfCascade() ? resetSql : fromResetSql;
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
