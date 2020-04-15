package com.easygql.graphql.datafetcher;

import com.easygql.dao.DataDeleter;
import com.easygql.util.AuthorityUtil;
import com.easygql.util.DaoFactory;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_WHERE_ARGUMENT;
import static com.easygql.util.AuthorityUtil.deletePermissionFilterBefore;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Slf4j
public class DataFetcherDeleteMany implements EasyGQLDataFetcher<Object> {
    protected final String objectName;
    protected final SchemaData schemaData;
    protected final String schemaID;
    protected final DataDeleter dataDeleter;
    private HashSet<String> disabledRoles;
    private HashMap<String, HashMap> rowConstraints;

    /**
     *
     * @param objectName
     * @param schemaData
     * @param schemaID
     */
    public DataFetcherDeleteMany(String objectName,SchemaData schemaData,String schemaID){
        this.objectName=objectName;
        this.schemaData=schemaData;
        this.schemaID=schemaID;
        this.dataDeleter = DaoFactory.getDeleteDao(schemaData.getDatabasekind());
        this.dataDeleter.Init(objectName,schemaData,schemaID);
        this.disabledRoles = new HashSet<>();
        this.disabledRoles.addAll(schemaData.getObjectMetaData().get(objectName).getUndeletable_roles());
        this.rowConstraints = schemaData.getObjectMetaData().get(objectName).getDelete_constraints();
    }

    /**
     *
     * @param dataFetchingEnvironment
     * @return
     */
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment)  {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                Object whereInput = dataFetchingEnvironment.getArgument(GRAPHQL_WHERE_ARGUMENT);
                HashMap userInfo = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
                deletePermissionFilterBefore(disabledRoles, userInfo, rowConstraints, (HashMap) whereInput, schemaData, objectName).whenCompleteAsync((finalCondition, permissionEx) -> {
                    if(null!=permissionEx) {
                        future.completeExceptionally(permissionEx);
                    } else {
                        dataDeleter.deleteDocs(finalCondition).whenComplete((deleteResult,deleteEx)->{
                            if(null!=deleteEx) {
                                future.completeExceptionally(deleteEx);
                            } else {
                                future.complete(deleteResult);
                            }
                        });
                    }
                });

            } catch ( Exception e) {
                future.completeExceptionally(e);
            }
        });
        return  future;
    }




}
