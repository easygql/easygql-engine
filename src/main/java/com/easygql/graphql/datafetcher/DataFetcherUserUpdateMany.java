package com.easygql.graphql.datafetcher;

import com.easygql.util.SchemaData;
import lombok.NonNull;

public class DataFetcherUserUpdateMany extends DataFetcherUpdateMany {
    /**
     * @param objectName
     * @param schemaData
     * @param schemaID
     * @author guofen
     * @date 2019-10-27 16:41
     */
    public DataFetcherUserUpdateMany(@NonNull String objectName, @NonNull SchemaData schemaData, @NonNull String schemaID) {
        super(objectName, schemaData, schemaID);
    }
}
