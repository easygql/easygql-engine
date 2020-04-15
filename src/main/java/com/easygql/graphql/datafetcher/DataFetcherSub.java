package com.easygql.graphql.datafetcher;

import com.easygql.dao.DataSub;
import com.easygql.component.SubscriptionCacheService;
import com.easygql.util.AuthorityUtil;
import com.easygql.util.DaoFactory;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;
import lombok.NonNull;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class DataFetcherSub implements EasyGQLDataFetcher<Object> {

  protected final String schemaID;
  protected final String objectName;
  protected final SchemaData schemaData;
  protected final DataSub dataSub;

  public DataFetcherSub(
      @NonNull String objectName, @NonNull SchemaData schemaData, @NonNull String schemaID) {
    this.objectName = objectName;
    this.schemaData = schemaData;
    this.schemaID = schemaID;
    this.dataSub = DaoFactory.getDataSub(schemaData.getDatabasekind());
  }

  /**
   * @param dataFetchingEnvironment
   * @return
   */
  @Override
  public CompletableFuture<Void> get(DataFetchingEnvironment dataFetchingEnvironment) {
      CompletableFuture<Void> result  = new CompletableFuture<>();
     CompletableFuture.runAsync(
        () -> {
            try{
                Object whereInput = dataFetchingEnvironment.getArgument(GRAPHQL_WHERE_ARGUMENT);
                HashMap contextHashMap = HashMap.class.cast(dataFetchingEnvironment.getContext());
                String subscriptionID =
                        String.class.cast(contextHashMap.get(GRAPHQL_SUBSCRIPTION_ID_ARGUMENT));
                String sessionID = String.class.cast(contextHashMap.get(GRAPHQL_SESSION_ID_ARGUMENT));
                HashMap<String, Object> selectFields =
                        dataFetchingEnvironment.getArgument(GRAPHQL_SELECTFIELDS_FIELDNAME);
                HashMap<String, Object> watchFields =
                        dataFetchingEnvironment.getArgument(GRAPHQL_WATCHFIELDS_FIELDNAME);
                HashMap userInfo = AuthorityUtil.getLoginUser(dataFetchingEnvironment);
                SubscriptionCacheService.subscriptionCacheService.addWebSocket(schemaID,schemaData,selectFields,watchFields,objectName,subscriptionID,sessionID,whereInput,dataSub).whenComplete((addResult,addEx)->{
                    if(null!=addEx){
                        result.completeExceptionally(addEx);
                    } else {
                        result.complete(null);
                    }
                });
            }catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
     return result;
  }
}
