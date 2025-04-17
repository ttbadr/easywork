package com.easywork.mapping.core.api;

/**
 * 后处理器接口
 * 对生成的输出进行后处理
 */
public interface PostProcessor {
    /**
     * 处理输出数据
     * @param output 输出数据
     * @return 处理后的输出数据
     */
    String process(String output);
}