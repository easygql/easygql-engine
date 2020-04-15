package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.One2ManyRelationCreater;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@EasyGQLThirdAPI(value = "SchemaCreate", type = APIType.MUTATION)
@Slf4j
public class SchemaCreate extends ThirdAPI {

  @Override
  public Object doWork(ThirdAPIInput thirdAPIInput) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            HashMap schemaInfoMap = new HashMap();
            String newSchemaID = IDTools.getID();
            schemaInfoMap.put(GRAPHQL_ID_FIELDNAME, newSchemaID);
            schemaInfoMap.put(
                GRAPHQL_NAME_FIELDNAME, thirdAPIInput.getRunTimeInfo().get(GRAPHQL_NAME_FIELDNAME));
            schemaInfoMap.put(
                GRAPHQL_DESCRIPTION_FIELDNAME,
                thirdAPIInput.getRunTimeInfo().get(GRAPHQL_DESCRIPTION_FIELDNAME));
            schemaInfoMap.put(GRAPHQL_SCHEMASTATUS_FIELDNAME, SCHEMA_STATUS_UNINITIALIZED);
            schemaInfoMap.put(
                GRAPHQL_DATABASEKIND_FIELDNAME,
                thirdAPIInput.getRunTimeInfo().get(GRAPHQL_DATABASEKIND_FIELDNAME));
            String schemaID = thirdAPIInput.getSchemaID();
            HashMap resultInfoMap = new HashMap();
            resultInfoMap.put(newSchemaID, schemaInfoMap);
            GraphQLCache.getEasyGQL(schemaID)
                .getObjectDaoMap()
                .get(GRAPHQL_SCHEMA_TYPENAME)
                .getDatainserter()
                .insertDoc(resultInfoMap, GRAPHQL_CONFLICT_REPLACE, null)
                .whenCompleteAsync(
                    (schemaResult, schemaEX) -> {
                      if (null != schemaEX) {
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.putAll(thirdAPIInput.getRunTimeInfo());
                          log.error(
                              "{}", LogData.getErrorLog("E10066", errorMap, (Throwable) schemaEX));
                        }
                        future.complete(new BusinessException("E10066"));
                      } else {
                        String userType_ID = IDTools.getID();
                        HashMap userType = new HashMap();
                        userType.put(GRAPHQL_NAME_FIELDNAME, GRAPHQL_USER_TYPENAME);
                        userType.put(GRAPHQL_ID_FIELDNAME, userType_ID);
                        userType.put(GRAPHQL_DESCRIPTION_FIELDNAME, "User Type");
                        List insertTypeList = new ArrayList<>();
                        String roleEnumID = newSchemaID + GRAPHQL_ROLE_ID_POSTFIX;
                        HashMap roleEnum = new HashMap();
                        roleEnum.put(GRAPHQL_ID_FIELDNAME, roleEnumID);
                        roleEnum.put(GRAPHQL_NAME_FIELDNAME, GRAPHQL_ROLE_ENUMNAME);
                        roleEnum.put(GRAPHQL_DESCRIPTION_FIELDNAME, "User Role");
                        List insertEnumList = new ArrayList();
                        insertEnumList.add(roleEnum);
                        insertTypeList.add(userType);
                        One2ManyRelationCreater userAdd =
                            (One2ManyRelationCreater)
                                GraphQLCache.getEasyGQL(schemaID)
                                    .getObjectDaoMap()
                                    .get(GRAPHQL_SCHEMA_TYPENAME)
                                    .getRelation_add_Fields()
                                    .get(GRAPHQL_OBJECTTYPES_FIELDNAME);
                        One2ManyRelationCreater roleAdd =
                            (One2ManyRelationCreater)
                                GraphQLCache.getEasyGQL(schemaID)
                                    .getObjectDaoMap()
                                    .get(GRAPHQL_SCHEMA_TYPENAME)
                                    .getRelation_add_Fields()
                                    .get(GRAPHQL_ENUMTYPES_FIELDNAME);
                        userAdd
                            .fromAdd(newSchemaID, insertTypeList, false)
                            .whenCompleteAsync(
                                (userAddResult, userAddEx) -> {
                                  try {
                                    if (null != userAddEx) {
                                      if (log.isErrorEnabled()) {
                                        HashMap errorMap = new HashMap();
                                        errorMap.putAll(thirdAPIInput.getRunTimeInfo());
                                        log.error(
                                            "{}",
                                            LogData.getErrorLog("E10067", errorMap, userAddEx));
                                      }
                                    } else {
                                      One2ManyRelationCreater fieldAdd =
                                          (One2ManyRelationCreater)
                                              GraphQLCache.getEasyGQL(schemaID)
                                                  .getObjectDaoMap()
                                                  .get(GRAPHQL_CONTENTTYPE_TYPENAME)
                                                  .getRelation_add_Fields()
                                                  .get(GRAPHQL_SCALARFIELD_FIELDNAME);
                                      HashMap idField = new HashMap();
                                      idField.put(GRAPHQL_ID_FIELDNAME, IDTools.getID());
                                      idField.put(GRAPHQL_NAME_FIELDNAME, GRAPHQL_ID_FIELDNAME);
                                      idField.put(GRAPHQL_TYPE_FIELDNAME, GRAPHQL_ID_TYPENAME);
                                      idField.put(GRAPHQL_NOTNULL_FIELDNAME, true);
                                      idField.put(GRAPHQL_DESCRIPTION_FIELDNAME, "");
                                      idField.put(GRAPHQL_ISLIST_FIELDNAME, false);
                                      idField.put("invisible_roles", new ArrayList<>());
                                      idField.put("irrevisible_roles", new ArrayList<>());

                                      HashMap userNameField = new HashMap();
                                      userNameField.put(GRAPHQL_ID_FIELDNAME, IDTools.getID());
                                      userNameField.put(
                                          GRAPHQL_NAME_FIELDNAME, GRAPHQL_USERNAME_FIELD);
                                      userNameField.put(
                                          GRAPHQL_TYPE_FIELDNAME, GRAPHQL_STRING_TYPENAME);
                                      userNameField.put(GRAPHQL_NOTNULL_FIELDNAME, true);
                                      userNameField.put(GRAPHQL_DESCRIPTION_FIELDNAME, "");
                                      userNameField.put(GRAPHQL_ISLIST_FIELDNAME, false);
                                      userNameField.put("invisible_roles", new ArrayList<>());
                                      userNameField.put("irrevisible_roles", new ArrayList<>());
                                      HashMap passwordField = new HashMap();
                                      passwordField.put(GRAPHQL_ID_FIELDNAME, IDTools.getID());
                                      passwordField.put(
                                          GRAPHQL_NAME_FIELDNAME, GRAPHQL_PASSWORD_FIELD);
                                      passwordField.put(
                                          GRAPHQL_TYPE_FIELDNAME, GRAPHQL_STRING_TYPENAME);
                                      passwordField.put(GRAPHQL_NOTNULL_FIELDNAME, true);
                                      passwordField.put(GRAPHQL_DESCRIPTION_FIELDNAME, "");
                                      passwordField.put(GRAPHQL_ISLIST_FIELDNAME, false);
                                      passwordField.put("invisible_roles", new ArrayList<>());
                                      passwordField.put("irrevisible_roles", new ArrayList<>());
                                      List fieldList = new ArrayList();
                                      fieldList.add(idField);
                                      fieldList.add(userNameField);
                                      fieldList.add(passwordField);
                                      fieldAdd
                                          .fromAdd(userType_ID, fieldList, false)
                                          .whenComplete(
                                              (userTypeAddResult, userTypeAddEx) -> {
                                                if (null != userTypeAddEx) {
                                                  if (log.isErrorEnabled()) {
                                                    HashMap errorMap = new HashMap();
                                                    errorMap.put(GRAPHQL_FROM_ID, userType_ID);
                                                    errorMap.put(GRAPHQL_TO_OBJECT, fieldList);
                                                    log.error(
                                                        "{}",
                                                        LogData.getErrorLog(
                                                            "E10067", errorMap, userTypeAddEx));
                                                  }
                                                  future.completeExceptionally(
                                                      new BusinessException("E10067"));
                                                } else {
                                                  roleAdd
                                                      .fromAdd(newSchemaID, insertEnumList, false)
                                                      .whenCompleteAsync(
                                                          (roleAddResult, roleAddEx) -> {
                                                            if (null != roleAddEx) {
                                                              if (log.isErrorEnabled()) {
                                                                HashMap errorMap = new HashMap();
                                                                errorMap.putAll(
                                                                    thirdAPIInput.getRunTimeInfo());
                                                                log.error(
                                                                    "{}",
                                                                    LogData.getErrorLog(
                                                                        "E10068", errorMap,
                                                                        roleAddEx));
                                                              }
                                                              future.completeExceptionally(
                                                                  new BusinessException("E10068"));
                                                            } else {
                                                              try {
                                                                One2ManyRelationCreater
                                                                    roleEnumAdd =
                                                                        (One2ManyRelationCreater)
                                                                            GraphQLCache.getEasyGQL(
                                                                                    schemaID)
                                                                                .getObjectDaoMap()
                                                                                .get(
                                                                                    GRAPHQL_ENUMTYPE_TYPENAME)
                                                                                .getRelation_add_Fields()
                                                                                .get(
                                                                                    GRAPHQL_VALUES_FIELDNAME);
                                                                HashMap adminRoleMap =
                                                                    new HashMap();
                                                                adminRoleMap.put(
                                                                    GRAPHQL_ID_FIELDNAME,
                                                                    IDTools.getID());
                                                                adminRoleMap.put(
                                                                    GRAPHQL_VALUE_FIELDNAME,
                                                                    "Admin");
                                                                adminRoleMap.put(
                                                                    GRAPHQL_DESCRIPTION_FIELDNAME,
                                                                    "Admin");
                                                                HashMap guestRoleMap =
                                                                    new HashMap();
                                                                guestRoleMap.put(
                                                                    GRAPHQL_ID_FIELDNAME,
                                                                    IDTools.getID());
                                                                guestRoleMap.put(
                                                                    GRAPHQL_VALUE_FIELDNAME,
                                                                    "Guest");
                                                                guestRoleMap.put(
                                                                    GRAPHQL_DESCRIPTION_FIELDNAME,
                                                                    "Guest");
                                                                List roleList = new ArrayList();
                                                                roleList.add(adminRoleMap);
                                                                roleList.add(guestRoleMap);
                                                                roleEnumAdd
                                                                    .fromAdd(
                                                                        roleEnumID, roleList, false)
                                                                    .whenComplete(
                                                                        (roleEnumResult,
                                                                            roleEnumEx) -> {
                                                                          if (null != roleEnumEx) {
                                                                            if (log
                                                                                .isErrorEnabled()) {
                                                                              HashMap errorMap =
                                                                                  new HashMap();
                                                                              errorMap.put(
                                                                                  GRAPHQL_FROM_ID,
                                                                                  roleEnumID);
                                                                              errorMap.put(
                                                                                  GRAPHQL_TO_OBJECT,
                                                                                  roleList);
                                                                              log.error(
                                                                                  "E10068",
                                                                                  errorMap,
                                                                                  roleEnumEx);
                                                                            }
                                                                            future
                                                                                .completeExceptionally(
                                                                                    new BusinessException(
                                                                                        "E10068"));
                                                                          } else {
                                                                            future.complete(
                                                                                schemaResult);
                                                                          }
                                                                        });
                                                              } catch (Exception e) {
                                                                if (log.isErrorEnabled()) {
                                                                  HashMap errorMap = new HashMap();
                                                                  errorMap.putAll(
                                                                      thirdAPIInput
                                                                          .getRunTimeInfo());
                                                                  log.error(
                                                                      "{}",
                                                                      LogData.getErrorLog(
                                                                          "E10068", errorMap, e));
                                                                }
                                                                future.completeExceptionally(
                                                                    new BusinessException(
                                                                        "E10068"));
                                                              }
                                                            }
                                                          });
                                                }
                                              });
                                    }
                                  } catch (Exception e) {
                                    if (log.isErrorEnabled()) {
                                      HashMap errorMap = new HashMap();
                                      errorMap.putAll(thirdAPIInput.getRunTimeInfo());
                                      log.error("{}", LogData.getErrorLog("E10067", errorMap, e));
                                    }
                                    future.completeExceptionally(new BusinessException("E10067"));
                                  }
                                });
                      }
                    });
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.putAll(thirdAPIInput.getRunTimeInfo());
              log.error("{}", LogData.getErrorLog("E10066", errorMap, e));
            }
            future.completeExceptionally(new BusinessException("E10068"));
          }
        });
    return future;
  }

  @Override
  public HashMap<String, ThirdAPIField> inputFields() {
    HashMap inputFieldsMap = new HashMap();
    ThirdAPIField nameScalarField = new ThirdAPIField();
    nameScalarField.setName(GRAPHQL_NAME_FIELDNAME);
    nameScalarField.setNotnull(true);
    nameScalarField.setType(GRAPHQL_STRING_TYPENAME);
    nameScalarField.setKind(GRAPHQL_TYPEKIND_SCALAR);
    nameScalarField.setDescription("Name Of Schema");
    inputFieldsMap.put(GRAPHQL_NAME_FIELDNAME, nameScalarField);
    ThirdAPIField descriptionScalarField = new ThirdAPIField();
    descriptionScalarField.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    descriptionScalarField.setType(GRAPHQL_STRING_TYPENAME);
    descriptionScalarField.setKind(GRAPHQL_TYPEKIND_SCALAR);
    descriptionScalarField.setDescription("Description Of Schema");
    inputFieldsMap.put(GRAPHQL_DESCRIPTION_FIELDNAME, descriptionScalarField);
    ThirdAPIField databaseKindField = new ThirdAPIField();
    databaseKindField.setName(GRAPHQL_DATABASEKIND_FIELDNAME);
    databaseKindField.setType(GRAPHQL_DATABASEKIND_ENUMNAME);
    databaseKindField.setKind(GRAPHQL_TYPEKIND_ENUM);
    databaseKindField.setDefaultValue(DATABASE_KIND_POSTGRES);
    inputFieldsMap.put(GRAPHQL_DATABASEKIND_FIELDNAME, databaseKindField);
    return inputFieldsMap;
  }

  @Override
  public HashMap<String, ThirdAPIField> outputFields() {
    HashMap<String, ThirdAPIField> outputFieldsMap = new HashMap<>();
    ThirdAPIField affectedRow = new ThirdAPIField();
    affectedRow.setType(GRAPHQL_INT_TYPENAME);
    affectedRow.setName(GRAPHQL_AFFECTEDROW_FIELDNAME);
    affectedRow.setDescription("rows affected");
    affectedRow.setKind(GRAPHQL_TYPEKIND_SCALAR);
    outputFieldsMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME, affectedRow);
    ThirdAPIField idList = new ThirdAPIField();
    idList.setKind(GRAPHQL_TYPEKIND_SCALAR);
    idList.setDescription("affected id list");
    idList.setName(GRAPHQL_IDLIST_FIELDNAME);
    idList.setType(GRAPHQL_ID_TYPENAME);
    idList.setIslist(true);
    outputFieldsMap.put(GRAPHQL_IDLIST_FIELDNAME, idList);
    return outputFieldsMap;
  }
}
