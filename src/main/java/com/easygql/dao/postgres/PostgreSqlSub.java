package com.easygql.dao.postgres;

import com.alibaba.fastjson.JSONArray;
import com.easygql.dao.DataSub;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.reactivex.ObservableEmitter;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.dao.postgres.PostgreSqlTriggerDao.getPayLoad;

/**
 * @author guofen
 * @date 2019/12/18 19:43
 */
@Slf4j
public class PostgreSqlSub implements DataSub {
  private String schemaID;
  private String objectName;
  private SchemaData schemaData;
  private final String triggerQueryStr = "SELECT count(*) FROM pg_trigger WHERE tgname = $1 ";
  private final String listenSql = "listen \"%s\";";
  private static HashMap<String, PgConnection> subscriberHashMap = new HashMap<>();
  public static final String channelName = "subscription";

  @Override
  public void Init(String objectName, SchemaData schemaData, String schemaID) {
    this.schemaID = schemaID;
    this.objectName = objectName;
    this.schemaData = schemaData;
  }

  @Override
  public CompletableFuture<Void> doSub(
      ObservableEmitter<Object> emitter,
      String schemaID,
      SchemaData schemaData,
      String objectName,
      List<String> selectFields,
      List<String> watchFields,
      String changeFeedName) {
    return CompletableFuture.runAsync(
        () -> {
          PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid())
              .whenCompleteAsync(
                  (sqlConnection, throwable) -> {
                    if (null != throwable) {
                      throw new BusinessException("E10026");
                    } else {
                      sqlConnection.preparedQuery(
                          triggerQueryStr,
                          Tuple.of(changeFeedName),
                          queryHandler -> {
                            if (queryHandler.succeeded()) {
                              Iterator<Row> iterator = queryHandler.result().iterator();
                              if (iterator.hasNext()) {
                                Row row = iterator.next();
                                int trigger_count = row.getInteger(0);
                                if (trigger_count == 0) {
                                  HashMap<String, String> resultMap =
                                      subConstruct(
                                          selectFields,
                                          watchFields,
                                          changeFeedName,
                                          schemaData,
                                          objectName);
                                  sqlConnection.query(
                                      resultMap.get("code"),
                                      functionHandler -> {
                                        if (functionHandler.succeeded()) {
                                          sqlConnection.query(
                                              resultMap.get("trigger"),
                                              triggerHandler -> {
                                                sqlConnection.close();
                                                if (!triggerHandler.succeeded()) {
                                                  throw new BusinessException("E10026");
                                                } else {
                                                  PgConnection.connect(
                                                      Vertx.vertx(),
                                                      PostgreSQLPoolCache.getPgConnectionOptions(
                                                          schemaData.getDatasourceinfo()),
                                                      pgConnectionAsyncResult -> {
                                                        if (pgConnectionAsyncResult.succeeded()) {
                                                          PgConnection connection =
                                                              pgConnectionAsyncResult.result();
                                                          connection.query(
                                                              String.format(
                                                                  listenSql, changeFeedName),
                                                              subQueryHandler -> {
                                                                if (subQueryHandler.succeeded()) {
                                                                  connection.notificationHandler(
                                                                      pgNotification -> {
                                                                        JSONArray arrayInfo =
                                                                            JSONArray.parseArray(
                                                                                pgNotification
                                                                                    .getPayload());
                                                                        if (arrayInfo.size() > 1) {
                                                                          JsonArray jsonArray =
                                                                              new JsonArray();
                                                                          jsonArray.add(
                                                                              arrayInfo.getString(
                                                                                  1));
                                                                          jsonArray.add(
                                                                              arrayInfo.get(2));
                                                                          jsonArray.add(
                                                                              arrayInfo.get(3));
                                                                          emitter.onNext(jsonArray);
                                                                        } else {
                                                                          String eventID =
                                                                              arrayInfo.getString(
                                                                                  0);
                                                                          getPayLoad(
                                                                                  eventID,
                                                                                  objectName,
                                                                                  schemaData)
                                                                              .whenComplete(
                                                                                  (payload,
                                                                                      payloadEx) -> {
                                                                                    if (null
                                                                                        != payloadEx) {
                                                                                      if (log
                                                                                          .isErrorEnabled()) {
                                                                                        log.error(
                                                                                            "{}",
                                                                                            LogData
                                                                                                .getErrorLog(
                                                                                                    "E10094",
                                                                                                    null,
                                                                                                    payloadEx));
                                                                                      }
                                                                                    } else {
                                                                                      emitter
                                                                                          .onNext(
                                                                                              payload);
                                                                                    }
                                                                                  });
                                                                        }
                                                                      });
                                                                  subscriberHashMap.put(
                                                                      changeFeedName, connection);
                                                                } else {
                                                                  if (log.isErrorEnabled()) {
                                                                    log.error(
                                                                        "{}",
                                                                        LogData.getErrorLog(
                                                                            "E10026",
                                                                            null,
                                                                            subQueryHandler
                                                                                .cause()));
                                                                  }
                                                                  throw new BusinessException(
                                                                      "E10026");
                                                                }
                                                              });

                                                        } else {
                                                          // 抛出异常
                                                          if (log.isErrorEnabled()) {
                                                            log.error(
                                                                "{}",
                                                                LogData.getErrorLog(
                                                                    "E10026",
                                                                    null,
                                                                    pgConnectionAsyncResult
                                                                        .cause()));
                                                          }
                                                          throw new BusinessException("E10026");
                                                        }
                                                      });
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
                                          throw new BusinessException("E10026");
                                        }
                                      });
                                } else {
                                  PgConnection.connect(
                                      PostgreSQLPoolCache.vertx,
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
                                                                  eventID, objectName, schemaData)
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
                                                  subscriberHashMap.put(changeFeedName, connection);
                                                } else {
                                                  if (log.isErrorEnabled()) {
                                                    log.error(
                                                        "{}",
                                                        LogData.getErrorLog(
                                                            "E10026",
                                                            null,
                                                            subQueryHandler.cause()));
                                                  }
                                                  throw new BusinessException("E10026");
                                                }
                                              });

                                        } else {
                                          // 抛出异常
                                          if (log.isErrorEnabled()) {
                                            log.error(
                                                "{}",
                                                LogData.getErrorLog(
                                                    "E10026",
                                                    null,
                                                    pgConnectionAsyncResult.cause()));
                                          }
                                          throw new BusinessException("E10026");
                                        }
                                      });
                                }
                              }
                            } else {
                              sqlConnection.close();

                              throw new BusinessException("E10026");
                            }
                          });
                    }
                  });
        });
  }

  @Override
  public CompletableFuture<Void> doClose(String changeFeedName) {
    return CompletableFuture.runAsync(
        () -> {
          subscriberHashMap.remove(changeFeedName).close();
        });
  }

  public static HashMap<String, String> subConstruct(
      List<String> selectFields,
      List<String> watchFields,
      String changeFeedName,
      SchemaData schemaData,
      String objectName) {
    String triggerDataTableName =
        POSTGRES_TABLENAME_PREFIX + objectName + GRAPHQL_EVENTDATA_POSTFIX;
    List<String> col_old = new ArrayList<>();
    List<String> col_new = new ArrayList<>();
    List<String> assign_old = new ArrayList<>();
    List<String> assign_new = new ArrayList<>();
    List<String> buildObj_old = new ArrayList<>();
    List<String> buildObj_new = new ArrayList<>();
    ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
    if (null == selectFields) {
      selectFields = new ArrayList<>();
      selectFields.addAll(objectTypeMetaData.getScalarFieldData().keySet());
      selectFields.addAll(objectTypeMetaData.getEnumFieldData().keySet());
      for (RelationField relationField : objectTypeMetaData.getFromRelationFieldData().values()) {
              if(relationField.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)) {
                  selectFields.add(relationField.getFromfield());
              }
      }
      for (RelationField relationField:objectTypeMetaData.getToRelationFieldData().values()) {
          if(relationField.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)||relationField.getRelationtype().equals(GRAPHQL_ONE2ONE_NAME)) {
          selectFields.add(relationField.getTofield());
          }
      }

    }
    for (String fieldName : selectFields) {
      String fieldType = objectTypeMetaData.getFields().get(fieldName);
      if (fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
        ScalarFieldInfo scalarFieldInfo = objectTypeMetaData.getScalarFieldData().get(fieldName);
        String sqlType = null;
        if (scalarFieldInfo.isIslist()) {
          sqlType = TypeMapping.getTypeName(DATABASE_KIND_POSTGRES, GRAPHQL_JSON_TYPENAME);
        } else {
          sqlType = TypeMapping.getTypeName(DATABASE_KIND_POSTGRES, scalarFieldInfo.getType());
        }
        col_old.add(fieldName + "_old " + sqlType + "; ");
        col_new.add(fieldName + "_new " + sqlType + "; ");
      } else {
        col_old.add(fieldName + "_old  varchar ; ");
        col_new.add(fieldName + "_new  varchar ; ");
      }
      assign_old.add(fieldName + "_old =OLD." + POSTGRES_COLUMNNAME_PREFIX + fieldName + "; ");
      assign_new.add(fieldName + "_new =NEW." + POSTGRES_COLUMNNAME_PREFIX + fieldName + "; ");
      buildObj_old.add("'" + fieldName + "', " + fieldName + "_old");
      buildObj_new.add("'" + fieldName + "', " + fieldName + "_new");
    }
    String no_change = " false ";
    String update_of = " ";
    HashSet<String> watchSet = new HashSet<>();
    if (null != watchFields && watchFields.size() > 0) {
      watchSet.addAll(watchFields);
    }
    if (null != selectFields && selectFields.size() > 0) {
      watchSet.addAll(selectFields);
    }
    if (watchSet.size() > 0) {
      List<String> keys = new ArrayList<>();
      List<String> xKeys = new ArrayList<>();
      watchSet.forEach(
          str -> {
            String fieldType = objectTypeMetaData.getFields().get(str);
            if (fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
              ScalarFieldInfo scalarFieldInfo = objectTypeMetaData.getScalarFieldData().get(str);
              String sqlType = null;
              if (scalarFieldInfo.isIslist()) {
                sqlType = TypeMapping.getTypeName(DATABASE_KIND_POSTGRES, GRAPHQL_JSON_TYPENAME);
              } else {
                sqlType =
                    TypeMapping.getTypeName(DATABASE_KIND_POSTGRES, scalarFieldInfo.getType());
              }
              if (sqlType.equals("json")) {
                keys.add(
                    " OLD."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str
                        + "::json::text"
                        + "=NEW."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str
                        + "::json::text");
              } else {
                keys.add(
                    " OLD."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str
                        + "=NEW."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str);
              }

            } else if(fieldType.equals(GRAPHQL_ENUMFIELD_TYPENAME)) {
              EnumField enumField = objectTypeMetaData.getEnumFieldData().get(str);
              if (enumField.isIslist()) {
                keys.add(
                    " OLD."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str
                        + "::json::text"
                        + "=NEW."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str
                        + "::json::text");
              } else {
                keys.add(
                    " OLD."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str
                        + "=NEW."
                        + POSTGRES_COLUMNNAME_PREFIX
                        + str);
              }
            } else {
                keys.add(
                        " OLD."
                                + POSTGRES_COLUMNNAME_PREFIX
                                + str
                                + "=NEW."
                                + POSTGRES_COLUMNNAME_PREFIX
                                + str);
            }
            xKeys.add("\"" + POSTGRES_COLUMNNAME_PREFIX + str + "\"");
          });
      no_change = String.join(" and ", keys);
      update_of = " OF " + String.join(" , ", xKeys);
    }
    String function =
        "CREATE OR REPLACE FUNCTION "
            + changeFeedName
            + "() RETURNS TRIGGER AS $$ \n"
            + "DECLARE \n"
            + " tmp_eventid varchar;\n"
            + " event_data json; \n"
            + "notification json;\n"
            + "obj_old json;\n"
            + "obj_new json;\n"
            + String.join("\n", col_old)
            + String.join("\n", col_new)
            + " BEGIN \n"
            + " -- TG_OP is 'DELETE', 'INSERT' or 'UPDATE' \n"
            + " IF TG_OP = 'DELETE' THEN \n"
            + String.join("\n", assign_old)
            + " obj_old = json_build_object( "
            + String.join(",", buildObj_old)
            + ");\n"
            + " END IF;\n"
            + " IF TG_OP = 'INSERT' THEN \n"
            + String.join("\n", assign_new)
            + "\n"
            + " obj_new=json_build_object("
            + String.join(",", buildObj_new)
            + ");\n"
            + " END IF;\n"
            + " IF TG_OP= 'UPDATE'  THEN \n"
            + "  IF "
            + no_change
            + "\n THEN "
            + "  RETURN NULL; \n"
            + " END IF;\n"
            + String.join("\n", assign_old)
            + "\n"
            + " obj_old = json_build_object( "
            + String.join(",", buildObj_old)
            + ");\n"
            + String.join("\n", assign_new)
            + "\n"
            + " obj_new = json_build_object( "
            + String.join(",", buildObj_new)
            + ");\n"
            + " END IF;\n"
            + "tmp_eventid=(select generate_id());\n"
            + "event_data = json_build_array(TG_OP, obj_new, obj_old);\n"
            + "IF octet_length(event_data::text)< 4000 THEN \n"
            + " notification = json_build_array(tmp_eventid,TG_OP, obj_new, obj_old);\n"
            + " ELSE \n"
            + "notification = json_build_array(tmp_eventid);\n"
            + "END IF;"
            + " delete from "
            + triggerDataTableName
            + " where f_datetime<(now()+'-2 min');"
            + "insert into "
            + triggerDataTableName
            + "(f_id,f_changefeedname,f_payload) values (tmp_eventid,'"
            + changeFeedName
            + "',event_data);"
            + "PERFORM pg_notify('"
            + changeFeedName
            + "', notification::text);\n"
            + "RETURN NULL;\n"
            + "END;\n"
            + "$$ LANGUAGE plpgsql;\n";
    String triggerStr =
        "CREATE TRIGGER "
            + changeFeedName
            + " AFTER INSERT OR DELETE OR UPDATE "
            + update_of
            + " ON "
            + objectTypeMetaData.getTableName()
            + " FOR EACH ROW EXECUTE PROCEDURE "
            + changeFeedName
            + "();";
    HashMap<String, String> resultMap = new HashMap<>();
    resultMap.put("code", function);
    resultMap.put("trigger", triggerStr);
    return resultMap;
  }
}
