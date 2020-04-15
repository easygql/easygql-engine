package com.easygql.dao.postgres;

import com.alibaba.fastjson.JSONObject;
import com.easygql.dao.SchemaDao;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019/12/7 22:08
 */
@Slf4j
public class PostgreSqlSchema implements SchemaDao {

  private static final String databaseQuery =
      "select datname from pg_catalog.pg_database where datname = $1 ";
  private static final String databaseCreate_format = "create database %s";
  private static final String databaseDrop = " drop database if exists %s ";
  private static final String tableDrop = "drop table if exists %s;";
  private static final String tableCreate_prefix = "create table ";
  private static final String tableCreate_postfix = " );";
  private static final String jdbcURLFormat = "jdbc:postgresql://%s:%d/%s";
  private static final String connection_check = "select version()";
  private static final String closeDatabase =
      "UPDATE pg_database SET datallowconn = 'false' WHERE datname = '%s';";
  private static final String endConnection =
      "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s';";
  private static final String sequenceExistSQL =
      "SELECT c.relname FROM pg_class c WHERE c.relkind = 'S' and relname='global_id_sequence';";
  private static final String sequenceCreate =
      "CREATE SEQUENCE global_id_sequence;"
          + " CREATE FUNCTION generate_id(OUT result bigint) AS $$\n"
          + "  DECLARE\n"
          + "    -- TO START IDS SMALLER, YOU COULD CHANGE THIS TO A MORE RECENT UNIX TIMESTAMP\n"
          + "    our_epoch bigint := 1483228800;\n"
          + "\n"
          + "    seq_id bigint;\n"
          + "    now_millis bigint;\n"
          + "    -- UNIQUE SERVICE IDENTIFIER\n"
          + "    -- CHANGE THIS FOR EACH SERVICE!!!\n"
          + "    service_id int := 1;\n"
          + "  BEGIN\n"
          + "      SELECT nextval('global_id_sequence') % 1024 INTO seq_id;\n"
          + "      SELECT FLOOR(EXTRACT(EPOCH FROM clock_timestamp())) INTO now_millis;\n"
          + "      result := (now_millis - our_epoch) << 20;\n"
          + "      result := result | (service_id << 10);\n"
          + "      result := result | (seq_id);\n"
          + "  END;\n"
          + "$$ LANGUAGE PLPGSQL; ";

  @Override
  public CompletableFuture<Boolean> schemaInitial(SchemaData schemadata) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            DataSourceInfo pgDataSource = schemadata.getDatasourceinfo();
            if (null == pgDataSource.getDatabasename()
                || pgDataSource.getDatabasename().equals("")) {
              pgDataSource.setDatabasename(POSTGRES_DATABASENAME_PREFIX + schemadata.getSchemaid());
            }
            ifDataBaseExists(pgDataSource)
                .whenCompleteAsync(
                    (result, databaseEx) -> {
                      if (null != databaseEx) {
                        future.completeExceptionally(databaseEx);
                      } else {
                        if (!result) {
                          DataSourceInfo tmpDataSourceInfo =
                              JSONObject.parseObject(
                                  JSONObject.toJSONString(pgDataSource), DataSourceInfo.class);
                          tmpDataSourceInfo.setDatabasename(POSTGRES_DEFAULT_DATABSENAME);
                          try {
                            createDataBase(tmpDataSourceInfo, pgDataSource.getDatabasename()).get();
                          } catch (Exception e) {
                            future.completeExceptionally(e);
                            return;
                          }
                        }
                        tableInitial(schemadata)
                            .whenComplete(
                                (resultinfo, tableEx) -> {
                                  if (null != tableEx) {
                                    if (log.isErrorEnabled()) {
                                      HashMap errorLog = new HashMap();
                                      errorLog.put(GRAPHQL_SCHEMAOBJECT_FIELDNAME, schemadata);
                                      log.error(
                                          "{}", LogData.getErrorLog("E10005", errorLog, tableEx));
                                    }
                                    future.complete(false);
                                  } else {
                                    future.complete(resultinfo);
                                  }
                                });
                      }
                    });
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorLog = new HashMap();
              errorLog.put(GRAPHQL_SCHEMAOBJECT_FIELDNAME, schemadata);
              log.error("{}", LogData.getErrorLog("E10002", errorLog, e));
            }
            future.complete(false);
          }
        });
    return future;
  }

  @Override
  public CompletableFuture<Boolean> ifDataBaseExists(DataSourceInfo dataSourceInfo) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          PgConnection.connect(
              Vertx.vertx(),
              PostgreSQLPoolCache.getPgConnectionOptions(dataSourceInfo),
              pgConnectionAsyncResult -> {
                if (pgConnectionAsyncResult.succeeded()) {
                  result.complete(true);
                  pgConnectionAsyncResult.result().close();
                } else {
                  if (log.isErrorEnabled()) {
                    HashMap errorLog = new HashMap();
                    errorLog.put(GRAPHQL_DATASOURCE_FIELDNAME, dataSourceInfo);
                    log.error(
                        "{}",
                        LogData.getErrorLog("E10005", errorLog, pgConnectionAsyncResult.cause()));
                    ;
                  }
                  result.complete(false);
                }
              });
        });
    return result;
  }

  @Override
  public CompletableFuture<Boolean> createDataBase(
      DataSourceInfo dataSourceInfo, String databaseName) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          PgConnection.connect(
              Vertx.vertx(),
              PostgreSQLPoolCache.getPgConnectionOptions(dataSourceInfo),
              pgConnectionAsyncResult -> {
                if (pgConnectionAsyncResult.succeeded()) {
                  SqlConnection connection = pgConnectionAsyncResult.result();
                  connection.query(
                      String.format(databaseCreate_format, databaseName),
                      queryResult -> {
                        if (queryResult.succeeded()) {
                          future.complete(true);
                        } else {
                          future.complete(false);
                          if (log.isErrorEnabled()) {
                            HashMap errorLog = new HashMap();
                            errorLog.put(GRAPHQL_NAME_FIELDNAME, databaseName);
                            log.error(
                                "{}", LogData.getErrorLog("E10006", errorLog, queryResult.cause()));
                          }
                        }
                        connection.close();
                      });
                } else {
                  if (log.isErrorEnabled()) {
                    HashMap errorLog = new HashMap();
                    errorLog.put(GRAPHQL_DATASOURCE_FIELDNAME, dataSourceInfo);
                    log.error(
                        "{}",
                        LogData.getErrorLog("E10005", errorLog, pgConnectionAsyncResult.cause()));
                  }
                  future.complete(false);
                }
              });
        });
    return future;
  }

  /**
   * @param schemaData
   * @return
   */
  public CompletableFuture<Boolean> tableInitial(SchemaData schemaData) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            PgConnection.connect(
                Vertx.vertx(),
                PostgreSQLPoolCache.getPgConnectionOptions(schemaData.getDatasourceinfo()),
                pgConnectionAsyncResult -> {
                  if (pgConnectionAsyncResult.succeeded()) {
                    SqlConnection connection = pgConnectionAsyncResult.result();
                    connection.query(
                        sequenceExistSQL,
                        handler -> {
                          if (handler.succeeded()) {
                            Iterator<Row> iterator = handler.result().iterator();
                            if (!iterator.hasNext()) {
                              connection.query(
                                  sequenceCreate,
                                  sequenceCreateHandler -> {
                                    if (sequenceCreateHandler.succeeded()) {
                                      StringBuilder tableSQLBuilder = new StringBuilder();
                                      StringBuilder constraintSQLBuilder = new StringBuilder();
                                      for (ObjectTypeMetaData objectTypeMetaData :
                                          schemaData.getObjectMetaData().values()) {
                                        HashMap<String, String> tableResult =
                                            tableGenerator(objectTypeMetaData, schemaData);
                                        if (null != tableResult.get(GRAPHQL_TABLE_SQL)) {
                                          tableSQLBuilder.append(
                                              tableResult.get(GRAPHQL_TABLE_SQL));
                                        }
                                        if (null != tableResult.get(GRAPHQL_CONSTRAINT_SQL)) {
                                          constraintSQLBuilder.append(
                                              tableResult.get(GRAPHQL_CONSTRAINT_SQL));
                                        }
                                      }
                                      tableSQLBuilder.append(constraintSQLBuilder);
                                      final String sqlFinal = tableSQLBuilder.toString();
                                      connection.query(
                                          sqlFinal,
                                          tableCreateHandler -> {
                                            if (tableCreateHandler.succeeded()) {
                                              result.complete(true);
                                              PostgreSqlTriggerDao triggerDao =
                                                  new PostgreSqlTriggerDao();
                                              triggerDao.init(schemaData, schemaData.getSchemaid());
                                              for (ObjectTypeMetaData objectTypeMetaData :
                                                  schemaData.getObjectMetaData().values()) {
                                                triggerDao.triggerCreate(objectTypeMetaData);
                                              }
                                            } else {
                                              if (log.isErrorEnabled()) {
                                                HashMap errorLog = new HashMap();
                                                errorLog.put(GRAPHQL_QUERYAPI_FIELDNAME, sqlFinal);
                                                log.error(
                                                    "{}",
                                                    LogData.getErrorLog(
                                                        "E10095", errorLog, handler.cause()));
                                              }
                                              result.complete(false);
                                            }
                                            connection.close();
                                          });
                                    } else {
                                      if (log.isErrorEnabled()) {
                                        HashMap errorLog = new HashMap();
                                        errorLog.put(GRAPHQL_QUERYAPI_FIELDNAME, sequenceCreate);
                                        log.error(
                                            "{}",
                                            LogData.getErrorLog(
                                                "E10095", errorLog, sequenceCreateHandler.cause()));
                                      }
                                      result.completeExceptionally(new BusinessException("E10001"));
                                      connection.close();
                                    }
                                  });
                            } else {
                              StringBuilder tableSQLBuilder = new StringBuilder();
                              StringBuilder constraintSQLBuilder = new StringBuilder();
                              for (ObjectTypeMetaData objectTypeMetaData :
                                  schemaData.getObjectMetaData().values()) {
                                HashMap<String, String> tableResult =
                                    tableGenerator(objectTypeMetaData, schemaData);
                                if (null != tableResult.get(GRAPHQL_TABLE_SQL)) {
                                  tableSQLBuilder.append(tableResult.get(GRAPHQL_TABLE_SQL));
                                }
                                if (null != tableResult.get(GRAPHQL_CONSTRAINT_SQL)) {
                                  constraintSQLBuilder.append(
                                      tableResult.get(GRAPHQL_CONSTRAINT_SQL));
                                }
                              }
                              tableSQLBuilder.append(constraintSQLBuilder);
                              final String sqlFinal = tableSQLBuilder.toString();
                              connection.query(
                                  sqlFinal,
                                  tableCreateHandler -> {
                                    if (tableCreateHandler.succeeded()) {
                                      result.complete(true);
                                      PostgreSqlTriggerDao triggerDao = new PostgreSqlTriggerDao();
                                          triggerDao.init(schemaData,schemaData.getSchemaid());
                                          for (ObjectTypeMetaData objectTypeMetaData:schemaData.getObjectMetaData().values()) {
                                            triggerDao.triggerCreate(objectTypeMetaData);
                                      }
                                    } else {
                                      if (log.isErrorEnabled()) {
                                        HashMap errorLog = new HashMap();
                                        errorLog.put(GRAPHQL_QUERYAPI_FIELDNAME, sqlFinal);
                                        log.error(
                                            "{}",
                                            LogData.getErrorLog(
                                                "E10095", errorLog, handler.cause()));
                                      }
                                      result.complete(false);
                                    }
                                    connection.close();
                                  });
                            }
                          } else {
                            if (log.isErrorEnabled()) {
                              HashMap errorLog = new HashMap();
                              errorLog.put(GRAPHQL_QUERYAPI_FIELDNAME, sequenceExistSQL);
                              log.error(
                                  "{}", LogData.getErrorLog("E10095", errorLog, handler.cause()));
                            }
                            result.completeExceptionally(new BusinessException("E10095"));
                            connection.close();
                          }
                        });
                  } else {
                    if (log.isErrorEnabled()) {
                      HashMap errorLog = new HashMap();
                      errorLog.put(GRAPHQL_SCHEMAOBJECT_FIELDNAME, schemaData);
                      log.error(
                          "{}",
                          LogData.getErrorLog("E10005", errorLog, pgConnectionAsyncResult.cause()));
                    }
                    result.complete(false);
                  }
                });

          } catch (Exception e) {
            result.completeExceptionally(e);
          }
        });
    return result;
  }

  /**
   * @param objectTypeMetaData
   * @return
   */
  public static HashMap tableGenerator(
      ObjectTypeMetaData objectTypeMetaData, SchemaData schemaData) {
    HashMap resultMap = new HashMap();
    StringBuilder tableGeneratorSQL = new StringBuilder();
    StringBuilder constraintSQL = new StringBuilder();
    tableGeneratorSQL.append(String.format(tableDrop, objectTypeMetaData.getTableName()));
    String tableName = objectTypeMetaData.getTableName();
    tableGeneratorSQL.append("\n");
    tableGeneratorSQL.append(tableCreate_prefix).append(tableName).append(" ( ");
    int location = 0;
    List<RelationField> many2ManyRelationField = new ArrayList<>();
    for (ScalarFieldInfo scalarFieldInfo : objectTypeMetaData.getScalarFieldData().values()) {
      if (0 != location) {
        tableGeneratorSQL.append(",");
      }
      tableGeneratorSQL.append("\n");
      location++;
      tableGeneratorSQL
          .append(POSTGRES_COLUMNNAME_PREFIX)
          .append(scalarFieldInfo.getName())
          .append(" ");
      if (!scalarFieldInfo.isIslist()) {
        switch (scalarFieldInfo.getType()) {
          case GRAPHQL_ID_TYPENAME:
            tableGeneratorSQL.append(" varchar ");
            if (scalarFieldInfo.getName().equals(GRAPHQL_ID_FIELDNAME)) {
              tableGeneratorSQL.append(" PRIMARY KEY");
            }
            break;
          case GRAPHQL_INT_TYPENAME:
            tableGeneratorSQL.append(" integer");
            break;
          case GRAPHQL_STRING_TYPENAME:
          case GRAPHQL_URL_TYPENAME:
          case GRAPHQL_EMAIL_TYPENAME:
            tableGeneratorSQL.append(" varchar");
            break;
          case GRAPHQL_BOOLEAN_TYPENAME:
            tableGeneratorSQL.append(" boolean");
            break;
          case GRAPHQL_LONG_TYPENAME:
          case GRAPHQL_BIGINTEGER_TYPENAME:
            tableGeneratorSQL.append(" bigint");
            break;
          case GRAPHQL_SHORT_TYPENAME:
            tableGeneratorSQL.append(" smallint");
            break;
          case GRAPHQL_BYTE_TYPENAME:
            tableGeneratorSQL.append(" bytea");
            break;
          case GRAPHQL_DATE_TYPENAME:
            tableGeneratorSQL.append(" date");
            break;
          case GRAPHQL_FLOAT_TYPENAME:
            tableGeneratorSQL.append(" float8");
            break;
          case GRAPHQL_CHAR_TYPENAME:
            tableGeneratorSQL.append(" char(1)");
            break;
          case GRAPHQL_OBJECT_TYPENAME:
          case GRAPHQL_JSON_TYPENAME:
            tableGeneratorSQL.append(" json");
            break;
          case GRAPHQL_DATETIME_TYPENAME:
            tableGeneratorSQL.append(" timestamp");
            break;
          case GRAPHQL_CREATEDAT_TYPENAME:
          case GRAPHQL_LASTUPDATE_TYPENAME:
            tableGeneratorSQL.append(" timestamp default current_timestamp");
            break;
          case GRAPHQL_BIGDECIMAL_TYPENAME:
            tableGeneratorSQL.append(" decimal");
            break;
          default:
            throw new BusinessException("E10050");
        }
      } else {
        tableGeneratorSQL.append(" json ");
      }
      tableGeneratorSQL.append(" ");
      if (scalarFieldInfo.isNotnull()) {
        tableGeneratorSQL.append(" NOT NULL ");
      }
    }
    for (EnumField enumField : objectTypeMetaData.getEnumFieldData().values()) {
      tableGeneratorSQL.append("\n");
      if (0 != location) {
        tableGeneratorSQL.append(",");
      }
      location++;
      tableGeneratorSQL.append(POSTGRES_COLUMNNAME_PREFIX).append(enumField.getName()).append(" ");
      if (enumField.isIslist()) {
        tableGeneratorSQL.append(" json ");
      } else {
        tableGeneratorSQL.append(" varchar ");
      }
      tableGeneratorSQL.append(" ");
      if (enumField.isNotnull()) {
        tableGeneratorSQL.append(" NOT NULL ");
      }
    }
    for (RelationField relationField : objectTypeMetaData.getFromRelationFieldData().values()) {
      switch (relationField.getRelationtype()) {
        case GRAPHQL_MANY2ONE_NAME:
          if (0 != location) {
            tableGeneratorSQL.append(",");
          }
          tableGeneratorSQL.append("\n");

          location++;
          tableGeneratorSQL
              .append(POSTGRES_COLUMNNAME_PREFIX)
              .append(relationField.getFromfield())
              .append(" varchar");
          constraintSQL
              .append("\n")
              .append("ALTER TABLE ")
              .append(tableName)
              .append("  ADD CONSTRAINT ")
              .append(tableName)
              .append("_")
              .append(relationField.getFromfield())
              .append(GRAPHQL_FOREIGN_KEY_POSTFIX)
              .append(" FOREIGN KEY (")
              .append(POSTGRES_COLUMNNAME_PREFIX)
              .append(relationField.getFromfield())
              .append(") REFERENCES ")
              .append(POSTGRES_TABLENAME_PREFIX)
              .append(relationField.getToobject())
              .append(" (")
              .append(POSTGRES_COLUMNNAME_PREFIX)
              .append(GRAPHQL_ID_FIELDNAME)
              .append(") ON DELETE SET NULL;");
          break;
        case GRAPHQL_ONE2MANY_NAME:
        case GRAPHQL_ONE2ONE_NAME:
          break;
        case GRAPHQL_MANY2MANY_NAME:
          many2ManyRelationField.add(relationField);
      }
    }
    for (RelationField relationField : objectTypeMetaData.getToRelationFieldData().values()) {
      switch (relationField.getRelationtype()) {
        case GRAPHQL_MANY2ONE_NAME:
        case GRAPHQL_MANY2MANY_NAME:
          break;
        case GRAPHQL_ONE2ONE_NAME:
        case GRAPHQL_ONE2MANY_NAME:
          if (!relationField.getIfcascade()) {
            if (0 != location) {
              tableGeneratorSQL.append(",");
            }
            tableGeneratorSQL.append("\n");
            location++;
            tableGeneratorSQL
                .append(POSTGRES_COLUMNNAME_PREFIX)
                .append(relationField.getTofield())
                .append(" varchar ");
            constraintSQL
                .append("\n")
                .append("ALTER TABLE ")
                .append(tableName)
                .append("  ADD CONSTRAINT ")
                .append(tableName)
                .append("_")
                .append(relationField.getTofield())
                .append(GRAPHQL_FOREIGN_KEY_POSTFIX)
                .append(" FOREIGN KEY (")
                .append(POSTGRES_COLUMNNAME_PREFIX)
                .append(relationField.getTofield())
                .append(") REFERENCES ")
                .append(POSTGRES_TABLENAME_PREFIX)
                .append(relationField.getFromobject())
                .append(" (")
                .append(POSTGRES_COLUMNNAME_PREFIX)
                .append(GRAPHQL_ID_FIELDNAME)
                .append(") ON DELETE SET NULL;");
          } else {
            if (0 != location) {
              tableGeneratorSQL.append(",");
            }
            tableGeneratorSQL.append("\n");
            location++;
            tableGeneratorSQL
                .append(POSTGRES_COLUMNNAME_PREFIX)
                .append(relationField.getTofield())
                .append(" varchar ");
            constraintSQL
                .append("\n")
                .append("ALTER TABLE ")
                .append(tableName)
                .append("  ADD CONSTRAINT ")
                .append(tableName)
                .append("_")
                .append(relationField.getTofield())
                .append(GRAPHQL_FOREIGN_KEY_POSTFIX)
                .append(" FOREIGN KEY (")
                .append(POSTGRES_COLUMNNAME_PREFIX)
                .append(relationField.getTofield())
                .append(") REFERENCES ")
                .append(POSTGRES_TABLENAME_PREFIX)
                .append(relationField.getFromobject())
                .append(" (")
                .append(POSTGRES_COLUMNNAME_PREFIX)
                .append(GRAPHQL_ID_FIELDNAME)
                .append(") ON DELETE CASCADE;");
          }
          break;
      }
    }
    for (UniqueConstraint uniqueConstraint : objectTypeMetaData.getUniqueConstraints()) {
      String unqiueConstraintList = "";
      int uniqueConstraintLoc = 0;
      for (String fieldname : uniqueConstraint.getFieldNames()) {
        if (0 != uniqueConstraintLoc) {
          unqiueConstraintList += ",";
        }
        uniqueConstraintLoc++;
        unqiueConstraintList += fieldname;
      }
      tableGeneratorSQL
          .append(",\n")
          .append(" CONSTRAINT ")
          .append(uniqueConstraint.getConstraintName())
          .append(" UNIQUE(")
          .append(unqiueConstraintList)
          .append(")");
    }
    tableGeneratorSQL.append("\n");
    tableGeneratorSQL.append(tableCreate_postfix).append("\n");
    for (RelationField relationField : many2ManyRelationField) {
      String tableNameTmp = getTableNameofRelation(relationField);
      tableGeneratorSQL.append(String.format(tableDrop, tableNameTmp));
      tableGeneratorSQL
          .append("\n")
          .append(tableCreate_prefix)
          .append(tableNameTmp)
          .append(" ( \n");
      tableGeneratorSQL
          .append(POSTGRES_FROM_COLUMNNAME)
          .append("  varchar not null")
          .append(",\n")
          .append(POSTGRES_TO_COLUMNNAME)
          .append(",\n PRIMARY KEY(")
          .append(POSTGRES_FROM_COLUMNNAME)
          .append(",")
          .append(POSTGRES_TO_COLUMNNAME)
          .append(") \n)\n");
      constraintSQL
          .append("\n")
          .append("ALTER TABLE ")
          .append(tableNameTmp)
          .append("  ADD CONSTRAINT ")
          .append(tableNameTmp)
          .append("-")
          .append(POSTGRES_FROM_COLUMNNAME)
          .append(GRAPHQL_FOREIGN_KEY_POSTFIX)
          .append(" FOREIGN KEY (")
          .append(POSTGRES_FROM_COLUMNNAME)
          .append(") REFERENCES ")
          .append(POSTGRES_TABLENAME_PREFIX)
          .append(relationField.getFromobject())
          .append(" (")
          .append(POSTGRES_COLUMNNAME_PREFIX)
          .append(GRAPHQL_ID_FIELDNAME)
          .append(") ON DELETE CASCADE;");
      constraintSQL
          .append("\n")
          .append("ALTER TABLE ")
          .append(tableNameTmp)
          .append("  ADD CONSTRAINT ")
          .append(tableNameTmp)
          .append("-")
          .append(POSTGRES_TO_COLUMNNAME)
          .append(GRAPHQL_FOREIGN_KEY_POSTFIX)
          .append(" FOREIGN KEY (")
          .append(POSTGRES_TO_COLUMNNAME)
          .append(") REFERENCES ")
          .append(POSTGRES_TABLENAME_PREFIX)
          .append(relationField.getToobject())
          .append(" (")
          .append(POSTGRES_COLUMNNAME_PREFIX)
          .append(GRAPHQL_ID_FIELDNAME)
          .append(") ON DELETE CASCADE;");
    }
    if (tableGeneratorSQL.length() > 0) {
      resultMap.put(GRAPHQL_TABLE_SQL, tableGeneratorSQL.toString());
    }
    if (constraintSQL.length() > 0) {
      resultMap.put(GRAPHQL_CONSTRAINT_SQL, constraintSQL.toString());
    }
    return resultMap;
  }

  public static String getTableNameofRelation(@NonNull RelationField relationField) {
    return POSTGRES_TABLENAME_PREFIX
        + relationField.getFromobject()
        + "_"
        + relationField.getFromfield();
  }
}
