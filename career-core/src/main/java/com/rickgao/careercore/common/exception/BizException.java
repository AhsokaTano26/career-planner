package com.rickgao.careercore.common.exception;

import com.rickgao.careercore.common.response.ResultCode;
import lombok.Getter;

/**
 * 业务异常:携带业务码,由全局异常处理器转换为统一响应。
 */
@Getter
public class BizException extends RuntimeException {

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
