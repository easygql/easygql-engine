package com.easygql.util;


import com.easygql.component.ConfigurationProperties;
import graphql.language.StringValue;
import graphql.scalars.util.Kit;
import graphql.schema.*;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class CreatedAtScalar extends GraphQLScalarType {
    public CreatedAtScalar() {
        super("CreatedAt", "An RFC-3339 compliant timestamp created with the object ", new Coercing<OffsetDateTime, String>() {
            @Override
            public String serialize(Object input) throws CoercingSerializeException {
                OffsetDateTime offsetDateTime;
                if (input instanceof OffsetDateTime) {
                    offsetDateTime = (OffsetDateTime)input;
                } else if (input instanceof ZonedDateTime) {
                    offsetDateTime = ((ZonedDateTime)input).toOffsetDateTime();
                } else {
                    if (!(input instanceof String)) {
                        throw new CoercingSerializeException("Expected something we can convert to 'java.time.OffsetDateTime' but was '" + Kit.typeName(input) + "'.");
                    }

                    offsetDateTime = this.parseOffsetDateTime(input.toString(), CoercingSerializeException::new);
                }

                try {
                    return ConfigurationProperties.getInstance().DEFAULT_DATETIME_FORMAT.format(offsetDateTime);
                } catch (DateTimeException var4) {
                    throw new CoercingSerializeException("Unable to turn TemporalAccessor into OffsetDateTime because of : '" + var4.getMessage() + "'.");
                }
            }

            @Override
            public OffsetDateTime parseValue(Object input) throws CoercingParseValueException {
                OffsetDateTime offsetDateTime;
                if (input instanceof OffsetDateTime) {
                    offsetDateTime = (OffsetDateTime)input;
                } else if (input instanceof ZonedDateTime) {
                    offsetDateTime = ((ZonedDateTime)input).toOffsetDateTime();
                } else {
                    if (!(input instanceof String)) {
                        throw new CoercingParseValueException("Expected a 'String' but was '" + Kit.typeName(input) + "'.");
                    }
                    offsetDateTime = this.parseOffsetDateTime(input.toString(), CoercingParseValueException::new);
                }

                return offsetDateTime;
            }

            @Override
            public OffsetDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                if (!(input instanceof StringValue)) {
                    throw new CoercingParseLiteralException("Expected AST type 'StringValue' but was '" + Kit.typeName(input) + "'.");
                } else {
                    return this.parseOffsetDateTime(((StringValue)input).getValue(), CoercingParseLiteralException::new);
                }
            }

            private OffsetDateTime parseOffsetDateTime(String s, Function<String, RuntimeException> exceptionMaker) {
                try {
                    return OffsetDateTime.parse(s, ConfigurationProperties.getInstance().DEFAULT_DATETIME_FORMAT);
                } catch (DateTimeParseException var4) {
                    throw (RuntimeException)exceptionMaker.apply("Invalid RFC3339 value : '" + s + "'. because of : '" + var4.getMessage() + "'");
                }
            }
        });
    }
    public static final  CreatedAtScalar createdatscalar = new CreatedAtScalar();
}
