package com.easygql.handler;

import com.alibaba.fastjson.JSONObject;
import com.easygql.exception.BusinessException;
import com.easygql.util.LogData;
import com.easygql.util.MessageServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_SESSIONID_FIELDNAME;


/**
 * @author ：fenyorome
 * @date ：Created in 2019/1/23/023 13:13
 * @description：${description}
 * @modified By：
 * @version: $version$
 */
@Component("GraphQLWebSocketHandler")
@Slf4j
public class GraphQLWebSocketHandler implements WebSocketHandler {
    public static ConcurrentHashMap<String,WebSocketSessionHandler> sessionhandlers=new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, HashSet<String>> schema_sessions=new ConcurrentHashMap<>();

    public GraphQLWebSocketHandler() {
    }

    //建立连接的方法

    /**
     *
     * @param webSocketSession
     * @return
     */
    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        String sessionid = webSocketSession.getId();
        if (!sessionhandlers.keySet().contains(sessionid)) {
            String path=webSocketSession.getHandshakeInfo().getUri().getPath();
            String[] paths = path.split("/");
            String schemaID=paths[paths.length-2];
            WebSocketSessionHandler sessionHandler = new WebSocketSessionHandler(webSocketSession,schemaID);
            sessionhandlers.put(sessionid, sessionHandler);
            if(null==schema_sessions.get(schemaID)) {
                schema_sessions.put(schemaID,new HashSet<String>());
            }
            schema_sessions.get(schemaID).add(sessionid);
            webSocketSession.receive().subscribeOn(Schedulers.elastic())
                    .map(msg->msg.getPayloadAsText())
                    .doOnNext(msg->sessionHandler.onReceive(msg))
                    .doOnError(msg->sessionHandler.onError(msg))
                    .doOnSubscribe(sub->sessionHandler.onSubscribe())
                    .doFinally(it->{
                        sessionHandler.onComplete();
                        sessionhandlers.remove(sessionid);
                        HashSet<String> sessionids=schema_sessions.get(schemaID);
                        sessionids.remove(sessionid);
                    }).subscribe();

           return webSocketSession.send(Flux.from(sessionHandler.outputmessage.replay().autoConnect(1).publishOn(Schedulers.elastic())).map(this::toJSON).map(webSocketSession::textMessage));
        }
        if(log.isErrorEnabled()) {
            HashMap errorMap = new HashMap();
            errorMap.put(GRAPHQL_SESSIONID_FIELDNAME,sessionid);
            log.error("{}", LogData.getErrorLog("E10025",errorMap,new BusinessException("E10025")));
        }
        return  Mono.empty();
    }



    /**
     *
     * @param event
     * @return
     */
    private String toJSON(MessageServer event) {
      return JSONObject.toJSONString(event);
    }

    /**
     *
     * @param sessionid
     * @return
     */
    public static Optional<WebSocketSessionHandler> gethandler(String sessionid){
        return  Optional.of(sessionhandlers.get(sessionid));
    }

    /**
     *
     * @param schemaid
     */
    public static void endSchema(String schemaid){
        HashSet<String> sessions = schema_sessions.get(schemaid);
        schema_sessions.remove(schemaid);
        for(String sessionid:sessions) {
            WebSocketSessionHandler sessionHandler = sessionhandlers.get(sessionid);
            sessionHandler.doClose();
            sessionhandlers.remove(sessionid);
        }
    }
}
