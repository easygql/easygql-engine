package com.easygql.graphql.datafetcher;

import com.easygql.dao.DataInserter;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
import graphql.schema.DataFetchingEnvironment;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.util.AuthorityUtil.insertPermissionFilterBefore;
import static com.easygql.util.EasyGqlUtil.*;
import static com.easygql.util.GraphQLUtil.constructSelectionHashMap;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Slf4j
public class DataFetcherCreate  implements EasyGQLDataFetcher<Object> {
    protected final String objectName;
    protected final SchemaData schemaData;
    protected  final String schemaID;
    protected  final DataInserter dataInserter;

    private HashSet<String> disabledRoles ;
    private HashMap<String, HashSet> forbiddenFields;
    private List<String> lastUpdateFields;
    private List<String> createdAtFields;

    /**
     *
     * @param objectName
     * @param schemaData
     * @param schemaid
     */
    public DataFetcherCreate(@NonNull  String objectName,@NonNull SchemaData schemaData,@NonNull String schemaid) {
        this.objectName=objectName;
        this.schemaData=schemaData;
        this.schemaID=schemaid;
        this.disabledRoles=new HashSet<>();
        this.disabledRoles.addAll(schemaData.getObjectMetaData().get(objectName).getUninsertable_roles());
        this.forbiddenFields = new HashMap<>();
        forbiddenFieldsConstruct(schemaData,objectName,forbiddenFields);
        this.lastUpdateFields =getLastUpdateFields(schemaData.getObjectMetaData().get(objectName));
        this.createdAtFields =getCreateAtFields(schemaData.getObjectMetaData().get(objectName));
        DataInserter dataInserter =DaoFactory.getInsertDao(schemaData.getDatabasekind());
        dataInserter.Init(objectName,schemaData,schemaID);
        this.dataInserter = dataInserter;

    }


    /**
     *
     * @param dataFetchingEnvironment
     * @return
     */
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        HashMap userInfo = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
        Object argument = dataFetchingEnvironment.getArgument(GRAPHQL_OBJECTS_ARGUMENT);
        String conflictStrategy = dataFetchingEnvironment.getArgument(GRAPHQL_CONFLICT_ARGUMENT);
        try {
            insertPermissionFilterBefore(disabledRoles, userInfo, forbiddenFields, argument).whenCompleteAsync((insertObj, permissionEx)->{
                if(null!=permissionEx) {
                    future.completeExceptionally(permissionEx);
                } else {
                    HashMap resultInfo = new HashMap();
                    try {
                        transferIdAndLastUpdate(insertObj, resultInfo,lastUpdateFields,createdAtFields);
                    } catch (Exception e) {
                        if(log.isErrorEnabled()) {
                            HashMap errorMap = new HashMap();
                            errorMap.put(GRAPHQL_OBJECTS_ARGUMENT,argument);
                            errorMap.put(GRAPHQL_CONFLICT_ARGUMENT,conflictStrategy);
                            errorMap.put(GRAPHQL_LOGINEDUSER_OBJECTNAME,userInfo);
                            log.error("{}",LogData.getErrorLog("E10069",errorMap,e));
                        }
                        future.completeExceptionally(new BusinessException("E10069"));
                        return;
                    }
                    dataInserter.insertDoc(resultInfo, conflictStrategy,constructSelectionHashMap(dataFetchingEnvironment.getSelectionSet())).whenComplete((it, insertEx) -> {
                        if (null == insertEx) {
                            future.complete(it);
                        } else {
                            if(log.isErrorEnabled()) {
                                HashMap errorMap = new HashMap();
                                errorMap.put(GRAPHQL_OBJECTS_ARGUMENT,argument);
                                errorMap.put(GRAPHQL_CONFLICT_ARGUMENT,conflictStrategy);
                                errorMap.put(GRAPHQL_LOGINEDUSER_OBJECTNAME,userInfo);
                                log.error("{}",LogData.getErrorLog("E10069",errorMap, (Throwable) insertEx));
                            }
                            future.completeExceptionally(new BusinessException("E10069"));
                        }
                    });
                }
            });
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                HashMap errorMap = new HashMap();
                errorMap.put(GRAPHQL_OBJECTS_ARGUMENT,argument);
                errorMap.put(GRAPHQL_CONFLICT_ARGUMENT,conflictStrategy);
                errorMap.put(GRAPHQL_LOGINEDUSER_OBJECTNAME,userInfo);
                log.error("{}",LogData.getErrorLog("E10069",errorMap,e));
            }
            future.completeExceptionally(new BusinessException("E10069"));
        }

        return future;
    }

    public static void forbiddenFieldsConstruct(SchemaData schemaData, String objectName, HashMap<String,HashSet> forbiddenFields) {
        ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
        Iterator<Map.Entry<String,String>> iterator = objectTypeMetaData.getFields().entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String,String> entry=iterator.next();
            String fieldName = entry.getKey();
            String fieldType = entry.getValue();
            if(fieldType.equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
                ScalarFieldInfo scalarFieldInfo = objectTypeMetaData.getScalarFieldData().get(fieldName);
                if(null!= scalarFieldInfo.getIrrevisible_roles()) {
                    for(String roleName: scalarFieldInfo.getIrrevisible_roles()) {
                        if(null==forbiddenFields.get(roleName)) {
                            forbiddenFields.put(roleName,new HashSet());
                        }
                        forbiddenFields.get(roleName).add(fieldName);
                    }
                }
            } else if(fieldType.equals(GRAPHQL_ENUMTYPE_TYPENAME)) {
                EnumField enumField = objectTypeMetaData.getEnumFieldData().get(fieldName);
                if(null!=enumField.getIrrevisible()) {
                    for(String roleName:enumField.getIrrevisible()) {
                        if(null==forbiddenFields.get(roleName)) {
                            forbiddenFields.put(roleName,new HashSet());
                        }
                        forbiddenFields.get(roleName).add(enumField.getName());
                    }
                }
            } else if(fieldType.equals(GRAPHQL_FROMRELATION_TYPENAME)) {
                RelationField relationField = objectTypeMetaData.getFromRelationFieldData().get(fieldName);
                if (relationField.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)) {
                    for(String roleName:relationField.getIrrevisible()) {
                        if(null==forbiddenFields.get(roleName)) {
                            forbiddenFields.put(roleName,new HashSet());
                        }
                        forbiddenFields.get(roleName).add(relationField.getFromfield());
                    }
                }
            } else {
                RelationField relationField = objectTypeMetaData.getToRelationFieldData().get(fieldName);
                if (relationField.getRelationtype().equals(GRAPHQL_ONE2ONE_NAME)||relationField.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)) {
                    for(String roleName:relationField.getIrrevisible()) {
                        if(null==forbiddenFields.get(roleName)) {
                            forbiddenFields.put(roleName,new HashSet());
                        }
                        forbiddenFields.get(roleName).add(relationField.getTofield());
                    }
                }
            }

        }
    }

}
