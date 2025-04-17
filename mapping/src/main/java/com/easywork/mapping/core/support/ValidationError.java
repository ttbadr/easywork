package com.easywork.mapping.core.support;

/**
 * 验证错误类
 * 包含错误代码和错误消息
 */
public class ValidationError {
    private String code;
    private String message;

    public ValidationError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}