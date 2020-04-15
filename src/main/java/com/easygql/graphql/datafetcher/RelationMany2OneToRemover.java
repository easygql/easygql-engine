package com.easygql.graphql.datafetcher;

import com.easygql.dao.Many2OneRelationRemover;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

public class RelationMany2OneToRemover implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected Many2OneRelationRemover many2OneRelationRemover;
    protected HashSet<String> disabledRoles=new HashSet<>();

    public RelationMany2OneToRemover(RelationField relationField, String schemaID, SchemaData schemaData, int direction) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.many2OneRelationRemover = DaoFactory.getMany2OneRelationRemover(schemaData.getDatabasekind());
        this.many2OneRelationRemover.Init(schemaData,schemaID,relationField);
        if(null!=relationField.getIrrevisible()){
            disabledRoles.addAll(relationField.getInvisible());
        }
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                List<String> fromIDs = dataFetchingEnvironment.getArgument(GRAPHQL_FROM_ID);
                String toID = dataFetchingEnvironment.getArgument(GRAPHQL_TO_ID);
                Boolean isReset = dataFetchingEnvironment.getArgument(GRAPHQL_RESET_FIELDNAME);
                many2OneRelationRemover.toRemove(toID,fromIDs,isReset).whenComplete((result,resultEx)->{
                    if(null!=resultEx) {
                        future.completeExceptionally(resultEx);
                    } else {
                        future.complete(result);
                    }
                });
            } catch ( Exception e) {
                future.completeExceptionally(e);
            }

        });
        return future;
    }
}
