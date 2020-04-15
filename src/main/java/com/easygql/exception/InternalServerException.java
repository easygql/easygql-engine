package com.easygql.exception;

import graphql.GraphQLException;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class InternalServerException extends GraphQLException {
    public static InternalServerException instance = new InternalServerException();
    public InternalServerException(){
        super("Internal Server Error!");
    }
}
