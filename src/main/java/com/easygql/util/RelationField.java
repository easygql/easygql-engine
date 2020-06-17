package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/23 16:04
 */
@Data
public class RelationField {
    private String id;
    private String relationType;
    private List<String> invisibleRoles =new ArrayList<>();
    private List<String> unmodifiableRoles =new ArrayList<>();
    private String description;
    private String fromObject;
    private String fromField;
    private String fromAlias;
    private String toObject;
    private String toField;
    private String toAlias;
    private Boolean ifCascade =false;
}
