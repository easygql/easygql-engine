package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import com.easygql.component.ConfigurationProperties;
import com.easygql.exception.BusinessException;
import com.easygql.exception.NotAuthorizedException;
import graphql.schema.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Slf4j
public class AuthorityUtil {
  /**
   * 填充过滤条件
   *
   * @param objectName
   * @param loginedUser
   * @param rowConstraint
   * @param schemaData
   * @param roleName
   * @return
   */
  public static HashMap transferRowFilter(
      String objectName,
      HashMap loginedUser,
      HashMap rowConstraint,
      SchemaData schemaData,
      String roleName) {
    HashMap finalMap = new HashMap();
    if (null == rowConstraint) {
      rowConstraint = new HashMap();
    }
    Iterator keyIterator = rowConstraint.keySet().iterator();
    ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
    while (keyIterator.hasNext()) {
      String keyInfo = String.valueOf(keyIterator.next());
      if (GRAPHQL_FILTER_AND_OPERATOR.equals(keyInfo)
          || GRAPHQL_FILTER_OR_OPERATOR.equals(keyInfo)) {
        List<Object> valueList = List.class.cast(rowConstraint.get(keyInfo));
        List<Object> finalList = new ArrayList<>();
        for (Object obj : valueList) {
          finalList.add(
              transferRowFilter(objectName, loginedUser, (HashMap) obj, schemaData, roleName));
        }
        finalMap.put(keyInfo, finalList);
      } else if (GRAPHQL_FILTER_NOT_OPERATOR.equals(keyInfo)) {
        finalMap.put(
            keyInfo,
            transferRowFilter(
                objectName,
                loginedUser,
                (HashMap) rowConstraint.get(keyInfo),
                schemaData,
                roleName));
      } else if (GRAPHQL_FILTER_FIELDCONTAIN_OPERATOR.equals(keyInfo)) {
        List<Object> fieldList = List.class.cast(rowConstraint.get(keyInfo));
        List<Object> fieldValueList = new ArrayList<>();
        fieldList.forEach(it -> getLeafValue(it, loginedUser));
        finalMap.put(keyInfo, fieldValueList);
      } else if (GRAPHQL_FILTER_FILTER_OPERATOR.equals(keyInfo)) {
        HashMap filterObj = HashMap.class.cast(rowConstraint.get(keyInfo));
        HashMap finalFilterObj = new HashMap();
        Iterator iteratorFilter = filterObj.keySet().iterator();
        HashMap finalFieldMap = new HashMap();
        while (iteratorFilter.hasNext()) {
          String fieldName = String.class.cast(iteratorFilter.next());
          String fieldType = objectTypeMetaData.getFields().get(fieldName);
          Map<String, Object> filterEntryMap = (Map<String, Object>) (filterObj.get(fieldName));
          if (filterEntryMap.size() > 1) {
            throw new BusinessException("E10089");
          }
          Iterator<String> iterator = filterEntryMap.keySet().iterator();
          if (iterator.hasNext()) {
            String operationkey = iterator.next();
            HashMap filterTuple = new HashMap();
            if (fieldType.equals(GRAPHQL_FROMRELATION_TYPENAME)) {
              RelationField relationField =
                  objectTypeMetaData.getFromRelationFieldData().get(fieldName);
              filterTuple.put(
                  operationkey,
                  transferRowFilter(
                      relationField.getToObject(),
                      loginedUser,
                      (HashMap) filterEntryMap.get(operationkey),
                      schemaData,
                      roleName));
            } else if (fieldType.equals(GRAPHQL_TORELATION_TYPENAME)) {
              RelationField relationField =
                  objectTypeMetaData.getToRelationFieldData().get(fieldName);
              filterTuple.put(
                  operationkey,
                  transferRowFilter(
                      relationField.getFromObject(),
                      loginedUser,
                      (HashMap) filterEntryMap.get(operationkey),
                      schemaData,
                      roleName));
            } else {
              filterTuple.put(
                  operationkey, getLeafValue(filterEntryMap.get(operationkey), loginedUser));
            }
            finalFieldMap.put(fieldName, filterTuple);
          }
        }
        finalFilterObj.put(keyInfo, finalFieldMap);
      } else {
        throw new BusinessException("E10011");
      }
    }
    return finalMap;
  }

  /**
   * 将用户属性放到过滤条件中去
   *
   * @param obj
   * @param loginedUser
   * @return
   */
  public static Object getLeafValue(@NonNull Object obj, @NonNull HashMap loginedUser) {
    RowConstraint rowConstraint = JSONObject.parseObject(String.valueOf(obj), RowConstraint.class);
    if (GRAPHQL_ROWCONSTRAINT_CONST.equals(rowConstraint.valuetype)) {
      return rowConstraint.value;
    } else {
      return loginedUser.get(String.valueOf(rowConstraint.value));
    }
  }

  /**
   * 对设置的行过滤器合法性进行检查
   *
   * @param rowConstraint
   * @param whereInputType
   * @param userType
   * @return
   */
  public static FilterResult CheckRowFilter(
      HashMap rowConstraint, GraphQLInputObjectType whereInputType, GraphQLObjectType userType) {
    Iterator keyIterator = rowConstraint.keySet().iterator();
    try {
      while (keyIterator.hasNext()) {
        String keyInfo = String.valueOf(keyIterator.next());
        if (null != whereInputType.getField(keyInfo)) {
          FilterResult filterResult = new FilterResult();
          filterResult.setHints(whereInputType.getName() + " 没有" + keyInfo + "字段");
          filterResult.setValid(false);
          return filterResult;
        } else {
          GraphQLInputObjectField inputField = whereInputType.getField(keyInfo);
          if (inputField.getType() instanceof GraphQLScalarType) {
            RowConstraint rowConstrainttmp =
                JSONObject.parseObject(
                    String.valueOf(rowConstraint.get(keyInfo)), RowConstraint.class);
            if (GRAPHQL_ROWCONSTRAINT_CONST.equals(rowConstrainttmp.valuetype)) {
              GraphQLElementGenerator.getScalarType(inputField.getType().getName())
                  .getCoercing()
                  .parseLiteral(rowConstrainttmp.value);
            } else {
              if (!inputField
                  .getType()
                  .equals(
                      userType
                          .getFieldDefinition(String.valueOf(rowConstrainttmp.value))
                          .getType())) {
                FilterResult filterResult = new FilterResult();
                filterResult.setHints("User中" + rowConstrainttmp.value + "字段类型和过滤字段不匹配");
                filterResult.setValid(false);
                return filterResult;
              }
            }
          } else {
            HashMap childMap = HashMap.class.cast(rowConstraint.get(keyInfo));
            FilterResult filterResult =
                CheckRowFilter(childMap, (GraphQLInputObjectType) inputField.getType(), userType);
            if (!filterResult.isValid()) {
              return filterResult;
            }
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_API_CONSTRAINT_FIELDNAME, rowConstraint);
        log.error("{}", LogData.getErrorLog("E10090", errorMap, e));
      }
      FilterResult filterResult = new FilterResult();
      filterResult.setHints(e.getMessage());
      filterResult.setValid(false);
      return filterResult;
    }
    FilterResult filterResult = new FilterResult();
    filterResult.setValid(true);
    return filterResult;
  }

  /**
   * @param environment
   * @return
   */
  public static HashMap getLoginUser(DataFetchingEnvironment environment) {
    HashMap contextHashMap = HashMap.class.cast(environment.getContext());
    HashMap userInfo = HashMap.class.cast(contextHashMap.get(GRAPHQL_LOGINEDUSER_OBJECTNAME));
    return userInfo;
  }

  /**
   * @param environment
   * @param disabledRoles
   */
  public static void authorityVerify(
      DataFetchingEnvironment environment, HashSet<String> disabledRoles) {
    if (null != disabledRoles) {
      HashMap userInfo = getLoginUser(environment);
      if (null == userInfo.get(GRAPHQL_ROLE_FIELDNAME)) {
        throw NotAuthorizedException.notAuthorizedException;
      }
      if (disabledRoles.contains(userInfo.get(GRAPHQL_ROLE_FIELDNAME))) {
        throw NotAuthorizedException.notAuthorizedException;
      }
    }
  }

  /**
   * 在进行查询前根据用户信息过滤
   *
   * @param userInfo
   * @param condition
   */
  public static CompletableFuture<Object> queryPermissionFilterBefore(
      HashMap userInfo, HashMap condition, String objectName, SchemaData schemaData) {
    return CompletableFuture.supplyAsync(
        () -> {
          String roleInfo =
              String.class.cast(
                  userInfo.get(ConfigurationProperties.getInstance().ROLE_IN_USER_FIELDNAME));
          List<String> disabledRoles =
              schemaData.getObjectMetaData().get(objectName).getUnreadableRoles();
          HashMap readConstraint =
              schemaData.getObjectMetaData().get(objectName).getReadConstraints();
          if (null != disabledRoles && disabledRoles.contains(roleInfo)) {
            throw new NotAuthorizedException();
          }
          HashMap rowConstraints = null;
          if (null != readConstraint) {
            rowConstraints = (HashMap) readConstraint.get(roleInfo);
          }
          HashMap condition1 = condition;
          if (null != rowConstraints) {
            HashMap preCondition =
                transferRowFilter(objectName, userInfo, rowConstraints, schemaData, roleInfo);
            condition1 = mergeCondition(preCondition, condition);
          }
          return condition1;
        });
  }

  /**
   * 在进行插入前根据用户信息进行过滤
   *
   * @param disabledRoles
   * @param userInfo
   * @param forbiddenFields
   * @param insertObj
   */
  public static CompletableFuture<Object> insertPermissionFilterBefore(
      @NonNull HashSet<String> disabledRoles,
      @NonNull HashMap userInfo,
      @NonNull HashMap forbiddenFields,
      @NonNull final Object insertObj) {
    return CompletableFuture.supplyAsync(
        () -> {
          String roleInfo =
              String.class.cast(
                  userInfo.get(ConfigurationProperties.getInstance().ROLE_IN_USER_FIELDNAME));
          if (disabledRoles.contains(roleInfo)) {
            throw new NotAuthorizedException();
          }
          if (forbiddenFields.containsKey(roleInfo)) {
            HashSet fieldsInfo = (HashSet) forbiddenFields.get(roleInfo);
            if (insertObj instanceof List) {
              List objList = (List) insertObj;
              return objList.stream()
                  .map(obj -> removeForbiddenFields((HashMap) obj, fieldsInfo))
                  .collect(Collectors.toList());
            } else {
              return removeForbiddenFields((HashMap) insertObj, fieldsInfo);
            }
          } else {
            return insertObj;
          }
        });
  }

  public static CompletableFuture<Boolean> relationPermissionFilter(
      @NonNull HashSet<String> relationDisabledRoles, @NonNull HashMap userInfo) {
    return CompletableFuture.supplyAsync(
        () -> {
          String roleInfo =
              String.class.cast(
                  userInfo.get(ConfigurationProperties.getInstance().ROLE_IN_USER_FIELDNAME));
          if (relationDisabledRoles.contains(roleInfo)) {
            throw new NotAuthorizedException();
          }
          return true;
        });
  }

  /**
   * 在进行数据更新前过滤
   *
   * @param disabledRoles
   * @param userInfo
   * @param updateForbiddenFields
   * @param rowConstraints
   * @param condition
   * @param updateObj
   */
  public static CompletableFuture<Object> updatePermissionFilterBefore(
      HashSet<String> disabledRoles,
      @NonNull HashMap userInfo,
      HashMap<String, HashSet> updateForbiddenFields,
      HashMap<String, HashMap> rowConstraints,
      HashMap condition,
      HashMap updateObj,
      SchemaData schemaData,
      String objectName) {
    return CompletableFuture.supplyAsync(
        () -> {
          String roleInfo =
              String.class.cast(
                  userInfo.get(ConfigurationProperties.getInstance().ROLE_IN_USER_FIELDNAME));
          if (null != disabledRoles && disabledRoles.contains(roleInfo)) {
            throw new NotAuthorizedException();
          }
          if ((null != updateForbiddenFields)
              && (updateForbiddenFields.size() > 0)
              && (null != updateForbiddenFields.get(roleInfo))) {
            HashSet<String> fieldset = updateForbiddenFields.get(roleInfo);
            HashSet<String> forbiddenSet = new HashSet<>();
            forbiddenSet.addAll(fieldset);
            forbiddenSet.retainAll(updateObj.keySet());
            if (forbiddenSet.size()>0) {
              throw new NotAuthorizedException();
            }
          }
          HashMap preCondition =
              transferRowFilter(
                  objectName, userInfo, rowConstraints.get(roleInfo), schemaData, roleInfo);
          HashMap condition1 = mergeCondition(preCondition, condition);
          return condition1;
        });
  }

  /**
   * 在进行数据删除前，进行过滤
   *
   * @param disabledRoles
   * @param loginedUser
   * @param rowConstraints
   * @param condition
   */
  public static CompletableFuture<Object> deletePermissionFilterBefore(
      HashSet<String> disabledRoles,
      HashMap loginedUser,
      HashMap<String, HashMap> rowConstraints,
      HashMap condition,
      SchemaData schemaData,
      String objectName) {
    return CompletableFuture.supplyAsync(
        () -> {
          String roleInfo =
              String.class.cast(
                  loginedUser.get(ConfigurationProperties.getInstance().ROLE_IN_USER_FIELDNAME));
          if (null != loginedUser && null != disabledRoles && disabledRoles.contains(roleInfo)) {
            throw new NotAuthorizedException();
          }
          HashMap preCondition =
              transferRowFilter(
                  objectName, loginedUser, rowConstraints.get(roleInfo), schemaData, roleInfo);
          HashMap condition1 = mergeCondition(preCondition, condition);
          return condition1;
        });
  }

  /**
   * 合并过滤条件
   *
   * @param preCondition
   * @param condition
   * @return
   */
  public static HashMap mergeCondition(HashMap preCondition, HashMap condition) {
    if (null == preCondition || preCondition.size() == 0) {
      return condition;
    }
    if (null == condition || condition.size() == 0) {
      return preCondition;
    }
    List conditionList = new ArrayList();
    conditionList.add(preCondition);
    conditionList.add(condition);
    HashMap finalCondition = new HashMap();
    finalCondition.put(GRAPHQL_FILTER_AND_OPERATOR, conditionList);
    return finalCondition;
  }

  /**
   * 删除数据对象中被禁止包含的字段
   *
   * @param objMap
   * @param forbiddenFields
   * @return
   */
  public static HashMap removeForbiddenFields(HashMap objMap, HashSet<String> forbiddenFields) {
    objMap.keySet().removeAll(forbiddenFields);
    return objMap;
  }
}
