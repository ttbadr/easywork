package com.easywork.mapping.core.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果类
 * 包含验证是否通过的标志和错误信息列表
 */
public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors;

    public ValidationResult() {
        this.valid = true;
        this.errors = new ArrayList<>();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public void addError(ValidationError error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addError(String code, String message) {
        addError(new ValidationError(code, message));
    }
}