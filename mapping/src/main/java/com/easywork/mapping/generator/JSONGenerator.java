package com.easywork.mapping.generator;

import com.easywork.mapping.config.MapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

public class JSONGenerator implements FormatGenerator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(Map<String, Object> data, MapperConfig config) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();
        
        // 遍历所有映射规则
        for (MapperConfig.MappingRule rule : config.getRules()) {
            String targetPath = rule.getTarget(); // 这是JSONPath表达式
            Object value = data.get(targetPath);
            
            if (value != null) {
                // 解析JSONPath，构建对应的JSON结构
                String[] pathParts = targetPath.split("\\.");
                ObjectNode currentNode = rootNode;
                
                // 处理嵌套路径
                for (int i = 0; i < pathParts.length - 1; i++) {
                    String part = pathParts[i];
                    if (!currentNode.has(part)) {
                        currentNode.putObject(part);
                    }
                    currentNode = (ObjectNode) currentNode.get(part);
                }
                
                // 设置最终值
                String lastPart = pathParts[pathParts.length - 1];
                currentNode.put(lastPart, value.toString());
            }
        }
        
        return objectMapper.writeValueAsString(rootNode);
    }
}