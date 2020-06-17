package com.easygql.graphql.datafetcher;

import com.easygql.dao.Many2OneRelationCreater;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

public class RelationMany2OneFromIDAdd implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected Many2OneRelationCreater many2OneRelationCreater;
    protected HashSet<String> disabledRoles=new HashSet<>();

    public RelationMany2OneFromIDAdd(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.many2OneRelationCreater = DaoFactory.getMany2OneRelationCreator(schemaData.getDatabasekind());
        this.many2OneRelationCreater.Init(schemaData,schemaID,relationField);
        if(null!=relationField.getUnmodifiableRoles()){
            disabledRoles.addAll(relationField.getInvisibleRoles());
        }
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                String fromID = dataFetchingEnvironment.getArgument(GRAPHQL_FROM_ID);
                String toID = dataFetchingEnvironment.getArgument(GRAPHQL_TO_ID);
                Boolean isReset = dataFetchingEnvironment.getArgument(GRAPHQL_RESET_FIELDNAME);
                many2OneRelationCreater.fromAdd(fromID,toID,isReset).whenComplete((result,resultEx)->{
                    if(null!=resultEx) {
                        future.completeExceptionally(resultEx);
                    } else {
                        future.complete(result);
                    }
                });
            } catch ( Exception e ) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
