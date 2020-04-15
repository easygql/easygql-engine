package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/21 13:01
 */
@Data
public class ThirdPartAPIMetaData {
    private  String id;
    private String apiname;
    private List<String> disabled_roles = new ArrayList<>();
}
