package com.easygql.dao;

import com.easygql.util.ObjectTypeMetaData;
import com.easygql.util.SchemaData;
import com.easygql.util.Trigger;
import io.reactivex.ObservableEmitter;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TriggerDao {
    void init(SchemaData schemaData ,String schemaID);
    CompletableFuture<Void> ListenTrigger(String typeName, ObservableEmitter<Object> emitter);
}
