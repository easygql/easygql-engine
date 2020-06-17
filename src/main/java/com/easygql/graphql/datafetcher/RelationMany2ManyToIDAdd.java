package com.easygql.graphql.datafetcher;

import com.easygql.dao.Many2ManyRelationCreater;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class RelationMany2ManyToIDAdd implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected Many2ManyRelationCreater many2ManyRelationCreater;
    protected HashSet<String> disabledRoles=new HashSet<>();

    public RelationMany2ManyToIDAdd(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.many2ManyRelationCreater= DaoFactory.getMany2ManyRelationCreator(schemaData.getDatabasekind());
        this.many2ManyRelationCreater.Init(schemaData,schemaID,relationField);
        if(null!=relationField.getUnmodifiableRoles()){
            disabledRoles.addAll(relationField.getInvisibleRoles());
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
                many2ManyRelationCreater.toAdd(toID,fromIDs,isReset).whenComplete((result,resultEx)->{
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
