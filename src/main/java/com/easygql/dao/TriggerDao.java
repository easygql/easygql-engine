package com.easygql.dao;

import com.easygql.util.SchemaData;
import io.reactivex.ObservableEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface TriggerDao {
    void init(SchemaData schemaData ,String schemaID);
    CompletableFuture<Void> ListenTrigger(String typeName, ObservableEmitter<Object> emitter);
    void AddTriggerEvent(String triggerID, String eventType, Map<String,Object> oldVal, Map<String,Object> newVal,Boolean isSucceed);
}
