package com.easygql.exception;

import com.easygql.util.LogData;
import lombok.extern.slf4j.Slf4j;
/**
 * 业务异常
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Slf4j
public class BusinessException  extends  RuntimeException {
    private String errorCode;
    public BusinessException(String errorCode) {
        super(LogData.getErrorMessage(errorCode));
        this.errorCode =errorCode;

    }
}
