package com.easygql.util;

import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;

import java.util.HashMap;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class GraphQLUtil {
    public static QueryType getQuerytype(String query) {
        String[] arr = query.trim().split("[^a-zA-Z0-9]+");
        if (arr.length > 1) {
            if (arr[0].equalsIgnoreCase("query")) {
                return QueryType.QUERY;
            } else if (arr[0].equalsIgnoreCase("mutation")) {
                return QueryType.MUTATION;
            } else if (arr[0].equalsIgnoreCase("subscription")) {
                return QueryType.SUBSCRIPTION;
            }
        }
        return QueryType.NOTQUERY;
    }
    /**
     * 构建结果集Map
     *
     * @param selectionSet
     * @return
     */
    public static HashMap<String, Object> constructSelectionHashMap(DataFetchingFieldSelectionSet selectionSet) {
        if (null == selectionSet) {
            return null;
        }
        HashMap selectionFields = new HashMap();
        for (SelectedField selectedField : selectionSet.getFields()) {
            if(selectedField.getName().equals(selectedField.getQualifiedName())) {
                if (selectedField.getSelectionSet().getFields().size() != 0) {
                    selectionFields.put(selectedField.getName(), constructSelectionHashMap(selectedField.getSelectionSet()));
                } else {
                        selectionFields.put(selectedField.getName(), 1);
                }
            }
        }
        return selectionFields;
    }
}
