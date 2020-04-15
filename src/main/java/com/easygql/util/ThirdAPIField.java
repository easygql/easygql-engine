package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/23 12:07
 */
@Data
public class ThirdAPIField {
    private String id;
    private String name;
    private boolean notnull;
    private String kind;//UserDefined，SystemDefined
    private boolean  islist ;
    private String type;
    private String description;
    private String defaultValue=null;
}
