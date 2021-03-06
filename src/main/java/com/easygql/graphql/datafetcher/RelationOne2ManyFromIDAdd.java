package com.easygql.graphql.datafetcher;

import com.easygql.dao.One2ManyRelationCreater;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

public class RelationOne2ManyFromIDAdd implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected One2ManyRelationCreater one2ManyRelationCreater;
    protected HashSet<String> disabledRoles=new HashSet<>();

    public RelationOne2ManyFromIDAdd(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.one2ManyRelationCreater = DaoFactory.getOne2ManyRelationCreator(schemaData.getDatabasekind());
        this.one2ManyRelationCreater.Init(schemaData,schemaID,relationField);
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
                List<String> toIDs = dataFetchingEnvironment.getArgument(GRAPHQL_TO_ID);
                Boolean isReset = dataFetchingEnvironment.getArgument(GRAPHQL_RESET_FIELDNAME);
                one2ManyRelationCreater.fromByID(fromID,toIDs,isReset).whenComplete((result,resultEx)->{
                    if(null!=resultEx){
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
