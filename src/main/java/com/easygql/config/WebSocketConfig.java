package com.easygql.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket配置
 * <p>
 * 自动注册使用了@ServerEndpoint注解声明的Websocket endpoint
 * 要注意，如果使用独立的servlet容器，而不是直接使用springboot的内置容器，就不要注入ServerEndpointExporter，因为它将由容器自己提供和管理。
 *
 * @author fenyorome
 * @date 2019/01/12
 */

@Configuration
public class WebSocketConfig  {
    @Autowired
    @Qualifier("GraphQLWebSocketHandler")
    private WebSocketHandler webSocketHandler;
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/graphqlws/*/subscription", webSocketHandler);
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new  WebSocketHandlerAdapter(){

            @Override
            public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
                String secinfo=exchange.getRequest().getHeaders().getFirst("Sec-WebSocket-Protocol");
                if(StringUtils.isNotEmpty(secinfo)&&secinfo.equals("graphql-ws")) {
                    exchange.getResponse().getHeaders().add("sec-websocket-protocol","graphql-ws");
                }
                return super.handle(exchange, handler);
            }
        };
    }

}
