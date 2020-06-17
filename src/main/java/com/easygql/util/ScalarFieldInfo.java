package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/23 12:07
 */
@Data
public class ScalarFieldInfo {
    private String id;
    private String name;
    private boolean notNull;
    private String fieldKind;//UserDefined，SystemDefined
    private boolean isList;
    private String type;
    private List<String> invisibleRoles =new ArrayList<>();
    private List<String> unmodifiableRoles =new ArrayList<>();
    private String description;
    private String alias;
    private Object defaultValue=null;
}
