package com.easygql.util;

import com.easygql.dao.*;
import lombok.Data;

import java.util.HashMap;

/**
 * @author guofen
 * @date 2019/12/17 11:42
 */
@Data
public class ObjectDao {
    private DataDeleter datadeleter;
    private DataInserter datainserter;
    private DataSelecter dataselecter;
    private DataUpdater dataupdater;
    private DataSub datasub;
    private HashMap<String, RelationObjectCreator> relation_add_Fields;
    private HashMap<String, RelationRemover> relation_remove_Fields;
    private TriggerSub triggerSub;
}
