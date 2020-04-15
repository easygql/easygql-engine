package com.easygql.util;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.pubsub.PgSubscriber;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnection;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_SCHEMAID_FIELDNAME;
import static com.easygql.component.ConfigurationProperties.getInstance;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Slf4j
public class PostgreSQLPoolCache {
    public static  Vertx vertx=Vertx.vertx();
    private static ConcurrentHashMap<String, PgPool> poolCache = new ConcurrentHashMap<String, PgPool>();

    /**
     * @param schemaID
     * @param dataSourceInfo
     */
    private static void addPool(@NonNull String schemaID, @NonNull DataSourceInfo dataSourceInfo) {
        if (!poolCache.containsKey(schemaID)) {
            //**此处未来应该添加针对schemaid的分布式锁
            poolCache.putIfAbsent(schemaID, getDatabaseClient(dataSourceInfo));
        }
    }



    /**
     * 获取连接工厂
     *
     * @param schemaID
     * @return
     */
    public static CompletableFuture<SqlConnection> getConnectionFactory(@NonNull String schemaID) {
        CompletableFuture<SqlConnection> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                if(null==poolCache.get(schemaID)) {
                    try {
                        addPool(schemaID,GraphQLCache.getEasyGQL(schemaID).getSchemaData().getDatasourceinfo());
                    } catch (Exception e) {
                        if(log.isErrorEnabled()) {
                            HashMap errorMap = new HashMap();
                            errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME,schemaID);
                            log.error("{}",LogData.getErrorLog("E10101",errorMap,e));
                        }
                    }
                }
                poolCache.get(schemaID).getConnection(sqlConnectionAsyncResult -> {
                    if(sqlConnectionAsyncResult.succeeded()) {
                        SqlConnection sqlConnection = sqlConnectionAsyncResult.result();
                        future.complete(sqlConnectionAsyncResult.result());
                    } else {
                        future.completeExceptionally(sqlConnectionAsyncResult.cause());
                    }
                });
            }catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;

    }

    private static PgPool getDatabaseClient(DataSourceInfo dataSourceInfo) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(Optional.ofNullable(dataSourceInfo.getPort()).orElse(getInstance().POSTGRES_PORT))
                .setHost(Optional.ofNullable(dataSourceInfo.getHost()).orElse(getInstance().POSTGRES_IP))
                .setDatabase(Optional.ofNullable(dataSourceInfo.getDatabasename()).orElse(getInstance().POSTGRES_DATABASENAME))
                .setUser(Optional.ofNullable(dataSourceInfo.getUsername()).orElse(getInstance().POSTGRES_USER))
                .setPassword(Optional.ofNullable(dataSourceInfo.getPassword()).orElse(getInstance().POSTGRES_PASS));
        // Pool options
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(getInstance().POSTGRES_MAX_CONNECTIONS);
// Create the client pool
        PgPool client = PgPool.pool(vertx,connectOptions, poolOptions);
        return client;
    }

    public static PgConnectOptions getPgConnectionOptions(DataSourceInfo dataSourceInfo) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(Optional.ofNullable(dataSourceInfo.getPort()).orElse(getInstance().POSTGRES_PORT))
                .setHost(Optional.ofNullable(dataSourceInfo.getHost()).orElse(getInstance().POSTGRES_IP))
                .setDatabase(Optional.ofNullable(dataSourceInfo.getDatabasename()).orElse(getInstance().POSTGRES_DATABASENAME))
                .setUser(Optional.ofNullable(dataSourceInfo.getUsername()).orElse(getInstance().POSTGRES_USER))
                .setPassword(Optional.ofNullable(dataSourceInfo.getPassword()).orElse(getInstance().POSTGRES_PASS));
        return connectOptions;
    }
    public static PgSubscriber getSubscriber(DataSourceInfo dataSourceInfo) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(Optional.ofNullable(dataSourceInfo.getPort()).orElse(getInstance().POSTGRES_PORT))
                .setHost(Optional.ofNullable(dataSourceInfo.getHost()).orElse(getInstance().POSTGRES_IP))
                .setDatabase(Optional.ofNullable(dataSourceInfo.getDatabasename()).orElse(getInstance().POSTGRES_DATABASENAME))
                .setUser(Optional.ofNullable(dataSourceInfo.getUsername()).orElse(getInstance().POSTGRES_USER))
                .setPassword(Optional.ofNullable(dataSourceInfo.getPassword()).orElse(getInstance().POSTGRES_PASS));
        return PgSubscriber.subscriber(vertx,connectOptions);
    }
}
