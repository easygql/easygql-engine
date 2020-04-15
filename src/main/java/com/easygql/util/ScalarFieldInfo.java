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
    private boolean notnull;
    private String fieldkind;//UserDefined，SystemDefined
    private boolean  islist ;
    private String type;
    private List<String> invisible_roles=new ArrayList<>();
    private List<String> irrevisible_roles =new ArrayList<>();
    private String description;
    private String defaultValue=null;
}
