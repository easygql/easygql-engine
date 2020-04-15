package com.easygql.graphql.datafetcher;

import com.easygql.util.AuthorityUtil;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class RelationDestory implements EasyGQLDataFetcher<Object> {
    protected String objectName;
    protected String fieldName;
    protected String schemaID;
    protected SchemaData schemaData;

    /**
     *
     * @param objectName
     * @param schemaData
     * @param fieldName
     * @param schemaID
     */
    public RelationDestory(String objectName,SchemaData schemaData,String fieldName,String schemaID) {
        this.objectName=objectName;
        this.fieldName=fieldName;
        this.schemaID=schemaID;
        this.schemaData=schemaData;
    }


    /**
     *
     * @param dataFetchingEnvironment
     * @return
     */
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        AuthorityUtil.authorityVerify(dataFetchingEnvironment, null);
        HashMap logineduser = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
        Object argument = dataFetchingEnvironment.getArgument("relation");
        CompletableFuture<Object> run = new CompletableFuture<>();
//        CompletableFuture.runAsync(() -> {
//            try {
//                HashMap relation = HashMap.class.cast(argument);
//                String srcid = String.class.cast(relation.get(sourceobjectname + "_id"));
//                Object destid = relation.get(fieldname + "_id");
//                rethinkRelationRemover.removeRelation(srcid, destid, logineduser).exceptionally(ex -> {
//                    return ex;
//                }).thenAccept(obj -> {
//                    if(obj instanceof  Exception) {
//                        run.completeExceptionally((Exception)obj);
//                    } else {
//                        run.complete(obj);
//                    }
//                    });
//                } catch(Exception e){
//                    run.completeExceptionally(e);
//                }
//            });
            return run;
        }
    }
