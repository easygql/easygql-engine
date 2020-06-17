package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.DataInserter;
import com.easygql.exception.BusinessException;
import com.easygql.util.*;
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

@EasyGQLThirdAPI("UserCreate")
@Slf4j
public class UserCreate extends  ThirdAPI {

    public static GraphQLFieldDefinition.Builder getDef() {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name("UserCreate")
                .argument(
                        GraphQLArgument.newArgument()
                                .name("username")
                                .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
                .argument(
                        GraphQLArgument.newArgument()
                                .name("password")
                                .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
                .type(
                        GraphQLObjectType.newObject()
                                .name("UserCreate_ThirdOutput")
                                .field(
                                        GraphQLFieldDefinition.newFieldDefinition()
                                                .name(GRAPHQL_OPERATION_RESULT_NAME)
                                                .type(Scalars.GraphQLBoolean)));
    }
    @Override
    public Object doWork(ThirdAPIInput thirdAPIInput) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            String schemaID = thirdAPIInput.getSchemaID();
            String username = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_USERNAME_FIELD);
            String password = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_PASSWORD_FIELD);
            String securePassword = PasswordUtils.generateSecurePassword(password,schemaID);
            DataInserter dataInserter = GraphQLCache.getEasyGQL(thirdAPIInput.getSchemaData().getSchemaid()).getObjectDaoMap().get(GRAPHQL_USER_TYPENAME).getDatainserter();
            HashMap insertMap = new HashMap();
            String userID = IDTools.getID();
            insertMap.put(GRAPHQL_ID_FIELDNAME, userID);
            insertMap.put(GRAPHQL_USERNAME_FIELD,username);
            insertMap.put(GRAPHQL_PASSWORD_FIELD,securePassword);
            insertMap.put(GRAPHQL_ROLE_FIELDNAME,ROLE_DEFAULT);
            HashMap<String,HashMap> insertObjMap = new HashMap<>();
            insertObjMap.put(userID,insertMap);
            dataInserter.insertDoc(insertObjMap,CONFLICTSTRATEGY_ERROR).whenCompleteAsync((insertResult,insertEx)->{
                if(null!=insertEx) {
                    future.completeExceptionally((Throwable) insertEx);
                } else {
                    Map result =(Map)insertResult;
                    if(null==result.get(GRAPHQL_AFFECTEDROW_FIELDNAME)) {
                        future.completeExceptionally(new BusinessException("E10109"));
                    } else {
                        Integer affectedRows = (Integer)result.get(GRAPHQL_AFFECTEDROW_FIELDNAME);
                        if(affectedRows<1) {
                            future.completeExceptionally(new BusinessException("E10109"));
                        } else {
                            HashMap operationResult = new HashMap();
                            operationResult.put(GRAPHQL_OPERATION_RESULT_NAME,true);
                            future.complete(operationResult);
                        }
                    }
                }
            });
        });
        return  future;
    }

    @Override
    public HashMap<String, ThirdAPIField> inputFields() {
        ThirdAPIField userNameField = new ThirdAPIField();
        userNameField.setName(GRAPHQL_USERNAME_FIELD);
        userNameField.setDescription("User Name");
        userNameField.setType(GRAPHQL_STRING_TYPENAME);
        userNameField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        userNameField.setNotnull(true);
        ThirdAPIField passwordField = new ThirdAPIField();
        passwordField.setName(GRAPHQL_PASSWORD_FIELD);
        passwordField.setNotnull(true);
        passwordField.setType(GRAPHQL_STRING_TYPENAME);
        passwordField.setDescription("PassWord");
        passwordField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        HashMap<String, ThirdAPIField> inputFieldsMap = new HashMap<>();
        inputFieldsMap.put(GRAPHQL_USERNAME_FIELD, userNameField);
        inputFieldsMap.put(GRAPHQL_PASSWORD_FIELD, passwordField);
        return inputFieldsMap;
    }

    @Override
    public HashMap<String, ThirdAPIField> outputFields() {
        ThirdAPIField operationResultField = new ThirdAPIField();
        operationResultField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        operationResultField.setDescription(GRAPHQL_OPERATION_RESULT_NAME);
        operationResultField.setType(GRAPHQL_BOOLEAN_TYPENAME);
        operationResultField.setNotnull(true);
        operationResultField.setName(GRAPHQL_OPERATION_RESULT_NAME);
        HashMap<String, ThirdAPIField> outputFieldsMap = new HashMap<>();
        outputFieldsMap.put(GRAPHQL_OPERATION_RESULT_NAME,operationResultField);
        return outputFieldsMap;
    }
}
