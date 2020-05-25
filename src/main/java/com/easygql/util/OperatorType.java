package com.easygql.util;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import lombok.Getter;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */

public class OperatorType {
    @Getter
    private static GraphQLInputObjectType intFilterInput;
    @Getter
    private static  GraphQLInputObjectType floatFilterInput;
    @Getter
    private static  GraphQLInputObjectType shortFilterInput;
    @Getter
    private static GraphQLInputObjectType bigDecimalFilterInput;
    @Getter
    private static GraphQLInputObjectType longFilterInput;
    @Getter
    private static GraphQLInputObjectType stringFilterInput;
    @Getter
    private static GraphQLInputObjectType stringEqFilterInput;
    @Getter
    private static  GraphQLInputObjectType stringInFilterInput;
    @Getter
    private static GraphQLInputObjectType booleanFilterInput;
    @Getter
    private static GraphQLInputObjectType idFilterInput;
    @Getter
    private static GraphQLInputObjectType enumFilterInput;
    @Getter
    private static GraphQLInputObjectType intListWhereInput;
    @Getter
    private static GraphQLInputObjectType longListWhereInput;
    @Getter
    private static GraphQLInputObjectType shortListWhereInput;
    @Getter
    private static GraphQLInputObjectType byteListWhereInput;
    @Getter
    private static GraphQLInputObjectType bigDecimalListWhereInput;
    @Getter
    private static GraphQLInputObjectType stringListWhereInput;
    @Getter
    private static GraphQLInputObjectType IDListWhereInput;
    @Getter
    private static GraphQLInputObjectType FloatListWhereInput;
    @Getter
    private static GraphQLInputObjectType DoubleListWhereInput;
    public static void setup() {
        GraphQLInputObjectField.Builder intEqFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLInt);
        GraphQLInputObjectField.Builder intNeFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_NE_OPERATOR).type(Scalars.GraphQLInt);
        GraphQLInputObjectField.Builder intLTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LT_OPERATOR).type(Scalars.GraphQLInt);
        GraphQLInputObjectField.Builder intLEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LE_OPERATOR).type(Scalars.GraphQLInt);
        GraphQLInputObjectField.Builder intGTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GT_OPERATOR).type(Scalars.GraphQLInt);
        GraphQLInputObjectField.Builder intGEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(Scalars.GraphQLInt);
        intFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_INTFILTER_TYPENAME)
                .field(intEqFieldBuilder)
                .field(intNeFieldBuilder)
                .field(intLTFieldBuilder)
                .field(intLEFieldBuilder)
                .field(intGTFieldBuilder)
                .field(intGEFieldBuilder).build();
        GraphQLInputObjectField.Builder floatEqFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLFloat);
        GraphQLInputObjectField.Builder floatNEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_NE_OPERATOR).type(Scalars.GraphQLFloat);
        GraphQLInputObjectField.Builder floatLTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LT_OPERATOR).type(Scalars.GraphQLFloat);
        GraphQLInputObjectField.Builder floatLEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LE_OPERATOR).type(Scalars.GraphQLFloat);
        GraphQLInputObjectField.Builder floatGTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GT_OPERATOR).type(Scalars.GraphQLFloat);
        GraphQLInputObjectField.Builder floatGEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(Scalars.GraphQLFloat);
        floatFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_FLOATFILTER_TYPENAME)
                .field(floatEqFieldBuilder)
                .field(floatNEFieldBuilder)
                .field(floatLTFieldBuilder)
                .field(floatLEFieldBuilder)
                .field(floatGTFieldBuilder)
                .field(floatGEFieldBuilder)
                .build();
        GraphQLInputObjectField.Builder shortEQFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLShort);
        GraphQLInputObjectField.Builder shortNEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_NE_OPERATOR).type(Scalars.GraphQLShort);
        GraphQLInputObjectField.Builder shortLTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LT_OPERATOR).type(Scalars.GraphQLShort);
        GraphQLInputObjectField.Builder shortLEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LE_OPERATOR).type(Scalars.GraphQLShort);
        GraphQLInputObjectField.Builder shortGTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GT_OPERATOR).type(Scalars.GraphQLShort);
        GraphQLInputObjectField.Builder shortGEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(Scalars.GraphQLShort);

        shortFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_SHORTFILTER_TYPENAME)
                .field(shortEQFieldBuilder)
                .field(shortNEFieldBuilder)
                .field(shortLTFieldBuilder)
                .field(shortLEFieldBuilder)
                .field(shortGTFieldBuilder)
                .field(shortGEFieldBuilder)
                .build();
        stringEqFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_FILTER_EQ_OPERATOR).field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLString)).build();

        //LongFilter
        GraphQLInputObjectField.Builder longEQFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLLong);
        GraphQLInputObjectField.Builder longNEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_NE_OPERATOR).type(Scalars.GraphQLLong);
        GraphQLInputObjectField.Builder longLTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LT_OPERATOR).type(Scalars.GraphQLLong);
        GraphQLInputObjectField.Builder longLEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LE_OPERATOR).type(Scalars.GraphQLLong);
        GraphQLInputObjectField.Builder longGTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GT_OPERATOR).type(Scalars.GraphQLLong);
        GraphQLInputObjectField.Builder longGEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(Scalars.GraphQLLong);
        longFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_LongFILTER_TYPENAME)
                .field(longEQFieldBuilder)
                .field(longNEFieldBuilder)
                .field(longLTFieldBuilder)
                .field(longLEFieldBuilder)
                .field(longGTFieldBuilder)
                .field(longGEFieldBuilder)
                .build();
        //BigDecimal Filter
        GraphQLInputObjectField.Builder bigDecimalEQFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLBigDecimal);
        GraphQLInputObjectField.Builder bigDecimalNEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_NE_OPERATOR).type(Scalars.GraphQLBigDecimal);
        GraphQLInputObjectField.Builder bigDecimalLTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LT_OPERATOR).type(Scalars.GraphQLBigDecimal);
        GraphQLInputObjectField.Builder bigDecimalLEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LE_OPERATOR).type(Scalars.GraphQLBigDecimal);
        GraphQLInputObjectField.Builder bigDecimalGTFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GT_OPERATOR).type(Scalars.GraphQLBigDecimal);
        GraphQLInputObjectField.Builder bigDecimalGEFieldBuilder = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(Scalars.GraphQLBigDecimal);
        bigDecimalFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_BigDecimalFILTER_TYPENAME)
                .field(bigDecimalEQFieldBuilder)
                .field(bigDecimalNEFieldBuilder)
                .field(bigDecimalLTFieldBuilder)
                .field(bigDecimalLEFieldBuilder)
                .field(bigDecimalGTFieldBuilder)
                .field(bigDecimalGEFieldBuilder)
                .build();
        //String Filter
        stringFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_STRINGFILTER_TYPENAME)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GT_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LT_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LE_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_MATCH_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_IN_OPERATOR).type(GraphQLList.list(Scalars.GraphQLString)).build())
                .build();
        //Boolean Filter
        GraphQLInputObjectField.Builder isTrueField = GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_ISTRUE_OPERATOR).type(Scalars.GraphQLBoolean);
        booleanFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_BOOLEANFILTER_TYPENAME)
                .field(isTrueField)
                .build();
        idFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_IDFILTER_TYPENAME)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GT_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LT_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_LE_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_MATCH_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_IN_OPERATOR).type(GraphQLList.list(Scalars.GraphQLString)).build())
                .build();
        enumFilterInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_ENUMFILTER_TYPENAME)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_EQ_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_NE_OPERATOR).type(Scalars.GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_GE_OPERATOR).type(new GraphQLList(Scalars.GraphQLString)).build())
                .build();
        intListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_INTLISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLInt))
                .build();
        longListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_LONGLISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLLong))
                .build();
        shortListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_SHORTLISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLShort))
                .build();
        byteListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_BYTELISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLByte))
                .build();
        bigDecimalListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_BIGDECIMALLISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLBigDecimal))
                .build();
        stringListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_StringLISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLString))
                .build();
        IDListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_StringLISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLID))
                .build();
        FloatListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_FLOATLISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLFloat))
                .build();
        DoubleListWhereInput = GraphQLInputObjectType.newInputObject().name(GRAPHQL_DOUBLELISTWHEREINPUT)
                .field(GraphQLInputObjectField.newInputObjectField().name(GRAPHQL_FILTER_HASONE_OPERATOR).type(Scalars.GraphQLFloat))
                .build();

    }
}
