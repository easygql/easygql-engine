package com.easygql.graphql.datafetcher;

import com.easygql.util.ObjectTypeMetaData;
import com.easygql.util.PasswordUtils;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_OBJECTS_ARGUMENT;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_PASSWORD_FIELD;

public class DataFetcherUserCreate extends  DataFetcherCreate {
    private String slat  = null;
    private ObjectTypeMetaData userTypeMetaData ;
    /**
     * @param objectName
     * @param schemaData
     * @param schemaid
     */
    public DataFetcherUserCreate(@NonNull String objectName, @NonNull SchemaData schemaData, @NonNull String schemaid) {
        super(objectName, schemaData, schemaid);
        slat = schemaid;
        userTypeMetaData =    schemaData.getObjectMetaData().get(objectName);
}

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        Object argument = dataFetchingEnvironment.getArgument(GRAPHQL_OBJECTS_ARGUMENT);
        if (argument instanceof List) {
            List objList = (List) argument;
            for(Object obj:objList) {
                Map map = (Map)obj;
                String password = String.valueOf(map.get(GRAPHQL_PASSWORD_FIELD));
                String securePassword = PasswordUtils.generateSecurePassword(password,slat);
                map.put(GRAPHQL_PASSWORD_FIELD,securePassword);
            }
        } else {
            Map map = (Map) argument;
            String password = String.valueOf(map.get(GRAPHQL_PASSWORD_FIELD));
            String securePassword = PasswordUtils.generateSecurePassword(password,slat);
            map.put(GRAPHQL_PASSWORD_FIELD,securePassword);
        }
        return super.get(dataFetchingEnvironment);
    }
}
