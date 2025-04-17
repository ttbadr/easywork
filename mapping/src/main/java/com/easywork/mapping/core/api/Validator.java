package com.easywork.mapping.core.api;

import com.easywork.mapping.core.support.ValidationResult;

/**
 * 验证器接口
 * 验证输入数据的格式和内容是否符合要求
 */
public interface Validator {
    /**
     * 验证输入数据
     * @param input 输入数据
     * @return 验证结果
     */
    ValidationResult validate(String input);
}