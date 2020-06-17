package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.DataUpdater;
import com.easygql.exception.BusinessException;
import com.easygql.exception.NotAuthorizedException;
import com.easygql.util.GraphQLCache;
import com.easygql.util.ThirdAPIField;
import com.easygql.util.ThirdAPIInput;
import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@EasyGQLThirdAPI("RolePromoted")
@Slf4j
public class RolePromoted extends  ThirdAPI {
    public static GraphQLFieldDefinition.Builder getDef() {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(GRAPHQL_ROLEPROMOTED_APINAME)
                .argument(
                        GraphQLArgument.newArgument()
                                .name(GRAPHQL_USERNAME_FIELD)
                                .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
                .type(
                        GraphQLObjectType.newObject()
                                .name("RolePromoted_ThirdOutput")
                                .field(
                                        GraphQLFieldDefinition.newFieldDefinition()
                                                .name("OperationResult")
                                                .type(Scalars.GraphQLBoolean)));
    }
    @Override
    public Object doWork(ThirdAPIInput thirdAPIInput) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                String userRole = String.valueOf(thirdAPIInput.getUserInfo().get(GRAPHQL_ROLE_FIELDNAME));
                if(!ROLE_ADMIN.equals(userRole)) {
                    future.completeExceptionally(new NotAuthorizedException());
                } else {
                    String schemaID = thirdAPIInput.getSchemaID();
                    String username = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_USERNAME_FIELD);
                    String role = String.valueOf(thirdAPIInput.getRunTimeInfo().get(GRAPHQL_ROLE_FIELDNAME));
                    if(role.equals(ROLE_GUEST)) {
                        future.completeExceptionally(new BusinessException("E10107"));
                    }
                    DataUpdater dataUpdater = GraphQLCache.getEasyGQL(thirdAPIInput.getSchemaData().getSchemaid()).getObjectDaoMap().get(GRAPHQL_USER_TYPENAME).getDataupdater();
                    HashMap nameEqMap = new HashMap();
                    nameEqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,username);

                    HashMap userMap = new HashMap();
                    userMap.put(GRAPHQL_USERNAME_FIELD,nameEqMap);
                    HashMap filterMap = new HashMap();
                    filterMap.put(GRAPHQL_FILTER_FILTER_OPERATOR,userMap);
                    HashMap updateObj = new HashMap();
                    updateObj.put(GRAPHQL_ROLE_FIELDNAME,role);
                    dataUpdater.updateWhere(filterMap,updateObj,CONFLICTSTRATEGY_REPLACE).whenCompleteAsync((updateResult,updateEx)->{
                        if(null!=updateEx) {
                            future.completeExceptionally(updateEx);
                        } else {
                            Map result =(Map)updateResult;
                            if(null==result.get(GRAPHQL_AFFECTEDROW_FIELDNAME)) {
                                future.completeExceptionally(new BusinessException("E10108"));
                            } else {
                                Integer affectedRows = (Integer)result.get(GRAPHQL_AFFECTEDROW_FIELDNAME);
                                if(affectedRows<1) {
                                    future.completeExceptionally(new BusinessException("E10108"));
                                } else {
                                    HashMap operationResult = new HashMap();
                                    operationResult.put(GRAPHQL_OPERATION_RESULT_NAME,true);
                                    future.complete(operationResult);
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return  future;

    }

    @Override
    public HashMap<String, ThirdAPIField> inputFields() {
        return null;
    }

    @Override
    public HashMap<String, ThirdAPIField> outputFields() {
        return null;
    }
}
