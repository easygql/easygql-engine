package com.easygql.controller;

import com.alibaba.fastjson.JSONObject;
import com.easygql.exception.NotAuthorizedException;
import com.easygql.service.QueryService;
import com.easygql.util.JwtUtil;
import graphql.schema.idl.SchemaPrinter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_ROLE_FIELDNAME;

/**
 * @author ：fenyorome
 * @date ：Created in 2019/1/31/031 13:04
 * @description：${description}
 * @modified By：
 * @version: $version$
 */
@Slf4j
@Controller
public class SchemaApiController {
    private static final SchemaPrinter sp = new SchemaPrinter();

    /**
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(NotAuthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public  String NotAuthorizedExceptionHandler(Exception ex){
        JSONObject jo = new JSONObject();
        jo.put("error","not authorized");
        return jo.toJSONString();
    }

    /**
     *
     * @param schemaid
     * @param paramstr
     * @param header
     * @return
     * @throws IOException
     */
    @PostMapping(value = "/api/graphql/{schemaid}",consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Mono<Object> postGraphQL(@PathVariable("schemaid") String schemaid,@RequestBody String paramstr,@RequestHeader Map<String,String> header) throws IOException {
        HashMap userInfo = new HashMap();
        if(null==header.get("Authorization")|| !JwtUtil.checkJWT(String.valueOf(header.get("Authorization")))){
            userInfo.put("id", UUID.randomUUID().toString());
            userInfo.put("username","Admin");
            userInfo.put(GRAPHQL_ROLE_FIELDNAME,"Admin");
        } else {
            userInfo=JSONObject.parseObject(JSONObject.toJSONString(JwtUtil.parser(header.get("Authorization")).getBody()),HashMap.class);
        }
        return Mono.fromFuture(QueryService.query(schemaid,paramstr,userInfo));
    }

    /**
     *
     * @param schemaid
     * @param model
     * @return
     */
    @RequestMapping("/dataexplorer/{schemaid}")
    public Mono<String> getGraphIQL(@PathVariable("schemaid") String schemaid, final Model model) {
        HashMap userinfo = new HashMap();
        userinfo.put("id","test");
        userinfo.put("username","graphiql");
        userinfo.put("password","graphiql");
        userinfo.put("role","Admin");
        model.addAttribute("schemaurl","/api/graphql/"+schemaid);
        model.addAttribute("websocketurl","/graphqlws/"+schemaid+"/subscription");
        model.addAttribute("usertoken",JwtUtil.buildJWT(userinfo));
        return Mono.create(stringMonoSink -> stringMonoSink.success("graphiql"));
    }
}
