package com.easygql.graphql.datafetcher;

import com.easygql.component.ConfigurationProperties;
import com.easygql.exception.NotAuthorizedException;
import com.easygql.thirdapis.ThirdAPI;
import com.easygql.util.*;
import graphql.schema.DataFetchingEnvironment;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author guofen
 * @date 2019/12/18 15:07
 */
public class DataFetcherThirdAPI implements EasyGQLDataFetcher<Object> {
    private SchemaData schemaData;
    private String schemaID;
    private String apiName;
    private HashSet<String> disabledRoles;


    public DataFetcherThirdAPI( SchemaData schemaData, String schemaID, String apiName) {
        this.schemaData=schemaData;
        this.schemaID=schemaID;
        this.apiName=apiName;
        if(null!=schemaData.getMutationMetaData().get(apiName)) {
            disabledRoles=new HashSet<>();
            if(null!=schemaData.getMutationMetaData().get(apiName).getDisabled_roles()){
                disabledRoles.addAll(schemaData.getMutationMetaData().get(apiName).getDisabled_roles());
            }
        } else if(null!=schemaData.getQueryMetaData().get(apiName)) {
            disabledRoles=new HashSet<>();
            if(null!=schemaData.getQueryMetaData().get(apiName).getDisabled_roles()){
                disabledRoles.addAll(schemaData.getQueryMetaData().get(apiName).getDisabled_roles());
            }
        } else {
            disabledRoles=new HashSet<>();
            if(null!=schemaData.getSubscriptionMetaData().get(apiName).getDisabled_roles()){
                disabledRoles.addAll(schemaData.getSubscriptionMetaData().get(apiName).getDisabled_roles());
            }
        }
    }
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        HashMap userInfo = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
        String roleInfo =
                String.class.cast(
                        userInfo.get(ConfigurationProperties.getInstance().ROLE_IN_USER_FIELDNAME));
        if(disabledRoles.contains(roleInfo)) {
            throw new NotAuthorizedException();
        } else {
            ThirdAPI thirdAPI = ThirdAPIPool.getThirdAPI(apiName);
            HashMap inputRunTime = new HashMap();
            inputRunTime.putAll(dataFetchingEnvironment.getArguments());
            ThirdAPIInput thirdAPIInput = new ThirdAPIInput();
            thirdAPIInput.setSchemaID(schemaID);
            thirdAPIInput.setRunTimeInfo(inputRunTime);
            thirdAPIInput.setSchemaData(schemaData);
            return thirdAPI.doWork(thirdAPIInput);
        }

    }
}
