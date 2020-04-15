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
    private boolean islist;
    private boolean notnull;
    private List<String> invisible=new ArrayList<>();
    private List<String> irrevisible =new ArrayList<>();
    private String description;
    private Object defaultvalue;
}
