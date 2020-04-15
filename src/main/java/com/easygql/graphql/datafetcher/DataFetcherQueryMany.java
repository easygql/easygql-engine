package com.easygql.graphql.datafetcher;

import com.easygql.component.ConfigurationProperties;
import com.easygql.dao.DataSelecter;
import com.easygql.util.AuthorityUtil;
import com.easygql.util.DaoFactory;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.util.GraphQLUtil.constructSelectionHashMap;

//import com.easygql.dao.rethink.RethinkSelecter;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class DataFetcherQueryMany implements EasyGQLDataFetcher<Object> {
    protected final String objectName;
    protected final SchemaData schemaData;
    protected final String schemaID;
    protected final DataSelecter dataSelecter;

    /**
     *
     * @param objectName
     * @param schemaData
     */
    public DataFetcherQueryMany(@NonNull String objectName, @NonNull  SchemaData schemaData,@NonNull  String schemaID) {
        this.objectName=objectName;
        this.schemaData=schemaData;
        this.schemaID=schemaID;
        this.dataSelecter = DaoFactory.getSelecterDao(schemaData.getDatabasekind());
        this.dataSelecter.Init(objectName, schemaData, schemaID);
    }

    /**
     *
     * @param dataFetchingEnvironment
     * @return
     */
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                Object whereInput = dataFetchingEnvironment.getArgument(GRAPHQL_WHERE_ARGUMENT);
                Integer skip = dataFetchingEnvironment.getArgument(GRAPHQL_SKIP_ARGUMENT);
                Integer limit = dataFetchingEnvironment.getArgument(GRAPHQL_LIMIT_ARGUMENT);
                String orderBy = dataFetchingEnvironment.getArgument(GRAPHQL_ORDERBY_ARGUMENT);
                HashMap loginedUser = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
                AuthorityUtil.queryPermissionFilterBefore(
                        loginedUser, (HashMap)whereInput, objectName, schemaData);
                dataSelecter.getFilterDocs(whereInput,skip,limit,orderBy,constructSelectionHashMap(dataFetchingEnvironment.getSelectionSet())).whenCompleteAsync((queryResult,queryEx)->{
                    if(null!=queryEx) {
                        future.completeExceptionally(queryEx);
                    } else {
                        if(null!=queryResult)  {
                            List<Map> reusltMap = (List<Map>)queryResult;
                            String roleInfo =
                                    String.class.cast(
                                            loginedUser.get(
                                                    ConfigurationProperties.getInstance()
                                                            .ROLE_IN_USER_FIELDNAME));
                            for(Map resultTmpMap:reusltMap) {
                                DataFetcherQueryByID.filterQueryField(roleInfo,schemaData,objectName,(HashMap<String, Object>)resultTmpMap);
                            }
                        }
                        future.complete(queryResult);
                    }
                });
            } catch ( Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;


    }
}
