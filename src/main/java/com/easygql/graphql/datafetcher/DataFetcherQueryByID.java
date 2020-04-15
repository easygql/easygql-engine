package com.easygql.graphql.datafetcher;

import com.easygql.component.ConfigurationProperties;
import com.easygql.dao.DataSelecter;
import com.easygql.util.*;
import graphql.schema.DataFetchingEnvironment;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_ONE2MANY_NAME;
import static com.easygql.util.GraphQLUtil.constructSelectionHashMap;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class DataFetcherQueryByID implements EasyGQLDataFetcher<Object> {
  protected final String objectName;
  protected final SchemaData schemaData;
  protected final String schemaID;
  protected final DataSelecter dataSelecter;

  /**
   * @param objectName
   * @param schemaData
   */
  public DataFetcherQueryByID(
      @NonNull String objectName, @NonNull SchemaData schemaData, @NonNull String schemaID) {
    this.objectName = objectName;
    this.schemaData = schemaData;
    this.schemaID = schemaID;
    DataSelecter tmpDataSelecter = DaoFactory.getSelecterDao(schemaData.getDatabasekind());
    tmpDataSelecter.Init(objectName, schemaData, schemaID);
    this.dataSelecter = tmpDataSelecter;
  }

  /**
   * @param dataFetchingEnvironment
   * @return
   */
  @Override
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            String id = dataFetchingEnvironment.getArgument("id");
            HashMap userInfo = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
            HashMap eqCondition = new HashMap();
            eqCondition.put(GRAPHQL_FILTER_EQ_OPERATOR, id);
            HashMap idCondition = new HashMap();
            idCondition.put(GRAPHQL_ID_FIELDNAME, eqCondition);
            HashMap filterCondtion = new HashMap();
            filterCondtion.put(GRAPHQL_FILTER_FILTER_OPERATOR, filterCondtion);
            AuthorityUtil.queryPermissionFilterBefore(
                userInfo, filterCondtion, objectName, schemaData);
            dataSelecter
                .getSingleDoc(
                    filterCondtion,
                    constructSelectionHashMap(dataFetchingEnvironment.getSelectionSet()))
                .whenComplete(
                    (queryReuslt, queryEx) -> {
                      if (null != queryEx) {
                        future.completeExceptionally(queryEx);
                      } else {
                        try {
                          if (null != queryReuslt) {
                            String roleInfo =
                                String.class.cast(
                                    userInfo.get(
                                        ConfigurationProperties.getInstance()
                                            .ROLE_IN_USER_FIELDNAME));
                            filterQueryField(
                                roleInfo,
                                schemaData,
                                objectName,
                                (HashMap<String, Object>) queryReuslt);
                          }
                          future.complete(queryReuslt);
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

  public static void filterQueryField(
      String roleInfo, SchemaData schemaData, String objectName, HashMap<String, Object> result) {
    if (null == result) {
      return;
    }
    ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
    Map<String, String> fieldsMap = objectTypeMetaData.getFields();
    Iterator<String> selectFieldsIterator = result.keySet().iterator();
    while (selectFieldsIterator.hasNext()) {
      String fieldName = selectFieldsIterator.next();
      String fieldType = fieldsMap.get(fieldName);
      if (fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
        ScalarFieldInfo scalarFieldInfo = objectTypeMetaData.getScalarFieldData().get(fieldName);
        if (null != scalarFieldInfo.getInvisible_roles()
            && scalarFieldInfo.getInvisible_roles().contains(roleInfo)) {
          result.remove(fieldName);
        }
      } else if (fieldType.equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
        EnumField enumField = objectTypeMetaData.getEnumFieldData().get(fieldName);
        if (null != enumField.getInvisible() && enumField.getInvisible().contains(roleInfo)) {
          result.remove(roleInfo);
        }
      } else if (fieldType.equals(GRAPHQL_FROMRELATION_TYPENAME)) {
        RelationField relationField = objectTypeMetaData.getFromRelationFieldData().get(fieldName);
        if(relationField.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)||relationField.getRelationtype().equals(GRAPHQL_MANY2MANY_NAME)) {
          if(null == result.get(fieldName)) {
            result.put(fieldName,new ArrayList<>());
          }
          List<HashMap> selectObjList = (List<HashMap>)result.get(fieldName);
          for (HashMap tmpResultMap : selectObjList ) {
            filterQueryField(roleInfo, schemaData, relationField.getToobject(), tmpResultMap);
          }
        } else {
          HashMap selectTmpFields = (HashMap) result.get(fieldName);
          filterQueryField(roleInfo, schemaData, relationField.getToobject(), selectTmpFields);
        }
      } else {
        RelationField relationField = objectTypeMetaData.getToRelationFieldData().get(fieldName);
        if(relationField.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)||relationField.getRelationtype().equals(GRAPHQL_MANY2MANY_NAME)) {
          if(null == result.get(fieldName)) {
            result.put(fieldName,new ArrayList<>());
          }
          List<HashMap> selectObjList = (List<HashMap>)result.get(fieldName);
          for (HashMap tmpResultMap : selectObjList ) {
            filterQueryField(roleInfo, schemaData, relationField.getFromobject(), tmpResultMap);
          }
        } else {
          HashMap selectTmpFields = (HashMap) result.get(fieldName);
          filterQueryField(roleInfo, schemaData, relationField.getFromobject(), selectTmpFields);
        }

      }
    }
  }
}
