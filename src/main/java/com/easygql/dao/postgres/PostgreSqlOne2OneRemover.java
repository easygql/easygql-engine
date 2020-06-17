package com.easygql.dao.postgres;

import com.easygql.dao.*;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class PostgreSqlOne2OneRemover implements One2OneRelationRemover {
  private SchemaData schemaData;
  private String schemaID;
  private RelationField relationField;
  private String fromSql;
  private String toSql;
  private ObjectTypeMetaData fromObject;
  private ObjectTypeMetaData toObject;

  @Override
  public void Init(SchemaData schemaData, String schemaID, RelationField relationField) {
    this.schemaData = schemaData;
    this.schemaID = schemaID;
    this.relationField = relationField;
    if (relationField.getIfCascade()) {
      this.fromSql =
          " delete from   "
              + schemaData.getObjectMetaData().get(relationField.getToObject())
              + "where "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "= $1 ";
    } else {
      this.fromSql =
          " update  "
              + schemaData.getObjectMetaData().get(relationField.getToObject())
              + " set "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "=null where "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "= $1 ";
    }

    this.toSql =
        " delete from   "
            + schemaData.getObjectMetaData().get(relationField.getToObject())
            + " where "
            + POSTGRES_COLUMNNAME_PREFIX
            + POSTGRES_ID_FIELD
            + "= $1 ";
      this.fromObject=schemaData.getObjectMetaData().get(relationField.getFromObject());
      this.toObject = schemaData.getObjectMetaData().get(relationField.getToObject());
  }

  @Override
  public CompletableFuture<Object> fromRemove(@NonNull String srcID) {
    Tuple tuple = Tuple.of(srcID);
    return doRemove(fromSql, tuple, schemaData);
  }

  @Override
  public CompletableFuture<Object> toRemove(@NonNull String destID) {
    Tuple tuple = Tuple.of(destID);
    return doRemove(toSql, tuple, schemaData);
  }

  public static CompletableFuture<Object> doRemove(String sql, Tuple tuple, SchemaData schemaData) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
              PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                  if(null!=throwable) {
                      if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_SQL_FIELDNAME, sql);
                          errorMap.put(GRAPHQL_TUPLE_FIELDNAME, tuple);
                          log.error(
                                  "{}",
                                  LogData.getErrorLog("E10003", errorMap, throwable));
                      }
                      future.completeExceptionally(
                              new BusinessException("E10003"));
                  } else {
                      sqlConnection.preparedQuery(
                              sql,
                              tuple,
                              removeHandler -> {
                                  if (removeHandler.succeeded()) {
                                      HashMap resultMap = new HashMap();
                                      resultMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME, 1);
                                      future.complete(resultMap);
                                  } else {
                                      if (log.isErrorEnabled()) {
                                          HashMap errorMap = new HashMap();
                                          errorMap.put(GRAPHQL_SQL_FIELDNAME, sql);
                                          errorMap.put(GRAPHQL_TUPLE_FIELDNAME, tuple);
                                          log.error(
                                                  "{}",
                                                  LogData.getErrorLog("E10003", errorMap, removeHandler.cause()));
                                      }
                                      future.completeExceptionally(
                                              new BusinessException("E10003"));
                                  }
                                  sqlConnection.close();
                              });
                  }
              });
          } catch (Exception e) {
              if (log.isErrorEnabled()) {
                  HashMap errorMap = new HashMap();
                  errorMap.put(GRAPHQL_SQL_FIELDNAME, sql);
                  errorMap.put(GRAPHQL_TUPLE_FIELDNAME, tuple);
                  log.error(
                          "{}",
                          LogData.getErrorLog("E10003", errorMap, e));
              }
              future.completeExceptionally(
                      new BusinessException("E10003"));
          }
        });
    return future;
  }
}
