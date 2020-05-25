package com.easygql.thirdapis;

import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.DataSelecter;
import com.easygql.util.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019/12/18 21:23
 */
@EasyGQLThirdAPI(value = "UserLogin",type = APIType.MUTATION)
@Slf4j
public class UserLogin extends  ThirdAPI {
    @Override
    public Object doWork(ThirdAPIInput thirdAPIInput) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            String username = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_USERNAME_FIELD);
            String password = (String)thirdAPIInput.getRunTimeInfo().get(GRAPHQL_PASSWORD_FIELD);
            DataSelecter dataSelecter = GraphQLCache.getEasyGQL(thirdAPIInput.getSchemaData().getSchemaid()).getObjectDaoMap().get(GRAPHQL_USER_TYPENAME).getDataselecter();
            HashMap nameEqMap = new HashMap();
            nameEqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,username);
            HashMap passwordEqMap = new HashMap();
            passwordEqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,password);
            HashMap userMap = new HashMap();
            userMap.put(GRAPHQL_USERNAME_FIELD,nameEqMap);
            userMap.put(GRAPHQL_PASSWORD_FIELD,passwordEqMap);
            HashMap filterMap = new HashMap();
            filterMap.put(GRAPHQL_FILTER_FILTER_OPERATOR,filterMap);
            HashMap<String,Object> selectionMap= new HashMap<>();
            ObjectTypeMetaData objectTypeMetaData = thirdAPIInput.getSchemaData().getObjectMetaData().get(GRAPHQL_USER_TYPENAME);
            for (ScalarFieldInfo scalarField : objectTypeMetaData.getScalarFieldData().values() ) {
                selectionMap.put(scalarField.getName(),1);
            }
            for (EnumField enumField:objectTypeMetaData.getEnumFieldData().values()) {
                selectionMap.put(enumField.getName(),1);
            };
            dataSelecter.getSingleDoc(filterMap,selectionMap).whenCompleteAsync((queryResult,queryEx)->{
               if(null!=queryEx) {
                   future.completeExceptionally(queryEx);
               }  else {
                   if(null==queryResult) {
                       HashMap mapInfo = new HashMap();
                       mapInfo.put(GRAPHQL_LOGIN_RESULT_FIELDNAME,false);
                       future.complete(mapInfo);
                   } else {
                       HashMap userInfo =  (HashMap) queryResult;
                       String token = JwtUtil.buildJWT(userInfo);
                       HashMap mapInfo = new HashMap();
                       mapInfo.put(GRAPHQL_TOKEN_FIELDNAME,token);
                       mapInfo.put(GRAPHQL_LOGIN_RESULT_FIELDNAME,true);
                       future.complete(mapInfo);
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
        inputFieldsMap.put(GRAPHQL_USERNAME_FIELD,userNameField);
        inputFieldsMap.put(GRAPHQL_PASSWORD_FIELD,passwordField);
        return inputFieldsMap;
    }

    @Override
    public HashMap<String, ThirdAPIField> outputFields() {
        ThirdAPIField tokenField = new ThirdAPIField();
        tokenField.setName(GRAPHQL_TOKEN_FIELDNAME);
        tokenField.setDescription("Token");
        tokenField.setType(GRAPHQL_STRING_TYPENAME);
        tokenField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        tokenField.setNotnull(true);
        ThirdAPIField loginResultField = new ThirdAPIField();
        loginResultField.setKind(GRAPHQL_TYPEKIND_SCALAR);
        loginResultField.setDescription("loginResult");
        loginResultField.setType(GRAPHQL_BOOLEAN_TYPENAME);
        loginResultField.setNotnull(true);
        loginResultField.setName(GRAPHQL_LOGIN_RESULT_FIELDNAME);
        HashMap<String, ThirdAPIField> outputFieldsMap = new HashMap<>();
        outputFieldsMap.put(GRAPHQL_LOGIN_RESULT_FIELDNAME,loginResultField);
        outputFieldsMap.put(GRAPHQL_TOKEN_FIELDNAME,tokenField);
        return outputFieldsMap;
    }

}
