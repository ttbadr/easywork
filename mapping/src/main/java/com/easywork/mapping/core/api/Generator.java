package com.easywork.mapping.core.api;

/**
 * 生成器接口
 * 将转换后的数据生成为最终输出格式
 */
public interface Generator {
    /**
     * 生成输出
     * @param input 输入数据对象
     * @return 生成的输出字符串
     */
    String generate(Object input);
}