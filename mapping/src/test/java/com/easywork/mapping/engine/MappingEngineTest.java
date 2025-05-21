package com.easywork.mapping.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.easywork.mapping.config.MapperConfig;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class MappingEngineTest {
    private MappingEngine engine;
    private Path configPath;

    @BeforeEach
    void setUp() throws Exception {
        engine = MappingEngine.getInstance();
        
        // 将测试配置文件从资源目录复制到临时文件
        configPath = Files.createTempFile("test-mapping", ".yaml");
        try (InputStream is = getClass().getResourceAsStream("/test-mapping-config.yaml")) {
            Files.copy(is, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    void testSingleton() {
        MappingEngine anotherEngine = MappingEngine.getInstance();
        assertSame(engine, anotherEngine, "应该返回相同的实例");
    }

    @Test
    void testLoadConfig() throws Exception {
        MapperConfig config = engine.loadConfig(configPath.toString());
        
        // 验证配置内容
        assertNotNull(config);
        assertEquals("json", config.getSource().getFormat());
        assertEquals("xml", config.getTarget().getFormat());
        assertEquals("jsonSchema", config.getSource().getValidator().getType());
        assertEquals("xsd", config.getTarget().getValidator().getType());
    }

    @Test
    void testTransform() throws Exception {
        String inputJson = "{\"name\": \"测试\"}";
        MapperConfig config = engine.loadConfig(configPath.toString());
        
        String result = engine.transform(inputJson, config);
        assertNotNull(result);
        // TODO: 添加更多具体的转换结果验证
    }
    
    @AfterEach
    void tearDown() throws Exception {
        // 清理临时文件
        if (configPath != null) {
            Files.deleteIfExists(configPath);
        }
    }
} 