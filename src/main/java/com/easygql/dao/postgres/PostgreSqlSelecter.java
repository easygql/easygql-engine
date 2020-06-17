package com.easygql.dao.postgres;

import com.easygql.dao.DataSelecter;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019/11/3 15:07
 */
@Slf4j
public class PostgreSqlSelecter implements DataSelecter {
  private SchemaData schemaData;
  private String schemadid;
  private HashSet<String> disabledRoles;
  private HashMap<String, HashSet> forbiddenFields;
  private String objectName;
  private ObjectTypeMetaData objectTypeMetaData;

  @Override
  public void Init(String objectName, SchemaData schemaData, String schemaID) {
    this.objectName = objectName;
    this.schemaData = schemaData;
    this.schemadid = schemaID;
    this.disabledRoles = new HashSet<>();
    this.disabledRoles.addAll(schemaData.getObjectMetaData().get(objectName).getUnreadableRoles());
    this.forbiddenFields = new HashMap<>();
    ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
    for (ScalarFieldInfo scalarFieldInfo : objectTypeMetaData.getScalarFieldData().values()) {
      if (null != scalarFieldInfo.getInvisibleRoles()) {
        for (String roleName : scalarFieldInfo.getUnmodifiableRoles()) {
          if (null == forbiddenFields.get(roleName)) {
            forbiddenFields.put(roleName, new HashSet());
          }
          forbiddenFields.get(roleName).add(scalarFieldInfo.getName());
        }
      }
    }
    for (EnumField enumField : objectTypeMetaData.getEnumFieldData().values()) {
      if (null != enumField.getInvisibleRoles()) {
        for (String roleName : enumField.getInvisibleRoles()) {
          if (null == forbiddenFields.get(roleName)) {
            forbiddenFields.put(roleName, new HashSet());
          }
          forbiddenFields.get(roleName).add(enumField.getName());
        }
      }
    }
    for (RelationField relationField : objectTypeMetaData.getFromRelationFieldData().values()) {
      for (String roleName : relationField.getInvisibleRoles()) {
        if (null == forbiddenFields.get(roleName)) {
          forbiddenFields.put(roleName, new HashSet());
        }
        forbiddenFields.get(roleName).add(relationField.getFromField());
      }
    }
    for (RelationField relationField : objectTypeMetaData.getToRelationFieldData().values()) {
      for (String roleName : relationField.getInvisibleRoles()) {
        if (null == forbiddenFields.get(roleName)) {
          forbiddenFields.put(roleName, new HashSet());
        }
        forbiddenFields.get(roleName).add(relationField.getToField());
      }
    }
    this.objectTypeMetaData= schemaData.getObjectMetaData().get(objectName);
  }

  @Override
  public CompletableFuture<Map> getSingleDoc(
      Object condition, HashMap<String, Object> selectFields) {
    CompletableFuture<Map> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(
                getSelectionField(
                    selectFields,
                    schemaData,
                    objectName,
                    schemaData.getObjectMetaData().get(objectName).getTableName()));
            String whereClause =
                getWhereCondition(HashMap.class.cast(condition), objectName, schemaData);
            if (null != whereClause) {
              stringBuilder
                  .append(" where ")
                  .append(POSTGRES_COLUMNNAME_PREFIX)
                  .append(POSTGRES_ID_FIELD)
                  .append(" in ( ")
                  .append(" select ")
                  .append(POSTGRES_COLUMNNAME_PREFIX)
                  .append(POSTGRES_ID_FIELD)
                  .append(" from ");
              stringBuilder.append(schemaData.getObjectMetaData().get(objectName).getTableName());
              stringBuilder.append(" where ").append(whereClause);
              stringBuilder.append(")");
            }
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid())
                .whenCompleteAsync(
                    (sqlConnection, throwable) -> {
                      if (null != throwable) {
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, condition);
                          log.error("{}", LogData.getErrorLog("E10008", errorMap, throwable));
                        }
                        future.completeExceptionally(new BusinessException("E10008"));
                      } else {
                        sqlConnection.query(
                            stringBuilder.toString(),
                            resultHandler -> {
                              if (resultHandler.succeeded()) {
                                Iterator<Row> iterator = resultHandler.result().iterator();
                                if (iterator.hasNext()) {
                                  Row row = iterator.next();
                                  HashMap result = new HashMap();
                                  for (String key : selectFields.keySet()) {
                                    Object tmpObject = row.getValue(key);
                                    if (tmpObject instanceof JsonArray) {
                                      JsonArray tmpArray = (JsonArray) tmpObject;
                                      result.put(key, tmpArray.getList());
                                    } else if (tmpObject instanceof JsonObject) {
                                      JsonObject tmpJsonObject = (JsonObject) tmpObject;
                                      result.put(key, tmpJsonObject.getMap());
                                    } else {
                                      result.put(key,tmpObject);
                                    }
                                  }
                                  future.complete(result);
                                } else {
                                  future.complete(null);
                                }
                              } else {
                                if (log.isErrorEnabled()) {
                                  HashMap errorMap = new HashMap();
                                  errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, condition);
                                  log.error(
                                      "{}",
                                      LogData.getErrorLog(
                                          "E10008", errorMap, resultHandler.cause()));
                                }
                                future.completeExceptionally(new BusinessException("E10008"));
                              }
                              sqlConnection.close();
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
  public CompletableFuture<List<Map>> getFilterDocs(
      Object inputObj,
      Integer skip,
      Integer limit,
      String orderby,
      HashMap<String, Object> selectFields) {
    CompletableFuture<List<Map>> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            StringBuilder stringBuilder = new StringBuilder();
            if (null == selectFields) {
              System.currentTimeMillis();
            }
            stringBuilder.append(
                getSelectionField(
                    selectFields,
                    schemaData,
                    objectName,
                    schemaData.getObjectMetaData().get(objectName).getTableName()));
            String whereClause =
                getWhereCondition(HashMap.class.cast(inputObj), objectName, schemaData);
            StringBuilder pagingClause = new StringBuilder();
            StringBuilder orderByClause = new StringBuilder().append(" order by ");
            if (limit < Integer.MAX_VALUE) {
              pagingClause.append(" limit ").append(limit).append(" ");
            }
            if (0 < skip) {
              pagingClause.append(" offset ").append(skip).append(" ");
            }
            int sortLoc = 1;
            if (null != orderby) {
              String[] sortArray = orderby.split(",");
              while (sortLoc < sortArray.length) {
                if (sortLoc > 1) {
                  orderByClause.append(",");
                }
                orderByClause.append(sortArray[sortLoc - 1]);
                sortLoc++;
              }
              if (sortArray.length < 2) {
                throw new BusinessException("E10063");
              }
              if (GRAPHQL_ASC_POSTFIX.equals(sortArray[sortArray.length - 1])) {
                orderByClause.append(" ").append(GRAPHQL_ASC_POSTFIX);
              } else if (GRAPHQL_DESC_POSTFIX.equals(sortArray[sortArray.length - 1])) {
                orderByClause.append(" ").append(GRAPHQL_DESC_POSTFIX);
              } else {
                throw new BusinessException("E10063");
              }
            }
            if (null != whereClause) {
              stringBuilder
                  .append(" where ")
                  .append(POSTGRES_COLUMNNAME_PREFIX)
                  .append(POSTGRES_ID_FIELD)
                  .append(" in ( ")
                  .append(" select ")
                  .append(POSTGRES_COLUMNNAME_PREFIX)
                  .append(POSTGRES_ID_FIELD)
                  .append(" from ");
              stringBuilder.append(schemaData.getObjectMetaData().get(objectName).getTableName());
              stringBuilder.append(" where ").append(whereClause);
              stringBuilder.append(")");
            }
            if (sortLoc > 1) {
              stringBuilder.append(orderByClause);
            }
            if (pagingClause.length() > 1) {
              stringBuilder.append(pagingClause);
            }
            PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid())
                .whenCompleteAsync(
                    (sqlConnection, throwable) -> {
                      if (null != throwable) {
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_OBJECT_ARGUMENT, inputObj);
                          errorMap.put(GRAPHQL_SKIP_ARGUMENT, skip);
                          errorMap.put(GRAPHQL_LIMIT_ARGUMENT, limit);
                          errorMap.put(GRAPHQL_ORDERBY_ARGUMENT, orderby);
                          log.error("{}", LogData.getErrorLog("E10008", errorMap, throwable));
                        }
                        future.completeExceptionally(new BusinessException("E10008"));
                      } else {
                        sqlConnection.query(
                            stringBuilder.toString(),
                            resultHandler -> {
                              if (resultHandler.succeeded()) {
                                Iterator<Row> iterator = resultHandler.result().iterator();
                                List<Map> resultList = new ArrayList<>();
                                while (iterator.hasNext()) {
                                  Row row = iterator.next();
                                  HashMap result = new HashMap();
                                  for (String key : selectFields.keySet()) {
                                    Object tmpObject = row.getValue(key);
                                    if (tmpObject instanceof JsonArray) {
                                      JsonArray tmpArray = (JsonArray) tmpObject;
                                      result.put(key, tmpArray.getList());
                                    } else if (tmpObject instanceof JsonObject) {
                                      JsonObject tmpJsonObject = (JsonObject) tmpObject;
                                      result.put(key, tmpJsonObject.getMap());
                                    } else {
                                      result.put(key, row.getValue(key));
                                    }
                                  }
                                  resultList.add(result);
                                }
                                future.complete(resultList);
                              } else {
                                if (log.isErrorEnabled()) {
                                  HashMap errorMap = new HashMap();
                                  errorMap.put(GRAPHQL_OBJECT_ARGUMENT, inputObj);
                                  errorMap.put(GRAPHQL_SKIP_ARGUMENT, skip);
                                  errorMap.put(GRAPHQL_LIMIT_ARGUMENT, limit);
                                  errorMap.put(GRAPHQL_ORDERBY_ARGUMENT, orderby);
                                  log.error(
                                      "{}",
                                      LogData.getErrorLog(
                                          "E10008", errorMap, resultHandler.cause()));
                                }
                                future.completeExceptionally(new BusinessException("E10008"));
                              }
                              sqlConnection.close();
                            });
                      }
                    });
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_OBJECT_ARGUMENT, inputObj);
              errorMap.put(GRAPHQL_SKIP_ARGUMENT, skip);
              errorMap.put(GRAPHQL_LIMIT_ARGUMENT, limit);
              errorMap.put(GRAPHQL_ORDERBY_ARGUMENT, orderby);
              log.error("{}", LogData.getErrorLog("E10008", errorMap, e));
            }
            future.completeExceptionally(new BusinessException("E10008"));
          }
        });
    return future;
  }

  /**
   * 构造查询 where 条件
   *
   * @param condition
   * @param objectName
   * @param schemaData
   * @return
   */
  public static String getWhereCondition(
      HashMap condition, String objectName, SchemaData schemaData) {
    StringBuilder queryStr = new StringBuilder();
    if (null == condition) {
      return null;
    }
    Iterator keyIteraotor = condition.keySet().iterator();
    if (keyIteraotor.hasNext()) {
      String keyStr = String.class.cast(keyIteraotor.next());
      Object keyObj = condition.get(keyStr);
      if (keyStr.equals(GRAPHQL_FILTER_OR_OPERATOR)) {
        List filterObj = List.class.cast(keyObj);
        for (int i = 0; i < filterObj.size(); i++) {
          String tmpWhereClause =
              getWhereCondition(HashMap.class.cast(filterObj.get(i)), objectName, schemaData);
          if (null == tmpWhereClause) {
            continue;
          }
          if (0 != i) {
            queryStr.append(" or ");
          }
          queryStr.append("( ").append(tmpWhereClause).append(" ) ");
        }
        return queryStr.toString();
      } else if (keyStr.equals(GRAPHQL_FILTER_AND_OPERATOR)) {
        List filterObj = List.class.cast(keyObj);
        for (int i = 0; i < filterObj.size(); i++) {
          String tmpWhereClause =
              getWhereCondition(HashMap.class.cast(filterObj.get(i)), objectName, schemaData);
          if (null == tmpWhereClause) {
            continue;
          }
          if (0 != i) {
            queryStr.append(" and  ");
          }
          queryStr.append("( ").append(tmpWhereClause).append(" ) ");
        }
        return queryStr.toString();
      } else if (keyStr.equals(GRAPHQL_FILTER_NOT_OPERATOR)) {
        String tmpWhereClause =
            getWhereCondition(HashMap.class.cast(keyObj), objectName, schemaData);
        if (null == tmpWhereClause) {
          return null;
        }
        queryStr.append(" not ( ").append(tmpWhereClause).append(" ) ");
        return queryStr.toString();
      } else if (keyStr.equals(GRAPHQL_FILTER_FILTER_OPERATOR)) {
        HashMap fieldFilterObj = HashMap.class.cast(keyObj);
        Iterator fieldIterator = fieldFilterObj.keySet().iterator();
        int phaseCounter = 0;
        ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
        while (fieldIterator.hasNext()) {
          if (0 < phaseCounter) {
            queryStr.append(" and ");
          }
          phaseCounter++;
          String fieldStr = String.class.cast(fieldIterator.next());
          HashMap fieldFinalObj = HashMap.class.cast(fieldFilterObj.get(fieldStr));
          Iterator operatorIterator = fieldFinalObj.keySet().iterator();
          String fieldType = objectTypeMetaData.getFields().get(fieldStr);
          if (fieldType.equals(GRAPHQL_FROMRELATION_TYPENAME)) {
            RelationField relationField =
                objectTypeMetaData.getFromRelationFieldData().get(fieldStr);
            if (relationField.getRelationType().equals(GRAPHQL_MANY2MANY_NAME)) {
              String operator = String.class.cast(operatorIterator.next());
              if (operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                String tmpWhereClause =
                    getWhereCondition(
                        HashMap.class.cast(fieldFinalObj.get(operator)),
                        relationField.getToObject(),
                        schemaData);
                if (null != tmpWhereClause) {
                  queryStr.append("(");
                  queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
                  queryStr.append(" in ");
                  queryStr
                      .append(" ( select ")
                      .append(POSTGRES_FROM_COLUMNNAME)
                      .append(" from ")
                      .append(PostgreSqlSchema.getTableNameofRelation(relationField))
                      .append(" where ")
                      .append(POSTGRES_TO_COLUMNNAME)
                      .append(" in ");
                  queryStr
                      .append(" (select ")
                      .append(POSTGRES_COLUMNNAME_PREFIX)
                      .append(POSTGRES_ID_FIELD)
                      .append(" from ");
                  queryStr.append(
                      schemaData
                          .getObjectMetaData()
                          .get(relationField.getToObject())
                          .getTableName());
                  queryStr.append(" where ");
                  queryStr.append(tmpWhereClause);
                  queryStr.append("))");
                }
              } else {
                throw new BusinessException("E10011");
              }
            } else if (relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
              String operator = String.class.cast(operatorIterator.next());
              if (operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                String tmpWhereClause =
                    getWhereCondition(
                        HashMap.class.cast(fieldFinalObj.get(operator)),
                        relationField.getToObject(),
                        schemaData);
                if (null != tmpWhereClause) {
                  queryStr.append("(");
                  queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
                  queryStr.append("in");
                  queryStr
                      .append(" (select ")
                      .append(POSTGRES_COLUMNNAME_PREFIX)
                      .append(relationField.getToField())
                      .append(" from ");
                  queryStr.append(
                      schemaData
                          .getObjectMetaData()
                          .get(relationField.getToObject())
                          .getTableName());
                  queryStr.append(" where ");
                  queryStr.append(tmpWhereClause);
                  queryStr.append("))");
                }
              }
            } else if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)) {
              String tmpWhereClause =
                  getWhereCondition(
                      (HashMap) fieldFinalObj, relationField.getToObject(), schemaData);
              if (null != tmpWhereClause) {
                queryStr.append("(");
                queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
                queryStr.append(" in ");
                queryStr.append("(");
                queryStr
                    .append(" (select ")
                    .append(POSTGRES_COLUMNNAME_PREFIX)
                    .append(relationField.getToField())
                    .append(" from ");
                queryStr.append(
                    schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName());
                queryStr.append(" where ");
                queryStr.append(tmpWhereClause);
                queryStr.append(")");
              }
            } else {
              String tmpWhereClause =
                  getWhereCondition(
                      HashMap.class.cast(fieldFinalObj), relationField.getToObject(), schemaData);
              if (null != tmpWhereClause) {
                queryStr.append("(");
                queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                queryStr.append(" in ");
                queryStr.append("(");
                queryStr
                    .append(" (select ")
                    .append(POSTGRES_COLUMNNAME_PREFIX)
                    .append(POSTGRES_ID_FIELD)
                    .append(" from ");
                queryStr.append(
                    schemaData.getObjectMetaData().get(relationField.getToObject()).getTableName());
                queryStr.append(" where ");
                queryStr.append(tmpWhereClause);
                queryStr.append(")");
              }
            }
          } else if (fieldType.equals(GRAPHQL_TORELATION_TYPENAME)) {
            RelationField relationField =
                objectTypeMetaData.getFromRelationFieldData().get(fieldStr);
            if (relationField.getRelationType().equals(GRAPHQL_MANY2MANY_NAME)) {
              String operator = String.class.cast(operatorIterator.next());
              if (operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                String tmpWhereClause =
                    getWhereCondition(
                        HashMap.class.cast(fieldFinalObj.get(operator)),
                        relationField.getFromObject(),
                        schemaData);
                if (null != tmpWhereClause) {
                  queryStr.append("(");
                  queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
                  queryStr.append(" in ");
                  queryStr
                      .append(" ( select ")
                      .append(POSTGRES_TO_COLUMNNAME)
                      .append(" from ")
                      .append(PostgreSqlSchema.getTableNameofRelation(relationField))
                      .append(" where ")
                      .append(POSTGRES_FROM_COLUMNNAME)
                      .append(" in ");
                  queryStr
                      .append(" (select ")
                      .append(POSTGRES_COLUMNNAME_PREFIX)
                      .append(POSTGRES_ID_FIELD)
                      .append(" from ");
                  queryStr.append(
                      schemaData
                          .getObjectMetaData()
                          .get(relationField.getFromObject())
                          .getTableName());
                  queryStr.append(" where ");
                  queryStr.append(tmpWhereClause);
                  queryStr.append("))");
                }
              } else {
                throw new BusinessException("E10011");
              }
            } else if (relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)
                || relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)) {
              String tmpWhereClause =
                  getWhereCondition(
                      HashMap.class.cast(fieldFinalObj), relationField.getFromObject(), schemaData);
              if (null != tmpWhereClause) {
                queryStr.append("(");
                queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                queryStr.append(" in ");
                queryStr.append("(");
                queryStr
                    .append(" (select ")
                    .append(POSTGRES_COLUMNNAME_PREFIX)
                    .append(POSTGRES_ID_FIELD)
                    .append(" from ");
                queryStr.append(
                    schemaData
                        .getObjectMetaData()
                        .get(relationField.getFromObject())
                        .getTableName());
                queryStr.append(" where ");
                queryStr.append(tmpWhereClause);
                queryStr.append(")");
              }
            } else {
              String tmpWhereClause =
                  getWhereCondition(
                      HashMap.class.cast(fieldFinalObj), relationField.getFromObject(), schemaData);
              if (null != tmpWhereClause) {
                queryStr
                    .append("(")
                    .append(POSTGRES_COLUMNNAME_PREFIX)
                    .append(POSTGRES_ID_FIELD)
                    .append(" in ( ");
                queryStr
                    .append(" (select ")
                    .append(POSTGRES_COLUMNNAME_PREFIX)
                    .append(relationField.getFromField())
                    .append(" from ");
                queryStr.append(
                    schemaData
                        .getObjectMetaData()
                        .get(relationField.getFromObject())
                        .getTableName());
                queryStr.append(" where ");
                queryStr.append(tmpWhereClause);
                queryStr.append(")");
              }
            }
          } else if (fieldType.equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
            EnumField enumField = objectTypeMetaData.getEnumFieldData().get(fieldStr);
            String fieldTypeName = enumField.getType();
            EnumTypeMetaData enumTypeMetaData = schemaData.getEnuminfo().get(fieldTypeName);
            String operator = String.class.cast(operatorIterator.next());
            if (enumField.isList()) {
              if (operator.equals(GRAPHQL_FILTER_CONTAIN_OPERATOR)) {
                queryStr.append(" @> ");
              } else if (operator.equals(GRAPHQL_FILTER_IN_OPERATOR)) {
                queryStr.append(" <@ ");
              }
              queryStr.append(" in (");
              List l = List.class.cast(fieldFinalObj.get(operator));
              for (int loc = 0; loc < l.size(); loc++) {
                if (loc != 0) {
                  queryStr.append(",");
                }
                queryStr.append("'").append(String.class.cast(l.get(loc))).append("'");
              }
              queryStr.append("))");
            } else {
              if (operator.equals(GRAPHQL_FILTER_EQ_OPERATOR)) {
                queryStr.append(" ").append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                queryStr.append(" = '").append(fieldFinalObj.get(operator)).append("'");
              } else if (operator.equals(GRAPHQL_FILTER_NE_OPERATOR)) {
                queryStr.append(" ").append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                queryStr.append(" != '").append(fieldFinalObj.get(operator)).append("'");
              } else if (operator.equals(GRAPHQL_FILTER_IN_OPERATOR)) {
                queryStr.append(" ").append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                List l = List.class.cast(fieldFinalObj.get(operator));
                queryStr.append(" in (");
                for (int loc = 0; loc < l.size(); loc++) {
                  if (loc != 0) {
                    queryStr.append(",");
                  }
                  queryStr.append("'").append(String.class.cast(l.get(loc))).append("'");
                }
                queryStr.append("))");
              }
            }

          } else {
            String fieldTypeName = objectTypeMetaData.getScalarFieldData().get(fieldStr).getType();
            ScalarFieldInfo scalarFieldInfo = objectTypeMetaData.getScalarFieldData().get(fieldStr);
            while (operatorIterator.hasNext()) {
              String operator = String.class.cast(operatorIterator.next());
              final boolean b =
                  fieldTypeName.equals(GRAPHQL_LONG_TYPENAME)
                      || fieldTypeName.equals(GRAPHQL_INT_TYPENAME)
                      || fieldTypeName.equals(GRAPHQL_BIGDECIMAL_TYPENAME)
                      || fieldTypeName.equals(GRAPHQL_SHORT_TYPENAME)
                      || fieldTypeName.equals(GRAPHQL_FLOAT_TYPENAME);
              if (scalarFieldInfo.isList()) {
                if (!operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                  if (b) {
                    queryStr.append(" ( ");
                    queryStr.append(fieldFinalObj.get(operator));
                    queryStr.append(" = any(");
                  } else {
                    queryStr.append(" ( \"");
                    queryStr.append(fieldFinalObj.get(operator));
                    queryStr.append("\" = any(");
                  }
                  queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                  queryStr.append(")");
                } else {
                  throw new BusinessException("E10011");
                }
              } else {
                queryStr.append(" ( ");
                queryStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                if (b) {
                  switch (operator) {
                    case GRAPHQL_FILTER_EQ_OPERATOR:
                      queryStr.append(" = ");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append(")");
                      break;
                    case GRAPHQL_FILTER_GE_OPERATOR:
                      queryStr.append(" >= ");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append(")");
                      break;
                    case GRAPHQL_FILTER_GT_OPERATOR:
                      queryStr.append(" > ");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append(")");
                      break;
                    case GRAPHQL_FILTER_LE_OPERATOR:
                      queryStr.append(" <= ");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append(")");
                      break;
                    case GRAPHQL_FILTER_LT_OPERATOR:
                      queryStr.append(" < ");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append(")");
                      break;
                    case GRAPHQL_FILTER_NE_OPERATOR:
                      queryStr.append(" != ");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append(")");
                      break;
                    case GRAPHQL_FILTER_IN_OPERATOR:
                      List l = List.class.cast(fieldFinalObj.get(operator));
                      queryStr.append(" in (");
                      for (int loc = 0; loc < l.size(); loc++) {
                        if (loc != 0) {
                          queryStr.append(",");
                        }
                        queryStr.append(String.class.cast(l.get(loc)));
                      }
                      queryStr.append("))");
                      break;
                    default:
                      throw new BusinessException("E10011");
                  }
                } else if (fieldTypeName.equals(GRAPHQL_STRING_TYPENAME)
                    || fieldTypeName.equals(GRAPHQL_ID_TYPENAME)) {
                  switch (operator) {
                    case GRAPHQL_FILTER_EQ_OPERATOR:
                      queryStr.append(" = '");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_GE_OPERATOR:
                      queryStr.append(" >= '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_GT_OPERATOR:
                      queryStr.append(" > '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_LE_OPERATOR:
                      queryStr.append(" <= '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_LT_OPERATOR:
                      queryStr.append(" < '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_NE_OPERATOR:
                      queryStr.append(" != '");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append("') ");
                      break;
                    case GRAPHQL_FILTER_IN_OPERATOR:
                      List l = List.class.cast(fieldFinalObj.get(operator));
                      queryStr.append(" in (");
                      for (int loc = 0; loc < l.size(); loc++) {
                        if (loc != 0) {
                          queryStr.append(",");
                        }
                        queryStr.append(" '");
                        queryStr.append(String.class.cast(l.get(loc)));
                        queryStr.append("' ");
                      }
                      queryStr.append("))");
                      break;
                    case GRAPHQL_FILTER_MATCH_OPERATOR:
                      queryStr.append(" ~ '");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append("' )");
                      break;
                    default:
                      throw new BusinessException("E10011");
                  }
                } else if (fieldTypeName.equals(GRAPHQL_BOOLEAN_TYPENAME)) {
                  switch (operator) {
                    case GRAPHQL_FILTER_ISTRUE_OPERATOR:
                      queryStr.append(" is ");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append(")");
                      break;
                    default:
                      throw new BusinessException("E10011");
                  }
                } else if (fieldTypeName.equals(GRAPHQL_DATETIME_TYPENAME)
                    || fieldTypeName.equals(GRAPHQL_TIME_TYPENAME)
                    || fieldTypeName.equals(GRAPHQL_DATE_TYPENAME)
                    || fieldTypeName.equals(GRAPHQL_CREATEDAT_TYPENAME)
                    || fieldTypeName.equals(GRAPHQL_LASTUPDATE_TYPENAME)) {
                  switch (operator) {
                    case GRAPHQL_FILTER_EQ_OPERATOR:
                      queryStr.append(" = '");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append("' )");
                      break;
                    case GRAPHQL_FILTER_GE_OPERATOR:
                      queryStr.append(" >= '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_GT_OPERATOR:
                      queryStr.append(" > '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_LE_OPERATOR:
                      queryStr.append(" <= '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_LT_OPERATOR:
                      queryStr.append(" < '");
                      queryStr.append(fieldFilterObj.get(operator));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_NE_OPERATOR:
                      queryStr.append(" != '");
                      queryStr.append(String.class.cast(fieldFinalObj.get(operator)));
                      queryStr.append("')");
                      break;
                    case GRAPHQL_FILTER_IN_OPERATOR:
                      List ll = List.class.cast(fieldFinalObj.get(operator));
                      queryStr.append(" in (");
                      for (int llloc = 0; llloc < ll.size(); llloc++) {
                        if (llloc != 0) {
                          queryStr.append(",");
                        }
                        queryStr.append("'");
                        queryStr.append(String.class.cast(ll.get(llloc)));
                        queryStr.append("'");
                      }
                      queryStr.append("))");
                      break;
                    default:
                      throw new BusinessException("E10011");
                  }

                } else {
                  throw new BusinessException("E10011");
                }
              }
            }
          }
        }
        return queryStr.toString();
      } else {
        throw new BusinessException("E10011");
      }
    } else {
      return null;
    }
  }

  /**
   * 根据 结果集要求，返回对应select 字段
   *
   * @param selectFields
   * @param schemaData
   * @param objectName
   * @param tableNameAlias
   * @return
   */
  public static String getSelectionField(
      HashMap<String, Object> selectFields,
      SchemaData schemaData,
      String objectName,
      String tableNameAlias) {
    ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
    String tableName = objectTypeMetaData.getTableName();
    StringBuilder selectionStr = new StringBuilder();
    selectionStr.append(" select ");
    Iterator fieldNameIterator = selectFields.keySet().iterator();
    int phaseCounter = 0;
    while (fieldNameIterator.hasNext()) {
      if (0 < phaseCounter) {
        selectionStr.append(",");
      }
      String fieldName = String.class.cast(fieldNameIterator.next());
      if (null == objectTypeMetaData
          || null == objectTypeMetaData.getFields()
          || null == objectTypeMetaData.getFields().get(fieldName)) {
        throw new BusinessException("E10091");
      }
      if (objectTypeMetaData.getFields().get(fieldName).equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
        selectionStr.append("(");
        selectionStr.append(tableNameAlias);
        selectionStr.append(".");
        selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldName);
        selectionStr.append(") as \"").append(fieldName).append("\" ");

      } else if (objectTypeMetaData
          .getFields()
          .get(fieldName)
          .equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
        ScalarFieldInfo scalarFieldInfo = objectTypeMetaData.getScalarFieldData().get(fieldName);
        selectionStr.append("(");
        selectionStr.append(tableNameAlias);
        selectionStr.append(".");
        selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldName);
        selectionStr.append(") as \"");
        selectionStr.append(fieldName).append("\" ");
      } else if (objectTypeMetaData
          .getFields()
          .get(fieldName)
          .equals(GRAPHQL_FROMRELATION_TYPENAME)) {
        RelationField relationField = objectTypeMetaData.getFromRelationFieldData().get(fieldName);
        HashMap subFields = HashMap.class.cast(selectFields.get(fieldName));
        String subTypeName = relationField.getToObject();
        String subTableNameAlias = tableNameAlias + "_" + fieldName + subTypeName;
        if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
            || relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
          selectionStr.append("(select row_to_json(");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") from (");
          selectionStr.append(
              getSelectionField(subFields, schemaData, subTypeName, subTableNameAlias));
          selectionStr.append(" where  ");
          selectionStr.append(subTableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(relationField.getToField());
          selectionStr.append("=");
          selectionStr.append(tableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
          selectionStr.append(") as ");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") as \"");
          selectionStr.append(fieldName).append("\" ");
        } else if (relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
          selectionStr.append("(select array_to_json(");
          selectionStr.append("array_agg(");
          selectionStr.append("row_to_json(");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append("))) from (");
          selectionStr.append(
              getSelectionField(subFields, schemaData, subTypeName, subTableNameAlias));
          selectionStr.append(" where  ");
          selectionStr.append(subTableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(relationField.getToField());
          selectionStr.append("=");
          selectionStr.append(tableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
          selectionStr.append(") as ");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") as \"");
          selectionStr.append(fieldName).append("\" ");
        } else {
          selectionStr.append("(select array_to_json(");
          selectionStr.append("array_agg(");
          selectionStr.append("row_to_json(");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append("))) from (");
          selectionStr.append(
              getSelectionField(subFields, schemaData, subTypeName, subTableNameAlias));
          selectionStr.append(" where  ");
          selectionStr.append(subTableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
          selectionStr.append(" in (");
          selectionStr
              .append(" select ")
              .append(POSTGRES_TO_COLUMNNAME)
              .append(" from ")
              .append(PostgreSqlSchema.getTableNameofRelation(relationField))
              .append(" where ")
              .append(POSTGRES_FROM_COLUMNNAME)
              .append(" = ");
          selectionStr.append(tableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldName);
          selectionStr.append(")) as ");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") as \"");
          selectionStr.append(fieldName).append("\" ");
        }
      } else {
        RelationField relationField = objectTypeMetaData.getToRelationFieldData().get(fieldName);
        HashMap subFields = HashMap.class.cast(selectFields.get(fieldName));
        String subTypeName = relationField.getFromObject();
        String subTableNameAlias = tableNameAlias + "_" + fieldName + subTypeName;
        if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)) {
          selectionStr.append("(select row_to_json(");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") from (");
          selectionStr.append(
              getSelectionField(subFields, schemaData, subTypeName, subTableNameAlias));
          selectionStr.append(" where  ");
          selectionStr.append(subTableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(relationField.getFromField());
          selectionStr.append("=");
          selectionStr.append(tableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
          selectionStr.append(") as ");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") as \"");
          selectionStr.append(fieldName).append("\" ");
        } else if (relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
          selectionStr.append("(select row_to_json(");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") from (");
          selectionStr.append(
              getSelectionField(subFields, schemaData, subTypeName, subTableNameAlias));
          selectionStr.append(" where  ");
          selectionStr.append(subTableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
          selectionStr.append("=");
          selectionStr.append(tableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldName);
          selectionStr.append(") as ");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") as \"");
          selectionStr.append(fieldName).append("\" ");
        } else if (relationField.getRelationType().equals(GRAPHQL_MANY2MANY_NAME)) {
          selectionStr.append("(select array_to_json(");
          selectionStr.append("array_agg(");
          selectionStr.append("row_to_json(");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append("))) from (");
          selectionStr.append(
              getSelectionField(subFields, schemaData, subTypeName, subTableNameAlias));
          selectionStr.append(" where  ");
          selectionStr.append(subTableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
          selectionStr.append(" in (");
          selectionStr
              .append(" select ")
              .append(POSTGRES_FROM_COLUMNNAME)
              .append(" from ")
              .append(PostgreSqlSchema.getTableNameofRelation(relationField))
              .append(" where ")
              .append(POSTGRES_TO_COLUMNNAME)
              .append(" = ");
          selectionStr.append(tableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldName);
          selectionStr.append(") as ");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") as \"");
          selectionStr.append(fieldName).append("\" ");
        } else {
          selectionStr.append("(select array_to_json(");
          selectionStr.append("array_agg(");
          selectionStr.append("row_to_json(");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append("))) from (");
          selectionStr.append(
              getSelectionField(subFields, schemaData, subTypeName, subTableNameAlias));
          selectionStr.append(" where  ");
          selectionStr.append(subTableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(relationField.getFromField());
          selectionStr.append("=");
          selectionStr.append(tableNameAlias);
          selectionStr.append(".");
          selectionStr.append(POSTGRES_COLUMNNAME_PREFIX).append(POSTGRES_ID_FIELD);
          selectionStr.append(") as ");
          selectionStr.append(tableNameAlias);
          selectionStr.append("_");
          selectionStr.append(fieldName);
          selectionStr.append(") as \"");
          selectionStr.append(fieldName).append("\" ");
        }
      }
      phaseCounter++;
    }
    selectionStr.append(" from ");
    selectionStr.append(tableName);
    selectionStr.append(" as ");
    selectionStr.append(tableNameAlias);
    selectionStr.append(" ");
    return selectionStr.toString();
  }
}
