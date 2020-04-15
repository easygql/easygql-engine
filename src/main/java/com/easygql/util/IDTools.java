package com.easygql.util;

import org.bson.types.ObjectId;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class IDTools {
    public static String getID(){
        ObjectId idStr = new ObjectId();
        return idStr.toHexString();
    }
}
