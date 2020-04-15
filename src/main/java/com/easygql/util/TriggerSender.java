package com.easygql.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
            WebClient.Builder webClientBuilder = WebClient.builder();
            URL url = new URL(trigger.getWebhookurl());
            String path = url.getFile().substring(0, url.getFile().lastIndexOf('/'));
            String baseURL = url.getProtocol() + "://" + url.getHost() + path;
            Integer urlIndex = url.getFile().lastIndexOf('/');
            String urlStr = url.getFile().substring(urlIndex + 1);
            webClientBuilder.baseUrl(baseURL);
            webClientBuilder.defaultHeader(
                HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            WebClient webClient = webClientBuilder.build();
            HashMap headerMap = trigger.getHeaders();
            Iterator<Map.Entry<String, String>> iterator = headerMap.keySet().iterator();
              Consumer<HttpHeaders> httpHeadersConsumer = (httpHeaders)->{
                  while(iterator.hasNext()) {
                      Map.Entry<String, String> entry = iterator.next();
                      String headerKey = entry.getKey();
                      String headerVal = entry.getValue();
                      httpHeaders.add(headerKey,headerVal);
                  }
              };
              String statusStr = trigger.getOk_status();
              Integer expectedStatus=200;
              try{
                  if(null==statusStr||"".equals(statusStr.trim())) {
                      expectedStatus=Integer.valueOf(statusStr);
                  } else {
                      expectedStatus=200;
                  }
              } catch (Exception e) {
                  expectedStatus = 200;
              }
              Integer finalStatus = webClient.post().uri(urlStr).headers(httpHeadersConsumer).body(Mono.just(payLoadStr),String.class).exchange().block().statusCode().value();
              if(finalStatus.equals(expectedStatus)) {
                  future.complete(true);
              } else {
                  future.complete(false);
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
