package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/23 16:16
 */
@Data
public class UniqueConstraint {
    private List<String> fieldNames=new ArrayList<>();
    private String constraintName;
}
