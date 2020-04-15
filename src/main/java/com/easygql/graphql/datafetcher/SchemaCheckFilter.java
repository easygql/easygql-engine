package com.easygql.graphql.datafetcher;

import com.easygql.component.SchemaService;
import com.easygql.util.AuthorityUtil;
import com.easygql.util.TypeConstraint;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.HashSet;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_FILTER_FILTER_OPERATOR;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class SchemaCheckFilter implements  EasyGQLDataFetcher<Object> {
    protected final HashSet<String> disabledrole;
    protected final HashMap<String, TypeConstraint> apiconstraints;

    /**
     *
     * @param disabledrole
     * @param apiconstraints
     */
    public SchemaCheckFilter(HashSet<String> disabledrole, HashMap<String, TypeConstraint> apiconstraints) {
        this.disabledrole = disabledrole;
        this.apiconstraints = apiconstraints;
    }

    /**
     *
     * @param dataFetchingEnvironment
     * @return
     */
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        AuthorityUtil.authorityVerify(dataFetchingEnvironment,disabledrole);
        String schemaid = dataFetchingEnvironment.getArgument("schemaid");
        String typename = dataFetchingEnvironment.getArgument("typename");
        HashMap filtermap = dataFetchingEnvironment.getArgument(GRAPHQL_FILTER_FILTER_OPERATOR);
        return SchemaService.schemaService.FilterCheck(schemaid,typename,filtermap);
    }
}
