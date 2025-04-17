package com.easywork.mapping.core.api;

/**
 * 预处理器接口
 * 在解析之前对输入进行预处理
 */
public interface PreProcessor {
    /**
     * 处理输入数据
     * @param input 输入数据
     * @return 处理后的数据
     */
    String process(String input);
}