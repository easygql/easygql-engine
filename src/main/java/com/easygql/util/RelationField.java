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
    private String relationtype;
    private List<String> invisible=new ArrayList<>();
    private List<String> irrevisible =new ArrayList<>();
    private String description;
    private String fromobject;
    private String fromfield;
    private String toobject;
    private String tofield;
    private Boolean ifcascade=false;
}
