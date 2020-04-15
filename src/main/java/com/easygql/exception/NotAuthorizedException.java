package com.easygql.exception;

import graphql.GraphQLException;
/**
 * 未授权异常
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class NotAuthorizedException extends GraphQLException  {
    public static NotAuthorizedException notAuthorizedException= new NotAuthorizedException();
    public NotAuthorizedException(){
        super("Not Authorized!");
    }
}
