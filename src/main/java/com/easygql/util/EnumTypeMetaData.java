package com.easygql.util;

import lombok.Data;

import java.util.List;

/**
 * @author guofen
 * @date 2019/11/19 13:55
 */
@Data
public class EnumTypeMetaData {
    private String id;
    private List<EnumElement> values;
    private String name;

}
