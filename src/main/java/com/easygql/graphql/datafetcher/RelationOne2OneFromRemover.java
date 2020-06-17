package com.easygql.graphql.datafetcher;

import com.easygql.dao.One2OneRelationRemover;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_FROM_ID;

public class RelationOne2OneFromRemover implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected One2OneRelationRemover one2OneRemover;
    protected HashSet<String> disabledRoles=new HashSet<>();

    public RelationOne2OneFromRemover(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.one2OneRemover = DaoFactory.getOne2OneRelationRemover(schemaData.getDatabasekind());
        this.one2OneRemover.Init(schemaData,schemaID,relationField);
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
                one2OneRemover.fromRemove(fromID).whenComplete((result,resultEx)->{
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
