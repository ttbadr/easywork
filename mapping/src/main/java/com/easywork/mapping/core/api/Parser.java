package com.easywork.mapping.core.api;

/**
 * 解析器接口
 * 将输入字符串解析为内部数据结构
 */
public interface Parser {
    /**
     * 解析输入数据
     * @param input 输入数据
     * @return 解析后的数据对象
     */
    Object parse(String input);
}