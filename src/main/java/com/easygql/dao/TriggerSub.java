package com.easygql.dao;

import com.easygql.util.SchemaData;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletableFuture;

public  interface TriggerSub{
    void init(SchemaData schemaData, String schemaID);
    CompletableFuture<Publisher<Object>> getPublish(String typeName);
}
