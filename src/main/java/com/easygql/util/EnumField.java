package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/29 15:45
 */
@Data
public class EnumField {
    private String id;
    private String name;
    private String type;
    private boolean isList;
    private boolean notNull;
    private List<String> invisibleRoles =new ArrayList<>();
    private List<String> unmodifiableRoles =new ArrayList<>();
    private String description;
    private Object defaultValue;
    private String alias;

}
