package com.easygql.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_PAYLOADARGS_FIELDNAME;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_TRIGGERS_FIELDNAME;

@Slf4j
public class TriggerSender
{

    public static CompletableFuture<Boolean> sendEvent(String payLoadStr, Trigger trigger) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
              HttpPost post = null;
              HttpClient httpClient = new DefaultHttpClient();
              // 设置超时时间
              httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000);
              httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 2000);

              post = new HttpPost(trigger.getWebhookurl());
              HashMap headerMap = trigger.getHeaders();
              if (null == headerMap) {
                  headerMap = new HashMap();
                  headerMap.put("Content-Type", "application/json");
              }
              Iterator<Map.Entry<String, String>> iterator = headerMap.entrySet().iterator();
              while (iterator.hasNext()) {
                  Map.Entry<String, String> entry = iterator.next();
                  String headerKey = entry.getKey();
                  String headerVal = entry.getValue();
                  post.setHeader(headerKey,headerVal);
              }
              // 构建消息实体
              StringEntity entity = new StringEntity(payLoadStr, Charset.forName("UTF-8"));
              entity.setContentEncoding("UTF-8");
              // 发送Json格式的数据请求
              entity.setContentType("application/json");
              post.setEntity(entity);

              HttpResponse response = httpClient.execute(post);

              // 检验返回码
              String statusStr = trigger.getOk_status();
              Integer expectedStatus = 200;
              try {
                  if (null == statusStr || "".equals(statusStr.trim())) {
                      expectedStatus = Integer.valueOf(statusStr);
                  } else {
                      expectedStatus = 200;
                  }
              } catch (Exception e) {
                  expectedStatus = 200;
              }
              int statusCode = response.getStatusLine().getStatusCode();
              if(statusCode != expectedStatus){
                  future.complete(false);
              }else{
                  future.complete(true);
              }
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap triggerEventMap = new HashMap();
              triggerEventMap.put(GRAPHQL_PAYLOADARGS_FIELDNAME, payLoadStr);
              triggerEventMap.put(GRAPHQL_TRIGGERS_FIELDNAME, trigger);
              log.error("{}", LogData.getErrorLog("E10083", triggerEventMap, e));
            }
            future.complete(false);
          }
        });
        return future;
    }
}
