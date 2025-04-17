package com.easywork.mapping.core.api;

/**
 * 转换器接口
 * 将解析后的数据转换为目标格式
 */
public interface Converter {
    /**
     * 转换数据
     * @param input 输入数据对象
     * @return 转换后的数据对象
     */
    Object convert(Object input);
}