package com.easygql.dao.postgres;

import com.easygql.dao.DataInserter;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * PostgreSQL的数据插入工具类
 *
 * @author guofen
 * @date 2019/11/3 14:59
 */
@Slf4j
public class PostgreSqlInserter implements DataInserter {
  private String objectName;
  private SchemaData schemaData;
  private String schemaId;
  private String insertSQL;
  private List<String> fieldSequence;
  private ObjectTypeMetaData objectTypeMetaData;
  /**
   * @param objectName
   * @param schemaData
   * @param schemaID
   */
  @Override
  public void Init(String objectName, SchemaData schemaData, String schemaID) {
    this.objectName = objectName;
    this.schemaData = schemaData;
    this.schemaId = schemaID;
    this.fieldSequence = new ArrayList<>();
    this.insertSQL = insertSqlConstruct(schemaData, objectName, this.fieldSequence);
    this.objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
  }

  /**
   * 插入doc对象
   *
   * @param resultInfo
   * @param conflictStrategy
   * @return
   */
  @Override
  public CompletableFuture<HashMap> insertDoc(
      @NonNull HashMap<String, HashMap> resultInfo,
      String conflictStrategy) {
    CompletableFuture<HashMap> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            List<Tuple> tuples = new ArrayList<>();
            for (HashMap resultData : resultInfo.values()) {
              Tuple tuple = Tuple.tuple();
              for (String field : fieldSequence) {
                if (null == resultData.get(field)) {
                  tuple.addValue(null);
                } else {
                  Object objectValue = resultData.get(field);
                  if (objectTypeMetaData
                      .getFields()
                      .get(field)
                      .equals(GRAPHQL_ENUMFIELD_TYPENAME)) {
                    if (objectTypeMetaData.getEnumFieldData().get(field).isList()) {
                      List<String> tmpEnumList = (List<String>) objectValue;
                      tuple.addStringArray(tmpEnumList.toArray(new String[tmpEnumList.size()]));
                    } else {
                      tuple.addValue(objectValue);
                    }
                  } else if (objectTypeMetaData.getFields().get(field).equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
                    ScalarFieldInfo scalarFieldInfo =
                        objectTypeMetaData.getScalarFieldData().get(field);
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
              tuples.add(tuple);
            }
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid())
                .whenCompleteAsync(
                    ((connection, throwable) -> {
                      if (null != throwable) {
                        if (log.isErrorEnabled()) {
                          HashMap detailMap = new HashMap();
                          detailMap.put(GRAPHQL_OBJECTS_ARGUMENT, resultInfo);
                          detailMap.put(GRAPHQL_CONFLICT_ARGUMENT, conflictStrategy);
                          log.error("{}", LogData.getErrorLog("E10014", detailMap, throwable));
                        }
                        future.completeExceptionally(new BusinessException("E10014"));
                      } else {
                        Transaction transaction = connection.begin();
                        connection.preparedBatch(
                            insertSQL,
                            tuples,
                            handler -> {
                              if (handler.succeeded()) {
                                transaction.commit(
                                    transactionHandler -> {
                                      if (transactionHandler.succeeded()) {
                                        HashMap resultMap = new HashMap(1);
                                        List<String> idList = new ArrayList<>();
                                        idList.addAll(resultInfo.keySet());
                                        resultMap.put(GRAPHQL_IDLIST_FIELDNAME, idList);
                                        resultMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME, idList.size());
                                        future.complete(resultMap);
                                      } else {
                                        if (log.isErrorEnabled()) {
                                          HashMap detailMap = new HashMap();
                                          detailMap.put(GRAPHQL_OBJECTS_ARGUMENT, resultInfo);
                                          detailMap.put(
                                              GRAPHQL_CONFLICT_ARGUMENT, conflictStrategy);
                                          log.error(
                                              "{}",
                                              LogData.getErrorLog(
                                                  "E10007", detailMap, transactionHandler.cause()));
                                        }
                                        future.completeExceptionally(
                                            new BusinessException("E10007"));
                                      }
                                      transaction.close();
                                      connection.close();
                                    });
                              } else {
                                if (log.isErrorEnabled()) {
                                  HashMap detailMap = new HashMap();
                                  detailMap.put(GRAPHQL_OBJECTS_ARGUMENT, resultInfo);
                                  detailMap.put(GRAPHQL_CONFLICT_ARGUMENT, conflictStrategy);
                                  log.error(
                                      "{}",
                                      LogData.getErrorLog("E10014", detailMap, handler.cause()));
                                }
                                future.completeExceptionally(new BusinessException("E10014",handler.cause().getMessage()));
                                transaction.close();
                                connection.close();
                              }
                            });
                      }
                    }));
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        });
    return future;
  }

  public static String insertSqlConstruct(
      SchemaData schemaData, String objectName, List<String> insertFields) {
    ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
    StringBuilder insertSQLTmp = new StringBuilder();
    StringBuilder valueSQLTmp = new StringBuilder();
    insertSQLTmp.append(" insert into ").append(objectTypeMetaData.getTableName()).append(" ( ");
    valueSQLTmp.append(" values ( ");
    int fieldLoc = 0;
    Iterator<Map.Entry<String, String>> iterator =
        objectTypeMetaData.getFields().entrySet().iterator();
    while (iterator.hasNext()) {
      boolean ifAdd = false;
      Map.Entry<String, String> entry = iterator.next();
      String fieldName = entry.getKey();
      String fieldType = entry.getValue();
      if (fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)
          || fieldType.equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
        ifAdd = true;
      } else if (fieldType.equals(GRAPHQL_FROMRELATION_TYPENAME)) {
        RelationField relationField = objectTypeMetaData.getFromRelationFieldData().get(fieldName);
        if (relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
          ifAdd = true;
        }
      } else {
        RelationField relationField = objectTypeMetaData.getToRelationFieldData().get(fieldName);
        if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
            || relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
          ifAdd = true;
        }
      }
      if (ifAdd) {
        if (0 != fieldLoc) {
          insertSQLTmp.append(",");
          valueSQLTmp.append(",");
        }
        fieldLoc++;
        insertSQLTmp.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldName);
        valueSQLTmp.append("$").append(fieldLoc);
        insertFields.add(fieldName);
      }
    }
    insertSQLTmp.append(" ) ");
    valueSQLTmp.append(") ");
    return insertSQLTmp.append(valueSQLTmp).toString();
  }
}
