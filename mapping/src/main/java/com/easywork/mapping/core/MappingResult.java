package com.easywork.mapping.core;

import java.util.ArrayList;
import java.util.List;

import com.easywork.mapping.core.support.ValidationError;

/**
 * 映射结果类
 * 包含处理是否成功的标志、输出结果和错误信息
 */
public class MappingResult {
    private boolean success;
    private String output;
    private List<ValidationError> errors;

    private MappingResult(boolean success, String output, List<ValidationError> errors) {
        this.success = success;
        this.output = output;
        this.errors = errors;
    }

    public static MappingResult success(String output) {
        return new MappingResult(true, output, new ArrayList<>());
    }

    public static MappingResult error(List<ValidationError> errors) {
        return new MappingResult(false, null, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}