package com.easygql.util;

import com.easygql.exception.BusinessException;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.util.Validator.isValidID;

/**
 * @author guofen
 * @date 2019/12/29 13:48
 */
public class EasyGqlUtil {
    /**
     * 获取LastUpdate字段列表
     *
     * @param objectTypeMetaData
     * @return
     */
    public static List<String> getLastUpdateFields(ObjectTypeMetaData objectTypeMetaData) {
        List<String> strList = new ArrayList();
        for (ScalarFieldInfo scalarFieldInfo : objectTypeMetaData.getScalarFieldData().values()) {
            if (scalarFieldInfo.getType().equals(GRAPHQL_LASTUPDATE_TYPENAME)) {
                strList.add(scalarFieldInfo.getName());
            }
        }
        return strList;
    }

    /**
     * 获取CreateAt字段列表
     *
     * @param objectTypeMetaData
     * @return
     */
    public static List<String> getCreateAtFields(ObjectTypeMetaData objectTypeMetaData) {
        List<String> strList = new ArrayList();
        for (ScalarFieldInfo scalarFieldInfo : objectTypeMetaData.getScalarFieldData().values()) {
            if (scalarFieldInfo.getType().equals(GRAPHQL_CREATEDAT_TYPENAME)) {
                strList.add(scalarFieldInfo.getName());
            }
        }
        return strList;
    }

    /**
     * 为插入对象赋ID值，以及对lastupdate类型的字段进行赋值
     *
     * @param doc
     * @param result
     */
    public static void transferIdAndLastUpdate(@NonNull Object doc, @NonNull Map<String, HashMap> result, @NonNull ObjectTypeMetaData objectTypeMetaData) {
        List<String> lastUpdateFields = getLastUpdateFields(objectTypeMetaData);
        List<String> createAtFields = getCreateAtFields(objectTypeMetaData);
        transferIdAndLastUpdate(doc, result, lastUpdateFields, createAtFields);
    }

    /**
     * 为插入对象赋ID值，以及对lastupdate类型的字段进行赋值
     *
     * @param doc
     * @param result
     */
    public static void transferIdAndLastUpdate(@NonNull Object doc, @NonNull Map<String, HashMap> result, @NonNull List<String> lastUpdateFields, @NonNull List<String> createAtFields) {
        String nowTime = getNowTimeStamp();
        if (doc instanceof List) {
            List<HashMap> objList = (List) doc;
            Map resultTmp = objList.stream().map(obj -> {
                HashMap resultObj = transferId(obj);
                for (String lastUpdateField : lastUpdateFields) {
                    resultObj.put(lastUpdateField, nowTime);
                }
                for (String createAtField : createAtFields) {
                    resultObj.put(createAtField, nowTime);
                }
                return resultObj;
            }).collect(Collectors.toMap(objMap -> objMap.get(GRAPHQL_ID_FIELDNAME), objMap -> objMap));
            result.putAll(resultTmp);
        } else {
            HashMap resultObj = transferId((HashMap) doc);
            for (String lastUpdateField : lastUpdateFields) {
                resultObj.put(lastUpdateField, nowTime);
            }
            for (String createAtField : createAtFields) {
                resultObj.put(createAtField, nowTime);
            }
            result.put(String.class.cast(resultObj.get(GRAPHQL_ID_FIELDNAME)), resultObj);
        }
    }

    /**
     * 为插入对象赋ID
     *
     * @param objMap
     * @return
     */
    public static HashMap transferId(HashMap objMap) {
        objMap.compute(GRAPHQL_ID_FIELDNAME, (k, v) -> {
            if (null != v && !isValidID((String) v)) {
                throw new BusinessException("E10053");
            }
            if (null == v) {
                return IDTools.getID();
            } else {
                return v;
            }
        });
        return objMap;
    }

    public static String getNowTimeStamp() {
        return LocalDateTime.now().format(getInstance().DEFAULT_DATETIME_FORMAT);
    }

}
