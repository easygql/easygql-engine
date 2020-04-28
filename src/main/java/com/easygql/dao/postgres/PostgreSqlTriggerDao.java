package com.easygql.dao.postgres;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.easygql.dao.DataSelecter;
import com.easygql.dao.TriggerDao;
import com.easygql.exception.BusinessException;
import com.easygql.service.SubscriptionService;
import com.easygql.util.*;
import io.reactivex.ObservableEmitter;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class PostgreSqlTriggerDao implements TriggerDao {
  private SchemaData schemaData;
  private String schemaID;
  private HashMap<String, Trigger> triggerdata = new HashMap<>();
  private DataSelecter triggerSelecter;
  private static final String triggerQueryStr =
      "SELECT count(*) FROM pg_trigger WHERE tgname = $1 ";
  private static final String listenSql = "listen \"%s\";";
  public static final String eventDataTableFormat =
      "create table if not EXISTS %s ( f_id varchar not null ,f_payload json not null,f_changefeedname varchar not null,f_datetime  TIMESTAMP not null default NOW()); ";
  public static final String ifEventDataTableFormat =
      " SELECT table_name FROM information_schema.tables WHERE table_schema='public'  and  table_name='%s';";
  private static final String deleteEventSQLFormat = " delete from %s where f_date < $1 ";
  private static final String eventSelectSQLFormat = " select f_payload from %s where f_id='%s';";
  private static HashMap<String, PgConnection> subscriberHashMap = new HashMap<>();
  private static final String triggerHisInsert = " insert into trigger_his(f_id,f_trigger,f_oldval,f_newval,f_eventtype,f_issucceed) values($1,$2,$3,$4,$5,$6)";

  private static HashMap<String, Object> triggerSelectFields = new HashMap();

  {
    HashMap triggerSelecter = new HashMap();
    triggerSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_NAME_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_TYPENAME_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_EVENTTYPE_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_HEADERS_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_OK_STATUS_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_PAYLOADFORMATTER_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_PAYLOADARGS_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_RETRY_TIMES_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_WEBHOOK_URL_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_STARTDATE_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_EXPIREDATE_FIELDNAME, 1);
    triggerSelecter.put(GRAPHQL_DESCRIPTION_FIELDNAME, 1);
    triggerSelectFields.put(GRAPHQL_ID_FIELDNAME, 1);
    triggerSelectFields.put(GRAPHQL_TRIGGERS_FIELDNAME, triggerSelecter);
  }

  @Override
  public void init(SchemaData schemaData, String schemaID) {
    this.schemaData = schemaData;
    this.schemaID = schemaID;
    if (schemaID.equals(GRAPHQL_SCHEMA_ID_DEFAULT)) {
      return;
    }
    this.triggerSelecter =
        DaoFactory.getSelecterDao(
            GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT).getSchemaData().getDatabasekind());
    this.triggerSelecter.Init(
        GRAPHQL_SCHEMA_TYPENAME,
        GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT).getSchemaData(),
        GRAPHQL_SCHEMA_ID_DEFAULT);
    try {
      HashMap eqOperator = new HashMap();
      eqOperator.put(GRAPHQL_FILTER_EQ_OPERATOR, schemaID);
      HashMap idConditon = new HashMap();
      idConditon.put(GRAPHQL_ID_FIELDNAME, eqOperator);
      HashMap filterOpertor = new HashMap();
      filterOpertor.put(GRAPHQL_FILTER_FILTER_OPERATOR, idConditon);
      Map schemaTriggerInfo =
          triggerSelecter.getSingleDoc(filterOpertor, triggerSelectFields).get();
      if (null == schemaTriggerInfo) {
        return;
      }
      List<Map> triggerMapList = (List<Map>) schemaTriggerInfo.get(GRAPHQL_TRIGGERS_FIELDNAME);
      List<Trigger> triggerList =
          JSON.parseArray(JSONObject.toJSONString(triggerMapList), Trigger.class);
      if (null != triggerList) {
        triggerList.forEach(
            trigger -> {
              triggerdata.put(GRAPHQL_TRIGGER_NAME_PREFIX + trigger.getId(), trigger);
            });
      }
    } catch (InterruptedException e) {
      if (log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_ID_FIELDNAME, schemaID);
        log.error("{}", LogData.getErrorLog("E10018", errorMap, e));
      }
    } catch (ExecutionException e) {
      if (log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_ID_FIELDNAME, schemaID);
        log.error("{}", LogData.getErrorLog("E10018", errorMap, e));
      }
    }
  }

  public CompletableFuture<Boolean> triggerCreate(ObjectTypeMetaData typeMetaData) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
              PgConnection.connect(
                      Vertx.vertx(),
                      PostgreSQLPoolCache.getPgConnectionOptions(schemaData.getDatasourceinfo()),
                      pgConnectionAsyncResult -> {
                          if (pgConnectionAsyncResult.succeeded()){
                              SqlConnection sqlConnection = pgConnectionAsyncResult.result();
                              String changeFeedName =
                                      SubscriptionService.getSubscriptionName(
                                              typeMetaData.getOutPutName(), null, null, schemaID);
                              String eventDataTableName =
                                      POSTGRES_TABLENAME_PREFIX
                                              + typeMetaData.getOutPutName()
                                              + GRAPHQL_EVENTDATA_POSTFIX;
                              String ifEventDataTableSQL =
                                      String.format(ifEventDataTableFormat, eventDataTableName);
                              sqlConnection.query(
                                      ifEventDataTableSQL,
                                      ifEventDataTableHandler -> {
                                          if (ifEventDataTableHandler.succeeded()) {
                                              Iterator<Row> iterator = ifEventDataTableHandler.result().iterator();
                                              if (!iterator.hasNext()) {
                                                  String eventDataTable =
                                                          String.format(eventDataTableFormat, eventDataTableName);
                                                  sqlConnection.query(
                                                          eventDataTable,
                                                          eventDataTableHandler -> {
                                                              if (eventDataTableHandler.succeeded()) {
                                                                  HashMap<String, String> resultMap =
                                                                          PostgreSqlSub.subConstruct(
                                                                                  null,
                                                                                  null,
                                                                                  changeFeedName,
                                                                                  schemaData,
                                                                                  typeMetaData.getOutPutName());
                                                                  sqlConnection.query(
                                                                          resultMap.get("code"),
                                                                          functionHandler -> {
                                                                              if (functionHandler.succeeded()) {
                                                                                  sqlConnection.query(
                                                                                          resultMap.get("trigger"),
                                                                                          triggerHandler -> {
                                                                                              sqlConnection.close();
                                                                                              HashMap tmpMap = resultMap;
                                                                                              if (!triggerHandler.succeeded()) {
                                                                                                  future.completeExceptionally(
                                                                                                          new BusinessException("E10026"));
                                                                                              } else {
                                                                                                  future.complete(true);
                                                                                              }
                                                                                          });

                                                                              } else {
                                                                                  sqlConnection.close();
                                                                                  HashMap tmpMap = resultMap;
                                                                                  if (log.isErrorEnabled()) {
                                                                                      log.error(
                                                                                              "{}",
                                                                                              LogData.getErrorLog(
                                                                                                      "E10026", null, functionHandler.cause()));
                                                                                  }
                                                                                  future.completeExceptionally(
                                                                                          new BusinessException("E10026"));
                                                                              }
                                                                          });
                                                              } else {
                                                                  if (log.isErrorEnabled()) {
                                                                      HashMap errorMap = new HashMap();
                                                                      errorMap.put(GRAPHQL_QUERYAPI_FIELDNAME, eventDataTable);
                                                                      log.error(
                                                                              "{}",
                                                                              LogData.getErrorLog(
                                                                                      "E10095", errorMap, eventDataTableHandler.cause()));
                                                                  }
                                                                  sqlConnection.close();

                                                                  future.completeExceptionally(new BusinessException("E10095"));
                                                              }
                                                          });
                                              } else {
                                                  HashMap<String, String> resultMap =
                                                          PostgreSqlSub.subConstruct(
                                                                  null,
                                                                  null,
                                                                  changeFeedName,
                                                                  schemaData,
                                                                  typeMetaData.getOutPutName());
                                                  sqlConnection.query(
                                                          resultMap.get("code"),
                                                          functionHandler -> {
                                                              if (functionHandler.succeeded()) {
                                                                  sqlConnection.query(
                                                                          resultMap.get("trigger"),
                                                                          triggerHandler -> {
                                                                              sqlConnection.close();
                                                                              if (!triggerHandler.succeeded()) {
                                                                                  future.completeExceptionally(
                                                                                          new BusinessException("E10026"));
                                                                              } else {
                                                                                  future.complete(true);
                                                                              }
                                                                          });

                                                              } else {
                                                                  sqlConnection.close();
                                                                  if (log.isErrorEnabled()) {
                                                                      log.error(
                                                                              "{}",
                                                                              LogData.getErrorLog(
                                                                                      "E10026", null, functionHandler.cause()));
                                                                  }
                                                                  future.completeExceptionally(new BusinessException("E10026"));
                                                              }
                                                          });
                                              }
                                          } else {
                                              if (log.isErrorEnabled()) {
                                                  HashMap errorMap = new HashMap();
                                                  errorMap.put(GRAPHQL_QUERYAPI_FIELDNAME, ifEventDataTableSQL);
                                                  log.error(
                                                          "{}",
                                                          LogData.getErrorLog(
                                                                  "E10095", errorMap, ifEventDataTableHandler.cause()));
                                              }
                                              sqlConnection.close();
                                              future.completeExceptionally(new BusinessException("E10095"));
                                          }
                                      });
                          } else {
                              future.completeExceptionally(new BusinessException("E10026"));
                          } });
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        });
    return future;
  }

  public static CompletableFuture<Object> getPayLoad(
      String eventID, String objectName, SchemaData schemaData) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            String eventDataTableName =
                POSTGRES_TABLENAME_PREFIX + objectName + GRAPHQL_EVENTDATA_POSTFIX;
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                if(null!=throwable) {
                    future.completeExceptionally(new BusinessException("E10049"));
                } else {
                    String eventSelectSQL =
                            String.format(eventSelectSQLFormat, eventDataTableName, eventID);
                    sqlConnection.query(
                            eventSelectSQL,
                            eventResultHandler -> {
                                if (eventResultHandler.succeeded()) {
                                    Iterator<Row> iterator = eventResultHandler.result().iterator();
                                    if (iterator.hasNext()) {
                                        Row row = iterator.next();
                                        Object objectVal = row.getValue("f_payload");
                                        future.complete(objectVal);
                                    } else {
                                        future.completeExceptionally(new BusinessException("E10094"));
                                    }
                                    sqlConnection.close();
                                } else {
                                    sqlConnection.close();
                                    future.completeExceptionally(new BusinessException("E10094"));
                                }
                            });
                }
            });
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        });
    return future;
  }

  @Override
  public CompletableFuture<Void> ListenTrigger(String typeName, ObservableEmitter<Object> emitter) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          if (null != subscriberHashMap.get(schemaID + "_" + typeName)) {
            future.completeExceptionally(new BusinessException("E10081"));
          } else {
            try {
                PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                    if(null!=throwable) {
                        future.completeExceptionally(new BusinessException("E10079"));
                    } else {
                        String changeFeedName =
                                SubscriptionService.getSubscriptionName(typeName, null, null, schemaID);
                        sqlConnection.preparedQuery(
                                triggerQueryStr,
                                Tuple.of(changeFeedName),
                                queryHandler -> {
                                    if (queryHandler.succeeded()) {
                                        Iterator<Row> iterator = queryHandler.result().iterator();
                                        if (iterator.hasNext()) {
                                            Row row = iterator.next();
                                            int trigger_count = row.getInteger(0);
                                            if (0 == trigger_count) {
                                                sqlConnection.close();
                                                future.completeExceptionally(new BusinessException("E10052"));
                                            } else {
                                                sqlConnection.close();
                                                PgConnection.connect(
                                                        Vertx.vertx(),
                                                        PostgreSQLPoolCache.getPgConnectionOptions(
                                                                schemaData.getDatasourceinfo()),
                                                        pgConnectionAsyncResult -> {
                                                            if (pgConnectionAsyncResult.succeeded()) {
                                                                PgConnection connection =
                                                                        pgConnectionAsyncResult.result();
                                                                connection.query(
                                                                        String.format(listenSql, changeFeedName),
                                                                        subQueryHandler -> {
                                                                            if (subQueryHandler.succeeded()) {
                                                                                connection.notificationHandler(
                                                                                        pgNotification -> {
                                                                                            JSONArray arrayInfo =
                                                                                                    JSONArray.parseArray(
                                                                                                            pgNotification.getPayload());
                                                                                            if (arrayInfo.size() > 1) {
                                                                                                JsonArray jsonArray = new JsonArray();
                                                                                                jsonArray.add(arrayInfo.getString(1));
                                                                                                jsonArray.add(arrayInfo.get(2));
                                                                                                jsonArray.add(arrayInfo.get(3));
                                                                                                emitter.onNext(jsonArray);
                                                                                            } else {
                                                                                                String eventID = arrayInfo.getString(0);
                                                                                                getPayLoad(
                                                                                                        eventID, typeName, schemaData)
                                                                                                        .whenComplete(
                                                                                                                (payload, payloadEx) -> {
                                                                                                                    if (null != payloadEx) {
                                                                                                                        if (log.isErrorEnabled()) {
                                                                                                                            log.error(
                                                                                                                                    "{}",
                                                                                                                                    LogData.getErrorLog(
                                                                                                                                            "E10094", null,
                                                                                                                                            payloadEx));
                                                                                                                        }
                                                                                                                    } else {
                                                                                                                        emitter.onNext(payload);
                                                                                                                    }
                                                                                                                });
                                                                                            }
                                                                                        });
                                                                                subscriberHashMap.put(
                                                                                        schemaID + "_" + typeName, connection);
                                                                            } else {
                                                                                if (log.isErrorEnabled()) {
                                                                                    log.error(
                                                                                            "{}",
                                                                                            LogData.getErrorLog(
                                                                                                    "E10079",
                                                                                                    null,
                                                                                                    subQueryHandler.cause()));
                                                                                }
                                                                                future.completeExceptionally(
                                                                                        new BusinessException("E10079"));
                                                                            }
                                                                        });
                                                            } else {
                                                                // 抛出异常
                                                                if (log.isErrorEnabled()) {
                                                                    log.error(
                                                                            "{}",
                                                                            LogData.getErrorLog(
                                                                                    "E10079",
                                                                                    null,
                                                                                    pgConnectionAsyncResult.cause()));
                                                                }
                                                                future.completeExceptionally(
                                                                        new BusinessException("E10079"));
                                                            }
                                                        });
                                            }
                                        } else {
                                            sqlConnection.close();

                                            future.completeExceptionally(new BusinessException("E10079"));
                                        }
                                    } else {
                                        sqlConnection.close();

                                        future.completeExceptionally(new BusinessException("E10079"));
                                    }
                                });
                    }
                });
            } catch (Exception e) {
              future.completeExceptionally(new BusinessException("E10079"));
            }
          }
        });
    return future;
  }

    @Override
    public void AddTriggerEvent(String triggerID, String eventType, Map<String, Object> oldVal, Map<String, Object> newVal,Boolean isSucceed) {
        CompletableFuture.runAsync(()->{
            try {
                PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                    if(null!=throwable) {
                        if(log.isErrorEnabled()) {
                            log.error("{}",LogData.getErrorLog("E10049",new HashMap<>(),throwable));
                        }
                    } else {
                        Tuple tuple = Tuple.tuple();
                        tuple.addString(IDTools.getID());
                        tuple.addString(triggerID);
                        tuple.addString(JSONObject.toJSONString(oldVal));
                        tuple.addString(JSONObject.toJSONString(newVal));
                        tuple.addString(eventType);
                        tuple.addBoolean(isSucceed);
                        sqlConnection.preparedQuery(triggerHisInsert,tuple,insertHandler->{
                            if(!insertHandler.succeeded()){
                                if(log.isErrorEnabled()) {
                                    HashMap errorMap = new HashMap();
                                    errorMap.put(GRAPHQL_EVENTTYPE_FIELDNAME,eventType);
                                    errorMap.put(SUBSCRIPTION_NODE_OLD,oldVal);
                                    errorMap.put(SUBSCRIPTION_NODE_NEW,newVal);
                                    errorMap.put(GRAPHQL_TRIGGER_FIELDNAME,triggerID);
                                    log.error("{}",LogData.getErrorLog("E10102",errorMap,insertHandler.cause()));
                                }
                            }
                            sqlConnection.close();
                        });
                    }
                });
            } catch (Exception e) {
                if(log.isErrorEnabled()) {
                    HashMap errorMap = new HashMap();
                    errorMap.put(GRAPHQL_EVENTTYPE_FIELDNAME,eventType);
                    errorMap.put(SUBSCRIPTION_NODE_OLD,oldVal);
                    errorMap.put(SUBSCRIPTION_NODE_NEW,newVal);
                    errorMap.put(GRAPHQL_TRIGGER_FIELDNAME,triggerID);
                    log.error("{}",LogData.getErrorLog("E10102",errorMap,e));
                }
            }
        });
    }
}
