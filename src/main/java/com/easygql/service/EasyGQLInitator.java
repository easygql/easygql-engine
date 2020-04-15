package com.easygql.service;

import com.easygql.annotation.EasyGQLDaoGenerator;
import com.easygql.annotation.EasyGQLThirdAPI;
import com.easygql.dao.DaoGenerator;
import com.easygql.exception.BusinessException;
import com.easygql.function.ClassMapper;
import com.easygql.function.ClassVerify;
import com.easygql.thirdapis.ThirdAPI;
import com.easygql.util.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.easygql.component.ConfigurationProperties.*;

@Slf4j
public class EasyGQLInitator  {
  private ClassVerify daoGeneratorVerify = (tmpClass)->{
    if(null==tmpClass) {
      return false;
    } else if(null==tmpClass.getAnnotation(EasyGQLDaoGenerator.class)) {
      return false;
    } else {
      List<Class> classesList = Arrays.asList(tmpClass.getInterfaces());
      if(classesList.contains(DaoGenerator.class)) {
        return true;
      } else {
        return false;
      }
    }
  };
  private ClassVerify thirdAPIVerify = (tmpClass)->{
    if(null==tmpClass) {
      return false;
    } else if(null==tmpClass.getAnnotation(EasyGQLThirdAPI.class)) {
      return false;
    } else if(!tmpClass.getSuperclass().equals(ThirdAPI.class)) {
      return false;
    } else {
      return true;
    }
  };
  private ClassMapper daoGeneratorBind = (hashMap, classes) -> {
    if(daoGeneratorVerify.isValid(classes)) {
      EasyGQLDaoGenerator easyGQLDaoGenerator = classes.getAnnotation(EasyGQLDaoGenerator.class);
      String databasekind = easyGQLDaoGenerator.value();
      try {
        DaoGenerator daoGenerator = (DaoGenerator) classes.newInstance();
        hashMap.put(databasekind,daoGenerator);
      } catch (InstantiationException e) {
        if(log.isErrorEnabled()) {
          HashMap errorMap = new HashMap();
          errorMap.put(GRAPHQL_CLASSNAME_FIELDNAME,classes.getCanonicalName());
          log.error("{}", LogData.getErrorLog("E10027",errorMap,e));
        }
      } catch (IllegalAccessException e) {
        if(log.isErrorEnabled()) {
          HashMap errorMap = new HashMap();
          errorMap.put(GRAPHQL_CLASSNAME_FIELDNAME,classes.getCanonicalName());
          log.error("{}", LogData.getErrorLog("E10027",errorMap,e));
        }
      }
    }
  };
  private ClassMapper thirdAPIBind=(hashMap, classes) -> {
    if(thirdAPIVerify.isValid(classes)) {
      EasyGQLThirdAPI annotation = classes.getAnnotation(EasyGQLThirdAPI.class);
      try {
        ThirdAPI thirdAPI = (ThirdAPI)classes.newInstance();
        APIType apiKind = annotation.type();
        if(apiKind.equals(APIType.QUERY)) {
          HashMap queryMap = (HashMap) hashMap.get(GRAPHQL_QUERY_TYPENAME);
          queryMap.put(annotation.value(),thirdAPI);
        } else if(apiKind.equals(APIType.MUTATION)) {
          HashMap queryMap = (HashMap) hashMap.get(GRAPHQL_MUTATION_TYPENAME);
          queryMap.put(annotation.value(),thirdAPI);
        } else {
          HashMap queryMap = (HashMap) hashMap.get(GRAPHQL_SUBSCRIPTION_TYPENAME);
          queryMap.put(annotation.value(),thirdAPI);
        }
      } catch (InstantiationException e) {
        if(log.isErrorEnabled()) {
          HashMap errorMap = new HashMap();
          errorMap.put(GRAPHQL_CLASSNAME_FIELDNAME,classes.getCanonicalName());
          log.error("{}", LogData.getErrorLog("E10009",errorMap,e));
        }
      } catch (IllegalAccessException e) {
        if(log.isErrorEnabled()) {
          HashMap errorMap = new HashMap();
          errorMap.put(GRAPHQL_CLASSNAME_FIELDNAME,classes.getCanonicalName());
          log.error("{}", LogData.getErrorLog("E10009",errorMap,e));
        }
      }
    }
  };
  public  void restPool() {
    log.info("Loading ThirdAPIS...");
    if(log.isInfoEnabled()) {
      log.info("{}",LogData.getInfoLog("I10005",null));
    }
    HashMap thirdAPIMap = new HashMap();
    thirdAPIMap.put(GRAPHQL_QUERYAPI_FIELDNAME,new HashMap<>());
    thirdAPIMap.put(GRAPHQL_MUTATIONAPI_FIELDNAME,new HashMap<>());
    thirdAPIMap.put(GRAPHQL_SUBSCRIPTIONAPI_FIELDNAME,new HashMap<>());
    getClassesWithAnnotationFromPackage("com.easygql.thirdapis",thirdAPIBind,thirdAPIMap);
    ThirdAPIPool.resetPool(thirdAPIMap);
    if(log.isInfoEnabled()) {
      log.info("{}",LogData.getInfoLog("I10006",null));
    }
    HashMap daoGeneratorMap = new HashMap();
    getClassesWithAnnotationFromPackage("com.easygql.dao",daoGeneratorBind,daoGeneratorMap);
    DaoFactory.reset(daoGeneratorMap);

  };

  private static   void getClassesWithAnnotationFromPackage(
      String packageName,ClassMapper classBinder,HashMap classMap) {
    String packageDirName = packageName.replace('.', '/');
    Enumeration<URL> dirs = null;
    try {
      dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
    } catch (IOException e) {
      if(log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_PACKAGENAME_FIELDNAME,packageName);
        log.error("{}", LogData.getErrorLog("E10028",errorMap,e));
      }
      return ;
    }

    while (dirs.hasMoreElements()) {
      URL url =
          dirs
              .nextElement(); // file:/D:/E/workspaceGitub/springboot/JSONDemo/target/classes/com/yq/controller
      String protocol = url.getProtocol(); // file

      // https://docs.oracle.com/javase/7/docs/api/java/net/URL.html
      // http, https, ftp, file, and jar
      // 本文只需要处理file和jar
      if ("file".equals(protocol)) {
        String filePath = null;
        try {
          filePath = URLDecoder.decode(url.getFile(), "UTF-8"); // /
        } catch (UnsupportedEncodingException e) {
          if(log.isErrorEnabled()) {
            HashMap errorMap = new HashMap();
            errorMap.put(GRAPHQL_PACKAGENAME_FIELDNAME,packageName);
            log.error("{}", LogData.getErrorLog("E10029",errorMap,e));
          }
        }

        filePath = filePath.substring(1);
        getClassesWithAnnotationFromFilePath(packageName, filePath, classMap,classBinder);
      } else if ("jar".equals(protocol)) {
        JarFile jar = null;
        try {
          jar = ((JarURLConnection) url.openConnection()).getJarFile();
          // 扫描jar包文件 并添加到集合中
        } catch (Exception e) {
          if(log.isErrorEnabled()) {
            HashMap errorMap = new HashMap();
            errorMap.put(GRAPHQL_PACKAGENAME_FIELDNAME,packageName);
            log.error("{}", LogData.getErrorLog("E10029",errorMap,e));
          }
        }
        findClassesByJar(packageName, jar, classMap,classBinder);
      } else {
        if(log.isErrorEnabled()) {
          HashMap errorMap = new HashMap();
          errorMap.put(GRAPHQL_PROTOCOL_FIELDNAME,protocol);
          log.error("{}",LogData.getErrorLog("E10030",errorMap,new BusinessException("E10030")));
        }
      }
    }
  }

  private static void findClassesByJar(String pkgName, JarFile jar, HashMap classMap,ClassMapper classBinder) {
    String pkgDir = pkgName.replace(".", "/");
    Enumeration<JarEntry> entry = jar.entries();
    while (entry.hasMoreElements()) {
      // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文
      JarEntry jarEntry = entry.nextElement();
      String name = jarEntry.getName();
      // 如果是以/开头的
      if (name.charAt(0) == '/') {
        // 获取后面的字符串
        name = name.substring(1);
      }
      if (jarEntry.isDirectory() || !name.startsWith(pkgDir) || !name.endsWith(".class")) {
        continue;
      }
      // 如果是一个.class文件 而且不是目录
      // 去掉后面的".class" 获取真正的类名
      String className = name.substring(0, name.length() - 6);
      Class<?> tempClass = loadClass(className.replace("/", "."));
      // 添加到集合中去
      classBinder.doBind(classMap,tempClass);
    }
  }

  /**
   * 加载类
   *
   * @param fullClsName 类全名
   * @return
   */
  private static Class<?> loadClass(String fullClsName) {
    try {
      return Thread.currentThread().getContextClassLoader().loadClass(fullClsName);
    } catch (ClassNotFoundException e) {
      if(log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_CLASSNAME_FIELDNAME,fullClsName);
        log.error("{}",LogData.getErrorLog("E10029",errorMap,e));
      }
    }
    return null;
  }

  // filePath is like this
  private static  void getClassesWithAnnotationFromFilePath(
      String packageName,
      String filePath,
      HashMap classMap,ClassMapper classBinder) {
    Path dir = Paths.get(filePath);

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        String fileName =
            String.valueOf(path.getFileName());
        // 如果path是目录的话， 此处需要递归，
        boolean isDir = Files.isDirectory(path);
        if (isDir) {
          getClassesWithAnnotationFromFilePath(
              packageName + "." + fileName, path.toString(), classMap,classBinder);
        } else {
          String className = fileName.substring(0, fileName.length() - 6);

          Class<?> classes = null;
          String fullClassPath = packageName + "." + className;
          try {
            if(log.isInfoEnabled()) {
              HashMap infoMap = new HashMap();
              infoMap.put(GRAPHQL_CLASSNAME_FIELDNAME,fullClassPath);
              log.info("{}",LogData.getInfoLog("I10004",infoMap));
            }
            classes = Thread.currentThread().getContextClassLoader().loadClass(fullClassPath);
          } catch (ClassNotFoundException e) {
            if(log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_CLASSNAME_FIELDNAME,fullClassPath);
              log.error("{}",LogData.getErrorLog("E10031",errorMap,e));
            }
          }
          classBinder.doBind(classMap,classes);
        }
      }
    } catch (IOException e) {
      if(log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_PACKAGENAME_FIELDNAME,packageName);
        log.error("{}",LogData.getErrorLog("E10032",errorMap,e));
      }
    }
  }
}
