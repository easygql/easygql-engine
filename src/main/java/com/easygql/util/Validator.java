package com.easygql.util;

import lombok.NonNull;

import java.util.HashSet;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_ID_FIELDNAME;

/**
 * 验证各种字段有效性
 * @author guofen
 * @date 2019/11/3 17:05
 */
public class Validator {
    private static  final  String regexprStr = "[_A-Za-z][_0-9A-Za-z]*";
    private static  final HashSet typeForbiddenWords= new HashSet();
    private static final HashSet roleNameForbiddenWords=new HashSet();
    private static final HashSet fieldForbiddenWords=new HashSet();
    static {
        typeForbiddenWords.add("id");
        typeForbiddenWords.add("float");
        typeForbiddenWords.add("boolean");
        typeForbiddenWords.add("string");
        typeForbiddenWords.add("int");
        typeForbiddenWords.add("long");
        typeForbiddenWords.add("short");
        typeForbiddenWords.add("biginteger");
        typeForbiddenWords.add("bigdecimal");
        typeForbiddenWords.add("char");
        typeForbiddenWords.add("object");
        typeForbiddenWords.add("json");
        typeForbiddenWords.add("date");
        typeForbiddenWords.add("datetime");
        typeForbiddenWords.add("time");
        typeForbiddenWords.add("email");
        typeForbiddenWords.add("lastupdate");
        typeForbiddenWords.add("createdat");
        typeForbiddenWords.add("user");
        typeForbiddenWords.add("role");
        typeForbiddenWords.add("userinput");
        typeForbiddenWords.add("userwhereinput");
        typeForbiddenWords.add("userupdateinput");
        fieldForbiddenWords.add("_id");
        fieldForbiddenWords.add(GRAPHQL_ID_FIELDNAME);
        roleNameForbiddenWords.add("admin");
        roleNameForbiddenWords.add("guest");
    }

    static {

    }
    /**
     * 校验是否合法ID
     * @param idstr
     * @return
     */
    public static boolean isValidID(@NonNull  String idstr) {
        if(idstr.length() <=32) {
            return  true;
        } else {
            return false;
        }
    }

    /**
     * 验证是否可以作为类型名
     * @param typeName
     * @return
     */
    public static boolean isValidTypeName(@NonNull String typeName) {
        if(typeName.matches(regexprStr)&&!typeForbiddenWords.contains(typeName)) {
            return  true;
        } else {
            return false;
        }
    }

    /**
     * 验证是否可以作为字段名
     * @param fieldName
     * @return
     */
    public static boolean isValidFieldName(String fieldName) {
        if(fieldName.matches(regexprStr)&&!fieldForbiddenWords.contains(fieldName)) {
            return  true;
        } else {
            return false;
        }
    }

    /**
     * 验证是否可以作为角色名
     * @param roleName
     * @return
     */
    public static boolean isValidRoleName(String roleName) {
        if(roleName.matches(regexprStr)&&!roleNameForbiddenWords.contains(roleName)) {
            return  true;
        } else {
            return false;
        }
    }
}
