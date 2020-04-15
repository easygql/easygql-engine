package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/7 15:51
 */
@Data
public class APIMetaData {
    private String apiname;
    private String apikind;//insert,update,query,delete,subscription,thirdapi
    private String objectname;
    private List<String> disabled_roles=new ArrayList<>();
}
