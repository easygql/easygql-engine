package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.DataUpdater;
import com.easygql.exception.BusinessException;
import com.easygql.util.GraphQLCache;
import com.easygql.util.PasswordUtils;
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
@EasyGQLThirdAPI("PasswordChange")
@Slf4j
public class PasswordChange extends ThirdAPI {
    public static GraphQLFieldDefinition.Builder getDef() {
     return GraphQLFieldDefinition.newFieldDefinition()
             .name("PasswordChange")
             .argument(
                     GraphQLArgument.newArgument()
                             .name("old_password")
                             .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
             .argument(
                     GraphQLArgument.newArgument()
                             .name("new_password")
                             .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
             .type(
                     GraphQLObjectType.newObject()
                             .name("PasswordChange_ThirdOutput")
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
                String schemaID = thirdAPIInput.getSchemaID();
                String username = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_USERNAME_FIELD);
                String oldPassword = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_OLD_PASSWORD_FIELD);
                String newPassword = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_NEW_PASSWORD_FIELD);
                String oldSecurePassword = PasswordUtils.generateSecurePassword(oldPassword,schemaID);
                String newSecurePassword = PasswordUtils.generateSecurePassword(newPassword,schemaID);
                DataUpdater dataUpdater = GraphQLCache.getEasyGQL(thirdAPIInput.getSchemaData().getSchemaid()).getObjectDaoMap().get(GRAPHQL_USER_TYPENAME).getDataupdater();
                HashMap nameEqMap = new HashMap();
                nameEqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,username);
                HashMap passwordEqMap = new HashMap();
                passwordEqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,oldSecurePassword);
                HashMap userMap = new HashMap();
                userMap.put(GRAPHQL_USERNAME_FIELD,nameEqMap);
                userMap.put(GRAPHQL_PASSWORD_FIELD,passwordEqMap);
                HashMap filterMap = new HashMap();
                filterMap.put(GRAPHQL_FILTER_FILTER_OPERATOR,userMap);
                HashMap updateObj = new HashMap();
                updateObj.put(GRAPHQL_PASSWORD_FIELD,newSecurePassword);
                dataUpdater.updateWhere(filterMap,updateObj,CONFLICTSTRATEGY_REPLACE).whenCompleteAsync((updateResult,updateEx)->{
                    if(null!=updateEx) {
                        future.completeExceptionally(updateEx);
                    } else {
                        Map result =(Map)updateResult;
                        if(null==result.get(GRAPHQL_AFFECTEDROW_FIELDNAME)) {
                            future.completeExceptionally(new BusinessException("E10105"));
                        } else {
                            Integer affectedRows = (Integer)result.get(GRAPHQL_AFFECTEDROW_FIELDNAME);
                            if(affectedRows<1) {
                                future.completeExceptionally(new BusinessException("E10106"));
                            } else {
                                HashMap operationResult = new HashMap();
                                operationResult.put(GRAPHQL_OPERATION_RESULT_NAME,true);
                                future.complete(operationResult);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
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
        ThirdAPIField oldPasswordField = new ThirdAPIField();
        oldPasswordField.setName(GRAPHQL_OLD_PASSWORD_FIELD);
        oldPasswordField.setNotnull(true);
        oldPasswordField.setType(GRAPHQL_STRING_TYPENAME);
        oldPasswordField.setDescription("Old PassWord");
        oldPasswordField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        ThirdAPIField newPasswordField = new ThirdAPIField();
        newPasswordField.setName(GRAPHQL_NEW_PASSWORD_FIELD);
        newPasswordField.setNotnull(true);
        newPasswordField.setType(GRAPHQL_STRING_TYPENAME);
        newPasswordField.setDescription("New PassWord");
        newPasswordField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        HashMap<String, ThirdAPIField> inputFieldsMap = new HashMap<>();
        inputFieldsMap.put(GRAPHQL_USERNAME_FIELD,userNameField);
        inputFieldsMap.put(GRAPHQL_OLD_PASSWORD_FIELD,oldPasswordField);
        inputFieldsMap.put(GRAPHQL_NEW_PASSWORD_FIELD,newPasswordField);
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
