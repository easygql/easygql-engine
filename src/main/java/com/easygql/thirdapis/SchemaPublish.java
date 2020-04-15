package com.easygql.thirdapis;

import com.alibaba.fastjson.JSONObject;
import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.*;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_OPERATION_RESULT_NAME;

/**
 * @author guofen
 * @date 2019/12/18 21:22
 */
@EasyGQLThirdAPI("SchemaPublish")
@Slf4j
public class SchemaPublish extends ThirdAPI {
  private SchemaStart schemaStart = new SchemaStart();
  private static HashMap schemaSelecter = new HashMap();
  private static HashMap trueResult = new HashMap();
  private static HashMap falseResult = new HashMap();

  static {
    schemaSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
    schemaSelecter.put(GRAPHQL_NAME_FIELDNAME, 1);
    schemaSelecter.put(GRAPHQL_DATABASEKIND_FIELDNAME, 1);
    HashMap datasourceSelecter = new HashMap();
    schemaSelecter.put(GRAPHQL_DATASOURCE_FIELDNAME, datasourceSelecter);
    datasourceSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_NAME_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_HOST_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_PORT_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_DATABASENAME_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_CHARACTER_ENCODING_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_MAX_CONNECTION_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_MIN_CONNECTION_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_MAX_IDLE_CONNECTION_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_MIN_IDLE_CONNECTION_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_USERNAME_FIELD, 1);
    datasourceSelecter.put(GRAPHQL_PASSWORD_FIELD, 1);
    datasourceSelecter.put(GRAPHQL_CONNECTION_STR_FIELDNAME, 1);
    datasourceSelecter.put(GRAPHQL_REPLICA_NAME_FIELDNAME, 1);
    HashMap thirdAPISelecter = new HashMap();
    schemaSelecter.put(GRAPHQL_THIRD_API_FIELDNAME, thirdAPISelecter);
    thirdAPISelecter.put(GRAPHQL_ID_FIELDNAME, 1);
    thirdAPISelecter.put(GRAPHQL_APINAME_FIELDNAME, 1);
    thirdAPISelecter.put(GRAPHQL_DISABLED_ROLES_FIELDNAME, 1);
    HashMap objectTypeSelecter = new HashMap();
    schemaSelecter.put(GRAPHQL_OBJECTTYPES_FIELDNAME, objectTypeSelecter);
    objectTypeSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
    objectTypeSelecter.put(GRAPHQL_NAME_FIELDNAME, 1);
    objectTypeSelecter.put(GRAPHQL_DESCRIPTION_FIELDNAME, 1);
    objectTypeSelecter.put("read_constraints", 1);
    objectTypeSelecter.put("update_constraints", 1);
    objectTypeSelecter.put("delete_constraints", 1);
    objectTypeSelecter.put("unreadable_roles", 1);
    objectTypeSelecter.put("uninsertable_roles", 1);
    objectTypeSelecter.put("undeletable_roles", 1);
    objectTypeSelecter.put("unupdatable_roles", 1);
    HashMap relatoinFields = new HashMap();
    relatoinFields.put(GRAPHQL_ID_FIELDNAME, 1);
    relatoinFields.put(GRAPHQL_FROMOBJECT_FIELDNAME, 1);
    relatoinFields.put(GRAPHQL_FROMFIELD_FIELDNAME, 1);
    relatoinFields.put(GRAPHQL_TOOBJECT_FIELDNAME, 1);
    relatoinFields.put(GRAPHQL_TOFIELD_FIELDNAME, 1);
    relatoinFields.put("relationtype", 1);
    relatoinFields.put("ifcascade", 1);
    relatoinFields.put("invisible_roles", 1);
    relatoinFields.put("irrevisible_roles", 1);
    schemaSelecter.put(GRAPHQL_RELATION_FIELDNAME, relatoinFields);
    HashMap scalarFields = new HashMap();
    objectTypeSelecter.put(GRAPHQL_SCALARFIELD_FIELDNAME, scalarFields);
    scalarFields.put(GRAPHQL_ID_FIELDNAME, 1);
    scalarFields.put(GRAPHQL_NAME_FIELDNAME, 1);
    scalarFields.put(GRAPHQL_NOTNULL_FIELDNAME, 1);
    scalarFields.put(GRAPHQL_ISLIST_FIELDNAME, 1);
    scalarFields.put(GRAPHQL_DESCRIPTION_FIELDNAME, 1);
    scalarFields.put(GRAPHQL_DEFAULTVALUE_FIELDNAME, 1);
    scalarFields.put(GRAPHQL_TYPE_FIELDNAME, 1);
    scalarFields.put("invisible_roles", 1);
    scalarFields.put("irrevisible_roles", 1);
    HashMap enumFields = new HashMap();
    objectTypeSelecter.put(GRAPHQL_ENUMFIELD_FIELDNAME, enumFields);
    enumFields.put(GRAPHQL_ID_FIELDNAME, 1);
    enumFields.put(GRAPHQL_NAME_FIELDNAME, 1);
    enumFields.put(GRAPHQL_ISLIST_FIELDNAME, 1);
    enumFields.put(GRAPHQL_DEFAULTVALUE_FIELDNAME, 1);
    enumFields.put(GRAPHQL_TYPE_FIELDNAME, 1);
    enumFields.put("invisible_roles", 1);
    enumFields.put("irrevisible_roles", 1);
    HashMap uniqueConstraintHashMap = new HashMap();
    uniqueConstraintHashMap.put(GRAPHQL_ID_FIELDNAME, 1);
    uniqueConstraintHashMap.put(GRAPHQL_FIELDS_FIELDNAME, 1);
    objectTypeSelecter.put(GRAPHQL_UNIQUE_CONSTRAINT_FIELDNAME, uniqueConstraintHashMap);
    HashMap enumTypesSelecter = new HashMap();
    schemaSelecter.put(GRAPHQL_ENUMTYPES_FIELDNAME, enumTypesSelecter);
    enumTypesSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
    enumTypesSelecter.put(GRAPHQL_NAME_FIELDNAME, 1);
    enumTypesSelecter.put(GRAPHQL_DESCRIPTION_FIELDNAME, 1);
    HashMap valuesMap = new HashMap();
    enumTypesSelecter.put(GRAPHQL_VALUES_FIELDNAME, valuesMap);
    valuesMap.put(GRAPHQL_ID_FIELDNAME, 1);
    valuesMap.put(GRAPHQL_VALUE_FIELDNAME, 1);
    valuesMap.put(GRAPHQL_DESCRIPTION_FIELDNAME, 1);
    HashMap publishedSchema = new HashMap();
    schemaSelecter.put(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME, publishedSchema);
    publishedSchema.put(GRAPHQL_ID_FIELDNAME, 1);
    publishedSchema.put(GRAPHQL_VERSIONID_FIELDNAME, 1);
    schemaSelecter.put(GRAPHQL_SCHEMASTATUS_FIELDNAME, 1);
    trueResult.put(GRAPHQL_OPERATION_RESULT_NAME, true);
    falseResult.put(GRAPHQL_OPERATION_RESULT_NAME, false);
  }

  @Override
  public Object doWork(ThirdAPIInput thirdAPIInput) {
    CompletableFuture future = new CompletableFuture();
    CompletableFuture.runAsync(
        () -> {
          try {
            String schemaID =
                String.class.cast(thirdAPIInput.getRunTimeInfo().get(GRAPHQL_SCHEMAID_FIELDNAME));
            DataSelecter dataSelecter =
                GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT)
                    .getObjectDaoMap()
                    .get(GRAPHQL_SCHEMA_TYPENAME)
                    .getDataselecter();
            HashMap eqMap = new HashMap();
            eqMap.put(GRAPHQL_FILTER_EQ_OPERATOR, schemaID);
            HashMap idMap = new HashMap();
            idMap.put(GRAPHQL_ID_FIELDNAME, eqMap);
            HashMap filterMap = new HashMap();
            filterMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, idMap);
            dataSelecter
                .getSingleDoc(filterMap, schemaSelecter)
                .whenCompleteAsync(
                    (schemaInfo, schemaInfoEx) -> {
                      if (null != schemaInfoEx) {
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME, thirdAPIInput.getRunTimeInfo());
                          log.error(
                              "{}",
                              LogData.getErrorLog("E10045", errorMap, (Throwable) schemaInfoEx));
                        }
                        future.completeExceptionally((Throwable) schemaInfoEx);
                      } else {
                        try {
                          HashMap schemaInfoMap = (HashMap) schemaInfo;
                          if(null==schemaInfoMap) {
                            future.completeExceptionally(new BusinessException("E10045"));
                            return;
                          }
                          SchemaObject schemaObject = new SchemaObject();
                          String schemaInfoID =
                              String.class.cast(schemaInfoMap.get(GRAPHQL_ID_FIELDNAME));
                          schemaObject.setId(schemaInfoID);
                          String schemaName =
                              String.class.cast(schemaInfoMap.get(GRAPHQL_NAME_FIELDNAME));
                          schemaObject.setName(schemaName);
                          String databaseKind =
                              String.class.cast(schemaInfoMap.get(GRAPHQL_DATABASEKIND_FIELDNAME));
                          schemaObject.setDatabasekind(databaseKind);
                          List<ThirdPartAPIMetaData> thirdPartAPIMetaDataList =
                              JSONObject.parseArray(
                                  JSONObject.toJSONString(
                                      schemaInfoMap.get(GRAPHQL_THIRD_API_FIELDNAME)),
                                  ThirdPartAPIMetaData.class);
                          schemaObject.setThirdapis(thirdPartAPIMetaDataList);
                          List<ObjectTypeInfo> objectTypeInfos =
                              JSONObject.parseArray(
                                  JSONObject.toJSONString(
                                      schemaInfoMap.get(GRAPHQL_OBJECTTYPES_FIELDNAME)),
                                  ObjectTypeInfo.class);
                          schemaObject.setObjecttypes(objectTypeInfos);
                          List<EnumTypeMetaData> enumTypeMetaDataList =
                              JSONObject.parseArray(
                                  JSONObject.toJSONString(
                                      schemaInfoMap.get(GRAPHQL_ENUMTYPES_FIELDNAME)),
                                  EnumTypeMetaData.class);
                          schemaObject.setEnumtypes(enumTypeMetaDataList);
                          String schemaStatus =
                              String.class.cast(schemaInfoMap.get(GRAPHQL_SCHEMASTATUS_FIELDNAME));
                          DataSourceInfo dataSourceInfo =
                              JSONObject.parseObject(
                                  JSONObject.toJSONString(
                                      schemaInfoMap.get(GRAPHQL_DATASOURCE_FIELDNAME)),
                                  DataSourceInfo.class);
                          if (null == dataSourceInfo) {
                            DataSourceInfo tmpDataSourceInfo =
                                GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT)
                                    .getSchemaData()
                                    .getDatasourceinfo()
                                    .clone();
                            tmpDataSourceInfo.setDatabasename(schemaName);
                            schemaObject.setDatasourceinfo(tmpDataSourceInfo);
                          } else {
                            schemaObject.setDatasourceinfo(dataSourceInfo);
                          }
                          HashMap publishedSchemaInfo =
                              (HashMap) schemaInfoMap.get(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME);
                          String publishedSchemaID = null;
                          Integer versionID = 1;
                          if (null != publishedSchemaInfo) {
                            publishedSchemaID =
                                (String) publishedSchemaInfo.get(GRAPHQL_ID_FIELDNAME);
                            Integer tmpVersionID =
                                Integer.valueOf(
                                    String.valueOf(
                                        publishedSchemaInfo.get(GRAPHQL_VERSIONID_FIELDNAME)));
                            if (null != tmpVersionID) {
                              versionID = tmpVersionID + 1;
                            }
                          }
                          final String publicHisSchemaID = publishedSchemaID;
                          SchemaData publishedSchemaData =
                              GraphQLElementGenerator.transferSchema(schemaObject);

                          One2OneRelationCreater publishedSchemaInserter =
                              (One2OneRelationCreater)
                                  GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT)
                                      .getObjectDaoMap()
                                      .get(GRAPHQL_SCHEMA_TYPENAME)
                                      .getRelation_add_Fields()
                                      .get(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME);
                          HashMap publishedSchemaInsertMap = new HashMap();
                          String insertSchemaID = IDTools.getID();
                          publishedSchemaInsertMap.put(GRAPHQL_ID_FIELDNAME, insertSchemaID);
                          publishedSchemaInsertMap.put(
                              GRAPHQL_SCHEMAOBJECT_FIELDNAME, publishedSchemaData);
                          publishedSchemaInsertMap.put(
                              GRAPHQL_VERSIONID_FIELDNAME, String.valueOf(versionID));
                          SchemaDao schemaInitialDao =
                              DaoFactory.getSchemaDao(publishedSchemaData.getDatabasekind());
                          schemaInitialDao
                              .schemaInitial(publishedSchemaData)
                              .whenComplete(
                                  (initialResult, initialEx) -> {
                                    if (null != initialEx) {
                                      future.completeExceptionally(initialEx);
                                    } else if (!initialResult) {
                                      future.completeExceptionally(new BusinessException("E10002"));
                                    } else {
                                      publishedSchemaInserter
                                          .doAdd(schemaID, publishedSchemaInsertMap)
                                          .whenCompleteAsync(
                                              (insertResult, insertEx) -> {
                                                if (null != insertEx) {
                                                  future.completeExceptionally(
                                                      (Throwable) insertEx);
                                                } else {
                                                  if (null != publicHisSchemaID
                                                      && !"".equals(publicHisSchemaID.trim())) {
                                                    One2ManyRelationCreater
                                                        publishedSchemaHisInserter =
                                                            (One2ManyRelationCreater)
                                                                GraphQLCache.getEasyGQL(
                                                                        GRAPHQL_SCHEMA_ID_DEFAULT)
                                                                    .getObjectDaoMap()
                                                                    .get(GRAPHQL_SCHEMA_TYPENAME)
                                                                    .getRelation_add_Fields()
                                                                    .get(
                                                                        GRAPHQL_PUBLISHEDHISTORY_FIELDNAME);
                                                    List<String> insertIDList = new ArrayList<>();
                                                    insertIDList.add(publicHisSchemaID);
                                                    publishedSchemaHisInserter.fromByID(
                                                        schemaID, insertIDList, false);
                                                  }
                                                  future.complete(trueResult);
                                                  if (null == schemaStatus
                                                      || SCHEMA_STATUS_UNINITIALIZED.equals(
                                                          schemaStatus)) {
                                                    DataUpdater dataUpdater =
                                                        GraphQLCache.getEasyGQL(
                                                                GRAPHQL_SCHEMA_ID_DEFAULT)
                                                            .getObjectDaoMap()
                                                            .get(GRAPHQL_SCHEMA_TYPENAME)
                                                            .getDataupdater();
                                                    HashMap statusUpdateMap = new HashMap();
                                                    statusUpdateMap.put(
                                                        GRAPHQL_SCHEMASTATUS_FIELDNAME,
                                                        SCHEMA_STATUS_RUNNING);
                                                    dataUpdater.updateWhere(
                                                        filterMap,
                                                        statusUpdateMap,
                                                        GRAPHQL_CONFLICT_REPLACE,
                                                        null);
                                                  }
                                                  SchemaStart.startSchema(
                                                      publishedSchemaData, schemaID);
                                                }
                                              });
                                    }
                                  });

                        } catch (Exception e) {
                          future.completeExceptionally(e);
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
  public HashMap<String, ThirdAPIField> inputFields() {
    HashMap<String, ThirdAPIField> inputFieldsMap = new HashMap<>();
    ThirdAPIField schemaIDField = new ThirdAPIField();
    schemaIDField.setDescription("Schema ID");
    schemaIDField.setNotnull(true);
    schemaIDField.setName(GRAPHQL_SCHEMAID_FIELDNAME);
    schemaIDField.setKind(GRAPHQL_TYPEKIND_SCALAR);
    schemaIDField.setType(GRAPHQL_STRING_TYPENAME);
    inputFieldsMap.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaIDField);
    return inputFieldsMap;
  }

  @Override
  public HashMap<String, ThirdAPIField> outputFields() {
    HashMap<String, ThirdAPIField> outputFieldsMap = new HashMap<>();
    ThirdAPIField operationResultField = new ThirdAPIField();
    operationResultField.setDescription("Operation Result");
    operationResultField.setNotnull(true);
    operationResultField.setName(GRAPHQL_OPERATION_RESULT_NAME);
    operationResultField.setType(GRAPHQL_STRING_TYPENAME);
    operationResultField.setKind(GRAPHQL_TYPEKIND_SCALAR);
    outputFieldsMap.put(GRAPHQL_OPERATION_RESULT_NAME, operationResultField);
    return outputFieldsMap;
  }
}
