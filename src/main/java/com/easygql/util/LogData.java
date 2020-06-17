package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import com.easygql.component.ConfigurationProperties;
import io.vertx.core.impl.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.HashMap;
import java.util.Map;

public class LogData {
  private static MessageSource messageSource;

  public static synchronized void resetMessage() {
    ReloadableResourceBundleMessageSource messageBundle =
        new ReloadableResourceBundleMessageSource();
    messageBundle.setBasename("classpath:languages/message");
    messageBundle.setDefaultEncoding("UTF-8");
    messageSource = messageBundle;
  }

  public static String getErrorLog(String logType, Object detail, Throwable throwable) {
    HashMap logData = new HashMap();
    logData.put("detail", detail);
    logData.put("logType", logType);
    if(null!=throwable) {
      logData.put("errorInfo", ExceptionUtils.getStackTrace(throwable));
    } else {
      System.out.println("xxxx");
    }

    logData.put(
        "message",
        messageSource.getMessage(
            logType, null, ConfigurationProperties.getInstance().GRAPHQL_LANG));
    String tmpStr = JSONObject.toJSONString(logData);
    try {
      tmpStr=StringEscapeUtils.unescapeJavaScript(tmpStr);
    } catch (Exception e) {

    }
    return tmpStr;
  }

  public static String getInfoLog(String logType, Map detail) {
    HashMap logData = new HashMap();
    logData.put("detail", detail);
    logData.put("logType", logType);
    logData.put(
        "message",
        messageSource.getMessage(
            logType, null, ConfigurationProperties.getInstance().GRAPHQL_LANG));
    String tmpStr = JSONObject.toJSONString(logData);
    try {
      tmpStr=StringEscapeUtils.unescapeJavaScript(tmpStr);
    } catch (Exception e) {

    }
    return tmpStr;
  }
  public static String getErrorMessage(String logType) {
      return messageSource.getMessage(logType,null,ConfigurationProperties.getInstance().GRAPHQL_LANG);
  }
}
