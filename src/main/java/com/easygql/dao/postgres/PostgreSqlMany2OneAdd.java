package com.easygql.dao.postgres;

import com.easygql.dao.Many2OneRelationCreater;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class PostgreSqlMany2OneAdd implements Many2OneRelationCreater {
  private String schemaID;
  private SchemaData schemaData;
  private RelationField relationField;
  private String fromSql;
  private String toResetSql;
  private String toSql;
  private ObjectTypeMetaData fromObject;
  private ObjectTypeMetaData toObject;

  @Override
  public void Init(SchemaData schemaData, String schemaID, RelationField relationField) {
    this.schemaData = schemaData;
    this.schemaID = schemaID;
    this.relationField = relationField;
    this.fromObject = schemaData.getObjectMetaData().get(relationField.getFromobject());
    this.toObject = schemaData.getObjectMetaData().get(relationField.getToobject());
    this.fromSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getFromobject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getFromfield()
            + " = $1  where "
            + POSTGRES_COLUMNNAME_PREFIX
            + POSTGRES_ID_FIELD
            + "=$2 ";
    this.toResetSql =
        " update "
            + schemaData.getObjectMetaData().get(relationField.getFromobject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getFromfield()
            + " = null where "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getFromfield()
            + " = $1 ";
    this.toSql =
        " update  "
            + schemaData.getObjectMetaData().get(relationField.getFromobject()).getTableName()
            + " set "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getFromfield()
            + " =$1 where "
            + POSTGRES_COLUMNNAME_PREFIX
            + POSTGRES_ID_FIELD
            + "=$2 ";
  }

  @Override
  public CompletableFuture<Object> fromAdd(String srcID, String destID, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
              PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                 if(null!=throwable) {
                     future.completeExceptionally(new BusinessException("E10004"));
                     if (log.isErrorEnabled()) {
                         HashMap errorDetail = new HashMap();
                         errorDetail.put(GRAPHQL_FROM_ID, srcID);
                         errorDetail.put(GRAPHQL_TO_ID, destID);
                         errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                         log.error(
                                 "{}",
                                 LogData.getErrorLog(
                                         "E10005", errorDetail, throwable));
                     }
                 }  else {
                     Transaction transaction = sqlConnection.begin();
                     if (reset) {
                         sqlConnection.preparedQuery(
                                 toResetSql,
                                 Tuple.of(destID),
                                 resetHandler -> {
                                     if (resetHandler.succeeded()) {
                                         sqlConnection.preparedQuery(
                                                 fromSql,
                                                 Tuple.of(destID).addString(srcID),
                                                 updateHandler -> {
                                                     if (updateHandler.succeeded()) {
                                                         transaction.commit(
                                                                 transactionHandler -> {
                                                                     if (transactionHandler.succeeded()) {
                                                                         HashMap resultMap = new HashMap();
                                                                         List<String> idList = new ArrayList<>();
                                                                         idList.add(destID);
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
                                                         future.completeExceptionally(new BusinessException("E10004"));
                                                         if (log.isErrorEnabled()) {
                                                             HashMap errorDetail = new HashMap();
                                                             errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                                             errorDetail.put(GRAPHQL_TO_ID, destID);
                                                             errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                                             log.error(
                                                                     "{}",
                                                                     LogData.getErrorLog(
                                                                             "E10004", errorDetail, updateHandler.cause()));
                                                         }
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
                                 Tuple.of(destID).addString(srcID),
                                 updateHandler -> {
                                     if (updateHandler.succeeded()) {
                                         transaction.commit(
                                                 transactionHandler -> {
                                                     if (transactionHandler.succeeded()) {
                                                         HashMap resultMap = new HashMap();
                                                         List<String> idList = new ArrayList<>();
                                                         idList.add(destID);
                                                         resultMap.put(GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                         future.complete(resultMap);
                                                     } else {
                                                         future.completeExceptionally(new BusinessException("E10007"));
                                                     }
                                                     transaction.close();
                                                     sqlConnection.close();
                                                 });
                                     } else {
                                         transaction.close();
                                         sqlConnection.close();
                                         future.completeExceptionally(new BusinessException("E10004"));
                                         if (log.isErrorEnabled()) {
                                             HashMap errorDetail = new HashMap();
                                             errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                             errorDetail.put(GRAPHQL_TO_ID, destID);
                                             errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                             log.error(
                                                     "{}",
                                                     LogData.getErrorLog(
                                                             "E10004", errorDetail, updateHandler.cause()));
                                         }
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
  public CompletableFuture<Object> toAdd(List<String> srcID, String destID, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture();
      CompletableFuture.runAsync(
              () -> {
                  try {
                      List<Tuple> tupleList = new ArrayList<>();
                      for (String srcIDSingle : srcID ) {
                          tupleList.add(Tuple.of(destID).addString(srcIDSingle));
                      }
                      PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                          if(null!=throwable) {
                              future.completeExceptionally(new BusinessException("E10004"));
                              if (log.isErrorEnabled()) {
                                  HashMap errorDetail = new HashMap();
                                  errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                  errorDetail.put(GRAPHQL_TO_ID, destID);
                                  errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                  log.error(
                                          "{}",
                                          LogData.getErrorLog(
                                                  "E10005", errorDetail, throwable));
                              }
                          } else {
                              Transaction transaction = sqlConnection.begin();
                              if (reset) {
                                  sqlConnection.preparedQuery(
                                          toResetSql,
                                          Tuple.of(destID),
                                          resetHandler -> {
                                              if (resetHandler.succeeded()) {
                                                  sqlConnection.preparedBatch(
                                                          fromSql,
                                                          tupleList,
                                                          updateHandler -> {
                                                              if (updateHandler.succeeded()) {
                                                                  transaction.commit(
                                                                          transactionHandler -> {
                                                                              if (transactionHandler.succeeded()) {
                                                                                  HashMap resultMap = new HashMap();
                                                                                  List<String> idList = new ArrayList<>();
                                                                                  idList.add(destID);
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
                                                                  future.completeExceptionally(new BusinessException("E10004"));
                                                                  if (log.isErrorEnabled()) {
                                                                      HashMap errorDetail = new HashMap();
                                                                      errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                                                      errorDetail.put(GRAPHQL_TO_ID, destID);
                                                                      errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                                                      log.error(
                                                                              "{}",
                                                                              LogData.getErrorLog(
                                                                                      "E10004", errorDetail, updateHandler.cause()));
                                                                  }
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
                                          updateHandler -> {
                                              if (updateHandler.succeeded()) {
                                                  transaction.commit(
                                                          transactionHandler -> {
                                                              if (transactionHandler.succeeded()) {
                                                                  HashMap resultMap = new HashMap();
                                                                  List<String> idList = new ArrayList<>();
                                                                  idList.add(destID);
                                                                  resultMap.put(GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                                                  future.complete(resultMap);
                                                              } else {
                                                                  future.completeExceptionally(new BusinessException("E10007"));
                                                              }
                                                              transaction.close();
                                                              sqlConnection.close();
                                                          });
                                              } else {
                                                  transaction.close();
                                                  sqlConnection.close();
                                                  future.completeExceptionally(new BusinessException("E10004"));
                                                  if (log.isErrorEnabled()) {
                                                      HashMap errorDetail = new HashMap();
                                                      errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                                      errorDetail.put(GRAPHQL_TO_ID, destID);
                                                      errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                                      log.error(
                                                              "{}",
                                                              LogData.getErrorLog(
                                                                      "E10004", errorDetail, updateHandler.cause()));
                                                  }
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
}
