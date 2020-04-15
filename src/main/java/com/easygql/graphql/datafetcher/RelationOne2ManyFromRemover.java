package com.easygql.graphql.datafetcher;

import com.easygql.dao.One2ManyRelationRemover;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_FROM_ID;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_TO_ID;

public class RelationOne2ManyFromRemover implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected One2ManyRelationRemover one2ManyRemover;
    protected HashSet<String> disabledRoles=new HashSet<>();
    public RelationOne2ManyFromRemover(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.one2ManyRemover = DaoFactory.getOne2ManyRelationRemover(schemaData.getDatabasekind());
        this.one2ManyRemover.Init(schemaData,schemaID,relationField);
        if(null!=relationField.getIrrevisible()){
            disabledRoles.addAll(relationField.getInvisible());
        }
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                String fromID = dataFetchingEnvironment.getArgument(GRAPHQL_FROM_ID);
                List<String> toIDs = dataFetchingEnvironment.getArgument(GRAPHQL_TO_ID);
                one2ManyRemover.fromRemove(fromID,toIDs).whenComplete((result,resultEx)->{
                    if(null!=resultEx) {
                        future.completeExceptionally(resultEx);
                    } else {
                        future.complete(result);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }

        });
        return future;
    }
}
