package com.easygql.dao.postgres;

import com.easygql.dao.One2ManyRelationRemover;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class PostgreSqlOne2ManyRemover implements One2ManyRelationRemover{
  private String schemaID;
  private SchemaData schemaData;
  private RelationField relationField;
  private String fromSql1;
  private String fromSql2;
  private String toSql;
  private ObjectTypeMetaData fromObject;
  private ObjectTypeMetaData toObject;

  @Override
  public void Init(
      @NonNull SchemaData schemaData,
      @NonNull String schemaID,
      @NonNull RelationField relationField) {
    this.relationField = relationField;
    this.schemaData = schemaData;
    this.schemaID = schemaID;
    if (relationField.getIfCascade()) {
      this.fromSql1 =
          "delete from  "
              + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
              + " where "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "=$1 and "
              + POSTGRES_COLUMNNAME_PREFIX
              + GRAPHQL_ID_FIELDNAME
              + " = $2 ";
      this.fromSql2 =
          " delete from  "
              + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
              + "  where "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "=$1 ";
    } else {
      this.fromSql1 =
          "update "
              + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
              + " set  "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "= null  where "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "=$1 and "
              + POSTGRES_COLUMNNAME_PREFIX
              + GRAPHQL_ID_FIELDNAME
              + " = $2 ";
      this.fromSql2 =
          "update "
              + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
              + " set  "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "= null  where "
              + POSTGRES_COLUMNNAME_PREFIX
              + relationField.getToField()
              + "=$1 ";
    }

    this.toSql =
        "update "
            + schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName()
            + " set  "
            + POSTGRES_COLUMNNAME_PREFIX
            + relationField.getToField()
            + "= null  where "
            + POSTGRES_COLUMNNAME_PREFIX
            + GRAPHQL_ID_FIELDNAME
            + "=$1 ";
      this.fromObject=schemaData.getObjectMetaData().get(relationField.getFromObject());
      this.toObject = schemaData.getObjectMetaData().get(relationField.getToObject());
  }

  @Override
  public CompletableFuture<Object> fromRemove(@NonNull String srcID, List<String> destIDList) {
    if (null == destIDList) {
      return PostgreSqlOne2OneRemover.doRemove(fromSql2, Tuple.of(srcID), schemaData);
    } else {
      List<Tuple> tupleList = new ArrayList<>();
      for (String destID : destIDList) {
        tupleList.add(Tuple.of(srcID).addString(destID));
      }
      return doRemove(fromSql1, tupleList, schemaData);
    }
  }

  @Override
  public CompletableFuture<Object> toRemove(@NonNull String destID) {
    Tuple tuple = Tuple.of(destID);
    return PostgreSqlOne2OneRemover.doRemove(toSql, tuple, schemaData);
  }

  public static CompletableFuture<Object> doRemove(
      String sql, List<Tuple> tupleList, SchemaData schemaData) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
              PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                  if(null!=throwable) {
                      if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_SQL_FIELDNAME, sql);
                          errorMap.put(GRAPHQL_TUPLE_FIELDNAME, tupleList);
                          log.error(
                                  "{}",
                                  LogData.getErrorLog("E10003", errorMap, throwable));
                      }
                      future.completeExceptionally(
                              new BusinessException("E10003"));
                  } else  {
                      sqlConnection.preparedBatch(
                              sql,
                              tupleList,
                              removeHandler -> {
                                  if (removeHandler.succeeded()) {
                                      HashMap resultMap = new HashMap();
                                      resultMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME, 1);
                                      future.complete(resultMap);
                                  } else {
                                      if (log.isErrorEnabled()) {
                                          HashMap errorMap = new HashMap();
                                          errorMap.put(GRAPHQL_SQL_FIELDNAME, sql);
                                          errorMap.put(GRAPHQL_TUPLE_FIELDNAME, tupleList);
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
                  errorMap.put(GRAPHQL_TUPLE_FIELDNAME, tupleList);
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
