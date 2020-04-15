package com.easygql.graphql.datafetcher;

import com.easygql.dao.One2OneRelationCreater;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_FROM_ID;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_TO_ID;

public class RelationOne2OneFromIDAdd implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected One2OneRelationCreater one2OneRelationCreater;
    protected HashSet<String> disabledRoles=new HashSet<>();

    public RelationOne2OneFromIDAdd(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.one2OneRelationCreater = DaoFactory.getOne2OneRelationCreator(schemaData.getDatabasekind());
        this.one2OneRelationCreater.Init(schemaData,schemaID,relationField);
        if(null!=relationField.getIrrevisible()){
            disabledRoles.addAll(relationField.getInvisible());
        }
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try{
                String fromID = dataFetchingEnvironment.getArgument(GRAPHQL_FROM_ID);
                String toID = dataFetchingEnvironment.getArgument(GRAPHQL_TO_ID);
                one2OneRelationCreater.doAddByID(fromID,toID).whenComplete((result,resultEx)->{
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
