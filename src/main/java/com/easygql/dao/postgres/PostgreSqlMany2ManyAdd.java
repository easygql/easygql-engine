package com.easygql.dao.postgres;

import com.easygql.dao.Many2ManyRelationCreater;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class PostgreSqlMany2ManyAdd implements Many2ManyRelationCreater {
  private String schemaID;
  private SchemaData schemaData;
  private RelationField relationField;
  private String fromSql;
  private String fromResetSql;
  private String toResetSql;
  private String toSql;
  private ObjectTypeMetaData fromObject;
  private ObjectTypeMetaData toObject;

  @Override
  public void Init(SchemaData schemaData, String schemaID, RelationField relationField) {
    this.schemaData = schemaData;
    this.schemaID = schemaID;
    this.relationField = relationField;
    this.fromObject=schemaData.getObjectMetaData().get(relationField.getFromobject());
    this.toObject = schemaData.getObjectMetaData().get(relationField.getToobject());
    fromSql =
        " insert into "
            + PostgreSqlSchema.getTableNameofRelation(relationField)
            + "("
            + POSTGRES_FROM_COLUMNNAME
            + ","
            + POSTGRES_TO_COLUMNNAME
            + ") values($1,$2)  ";
    fromResetSql =
        "delete from "
            + PostgreSqlSchema.getTableNameofRelation(relationField)
            + " where "
            + POSTGRES_FROM_COLUMNNAME
            + "=$1  ";
    String toResetSql =
        " delete from "
            + PostgreSqlSchema.getTableNameofRelation(relationField)
            + " where "
            + POSTGRES_TO_COLUMNNAME
            + "=$1 ";
    String toSql =
        " insert into "
            + PostgreSqlSchema.getTableNameofRelation(relationField)
            + "("
            + POSTGRES_TO_COLUMNNAME
            + ","
            + POSTGRES_FROM_COLUMNNAME
            + ") values($1,$2)  ";
  }

  @Override
  public CompletableFuture<Object> fromAdd(
      @NonNull String srcID, @NonNull List<String> targetList, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    doAdd(fromResetSql, fromSql, srcID, targetList, reset, future, schemaData);
    return future;
  }

  @Override
  public CompletableFuture<Object> toAdd(
      @NonNull String destID, @NonNull List<String> srcIDList, Boolean reset) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    doAdd(toResetSql, toSql, destID, srcIDList, reset, future, schemaData);
    return future;
  }

  public static void doAdd(
      String resetSql,
      String insertSql,
      String srcID,
      List<String> idList,
      Boolean reset,
      CompletableFuture future,
      SchemaData schemaData) {
    CompletableFuture.runAsync(
        () -> {
            if(reset) {
                PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                    if(null!=throwable){
                        future.completeExceptionally(new BusinessException("E10004"));
                        HashMap errorDetail = new HashMap();
                        errorDetail.put(GRAPHQL_FROM_ID, srcID);
                        errorDetail.put(GRAPHQL_TO_ID, idList);
                        errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                        errorDetail.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaData.getSchemaid());
                        log.error(
                                "{}",
                                LogData.getErrorLog("E10005", errorDetail, throwable));
                    } else {
                        Transaction tx = sqlConnection.begin();
                        sqlConnection.preparedQuery(
                                resetSql,
                                Tuple.of(srcID),
                                resetHandler -> {
                                    if (resetHandler.succeeded()) {
                                        List<Tuple> tupleList = new ArrayList<>();
                                        idList.forEach(
                                                targetID -> {
                                                    tupleList.add(Tuple.of(srcID).addString(targetID));
                                                });
                                        sqlConnection.preparedBatch(
                                                insertSql,
                                                tupleList,
                                                insertHandler -> {
                                                    if (insertHandler.succeeded()) {
                                                        tx.commit(
                                                                transactionHandler -> {
                                                                    if (transactionHandler.succeeded()) {
                                                                        HashMap<String, Object> resultMap = new HashMap<>();
                                                                        resultMap.put(
                                                                                GRAPHQL_AFFECTEDROW_FIELDNAME, tupleList.size());
                                                                        resultMap.put(GRAPHQL_IDLIST_FIELDNAME, idList);
                                                                        future.complete(resultMap);
                                                                    } else {
                                                                        future.completeExceptionally(
                                                                                new BusinessException("E10004"));
                                                                        if (log.isErrorEnabled()) {
                                                                            HashMap errorDetail = new HashMap();
                                                                            errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                                                            errorDetail.put(GRAPHQL_TO_ID, idList);
                                                                            errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                                                            errorDetail.put(
                                                                                    GRAPHQL_SCHEMAID_FIELDNAME,
                                                                                    schemaData.getSchemaid());
                                                                            log.error(
                                                                                    "{}",
                                                                                    LogData.getErrorLog(
                                                                                            "E10004",
                                                                                            errorDetail,
                                                                                            insertHandler.cause()));
                                                                        }
                                                                    }
                                                                    tx.close();
                                                                    sqlConnection.close();
                                                                });
                                                    }
                                                });
                                    } else {
                                        tx.close();
                                        sqlConnection.close();
                                        future.completeExceptionally(new BusinessException("E10004"));
                                        if (log.isErrorEnabled()) {
                                            HashMap errorDetail = new HashMap();
                                            errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                            errorDetail.put(GRAPHQL_TO_ID, idList);
                                            errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                            errorDetail.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaData.getSchemaid());
                                            log.error(
                                                    "{}",
                                                    LogData.getErrorLog("E10004", errorDetail, resetHandler.cause()));
                                        }
                                    }
                                });
                    }
                });
            } else {
                PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                    if(null!=throwable) {
                        future.completeExceptionally(new BusinessException("E10004"));
                        if (log.isErrorEnabled()) {
                            HashMap errorDetail = new HashMap();
                            errorDetail.put(GRAPHQL_FROM_ID, srcID);
                            errorDetail.put(GRAPHQL_TO_ID, idList);
                            errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                            errorDetail.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaData.getSchemaid());
                            log.error(
                                    "{}",
                                    LogData.getErrorLog("E10005", errorDetail, throwable));
                        }
                    } else {
                        List<Tuple> tupleList = new ArrayList<>();
                        idList.forEach(
                                targetID -> {
                                    tupleList.add(Tuple.of(srcID).addString(targetID));
                                });
                        sqlConnection.preparedBatch(
                                insertSql,
                                tupleList,
                                insertHandler -> {
                                    if (insertHandler.succeeded()) {
                                        HashMap<String, Object> resultMap = new HashMap<>();
                                        resultMap.put(GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME, idList);
                                        future.complete(resultMap);
                                    } else {
                                        future.completeExceptionally(new BusinessException("E10004"));
                                        if (log.isErrorEnabled()) {
                                            HashMap errorDetail = new HashMap();
                                            errorDetail.put(GRAPHQL_FROM_ID, srcID);
                                            errorDetail.put(GRAPHQL_TO_ID, idList);
                                            errorDetail.put(GRAPHQL_RESET_FIELDNAME, reset);
                                            errorDetail.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaData.getSchemaid());
                                            log.error(
                                                    "{}",
                                                    LogData.getErrorLog(
                                                            "E10004", errorDetail, insertHandler.cause()));
                                        }
                                    }
                                    sqlConnection.close();
                                });
                    }
                });
            }
        });
  }
}
