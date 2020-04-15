package com.easygql.graphql.datafetcher;

import com.easygql.dao.DataUpdater;
import com.easygql.util.AuthorityUtil;
import com.easygql.util.DaoFactory;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.graphql.datafetcher.DataFetcherCreate.forbiddenFieldsConstruct;
import static com.easygql.util.AuthorityUtil.updatePermissionFilterBefore;
import static com.easygql.util.GraphQLUtil.constructSelectionHashMap;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class DataFetcherUpdateMany implements EasyGQLDataFetcher<Object> {
    protected final String objectName;
    protected final SchemaData schemaData;
    protected  final String schemaID;
    protected final DataUpdater dataUpdater;
    protected  final HashMap forbiddenFields;
    protected final HashMap<String, HashMap> rowConstraints;
    protected  final HashSet<String> disabledRoles;

    /**
     * @author guofen
     * @date 2019-10-27 16:41
     */
    public DataFetcherUpdateMany(@NonNull String objectName,@NonNull SchemaData schemaData,@NonNull String schemaID) {
        this.objectName=objectName;
        this.schemaData=schemaData;
        this.schemaID=schemaID;
        DataUpdater tmpdataUpdater = DaoFactory.getUpdaterDao(schemaData.getDatabasekind());
        tmpdataUpdater.Init(objectName,schemaData,schemaID);
        this.forbiddenFields = new HashMap<>();
        forbiddenFieldsConstruct(schemaData,objectName,forbiddenFields);
        this.rowConstraints = schemaData.getObjectMetaData().get(objectName).getUpdate_constraints();
        disabledRoles = new HashSet<>();
        if(null!=schemaData.getObjectMetaData().get(objectName).getUnupdatable_roles()) {
            disabledRoles.addAll(schemaData.getObjectMetaData().get(objectName).getUnupdatable_roles());
        }
        this.dataUpdater=tmpdataUpdater;
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                Object whereObj = dataFetchingEnvironment.getArgument(GRAPHQL_WHERE_ARGUMENT);
                Object updateObj = dataFetchingEnvironment.getArgument(GRAPHQL_OBJECT_ARGUMENT);
                String updateType = EVENTTYPE_UPDATE;
                HashMap userInfo = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
                updatePermissionFilterBefore(disabledRoles, userInfo, forbiddenFields, rowConstraints, (HashMap) whereObj, (HashMap) updateObj, schemaData, objectName).whenCompleteAsync((finalCondition, permissionEx) -> {
                    if(null!=permissionEx) {
                        future.completeExceptionally((Throwable) permissionEx);
                    } else {
                        dataUpdater.updateWhere(finalCondition, updateObj, updateType,constructSelectionHashMap(dataFetchingEnvironment.getSelectionSet())).whenCompleteAsync((it,throwable) ->
                        {
                            if(null!=throwable) {
                                future.completeExceptionally(throwable);
                            } else {
                                future.complete(it);
                            }
                        });
                    }
                });
            } catch ( Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }


}