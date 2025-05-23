# Mapping 模块设计文档

## 1. 概述

本模块旨在实现不同数据格式之间的相互转换，包括XML、JSON、定长格式(Fixed Length)、ISO8583和ISO20022等金融报文格式。通过配置驱动的方式，实现灵活的格式转换，无需修改代码即可适应新的转换需求。

## 2. 架构设计

### 2.1 整体架构

采用适配器模式和策略模式相结合的架构，主要包含以下核心组件：

1. **格式验证器(Format Validator)**：负责验证输入和输出数据是否符合对应格式的规范
2. **预处理器(Pre-Processor)**：在格式解析前对输入数据进行预处理，支持自定义处理逻辑
3. **格式解析器(Format Parser)**：负责将各种格式的输入数据解析成统一的中间数据模型
4. **中间数据模型(Intermediate Data Model)**：所有格式转换的枢纽，采用简单的键值对结构表示数据
5. **映射引擎(Mapping Engine)**：根据配置的映射规则，将源格式的中间模型转换为目标格式的中间模型
6. **转换规则配置(Mapping Configuration)**：定义字段映射关系、转换规则、验证规则、预处理器和后处理器等
7. **格式生成器(Format Generator)**：将中间数据模型转换为目标格式的输出
8. **后处理器(Post-Processor)**：在格式生成后对输出数据进行后处理，支持自定义处理逻辑

### 2.2 组件关系图

```
输入数据 → 格式验证器 → 预处理器 → 格式解析器 → 中间数据模型 → 映射引擎 → 中间数据模型 → 格式生成器 → 后处理器 → 格式验证器 → 输出数据
                                                              ↑
                                                              |
                                                         转换规则配置
```

## 3. 核心组件详细设计

### 3.1 格式验证器(Format Validator)

为每种支持的格式提供专门的验证器实现：

- **XMLValidator**: 使用XSD Schema验证XML格式
- **JSONValidator**: 使用JSON Schema验证JSON格式
- **FixedLengthValidator**: 根据配置的字段长度、类型和位置规则进行验证
- **ISO8583Validator**: 根据ISO8583标准定义的字段规范和格式规则进行验证
- **ISO20022Validator**: 验证ISO20022 XML Schema

```java
public interface FormatValidator {
    // 使用 MapperConfig 统一管理配置
    ValidationResult validate(String input, MapperConfig config);
}

public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors;
    // getters and setters
}
```

### 3.2 格式解析器(Format Parser)

为每种支持的格式提供专门的解析器实现：

- **XMLParser**: 解析XML格式，支持XPath
- **JSONParser**: 解析JSON格式，支持JSONPath
- **FixedLengthParser**: 解析定长格式，根据配置的字段长度和位置
- **ISO8583Parser**: 解析ISO8583金融报文
- **ISO20022Parser**: 解析ISO20022 XML格式报文

### 3.2 中间数据模型(Intermediate Data Model)

采用Map结构表示数据，主要特点：

- **键值对结构**: 使用Map<String, Object>存储数据，键为字段路径，值为实际数据对象
- **灵活的值类型**: 
  - 值类型使用Object而不是byte[]，支持直接存储Java对象
  - 无需频繁的序列化/反序列化操作
  - 支持复杂数据结构（如嵌套对象、集合等）
  - 便于数据验证和类型转换
- **类型支持**: 支持字符串、数值、布尔、日期时间等基本类型及其包装类
- **路径表达式**: 根据输入输出格式使用对应的原生路径表达式(如XPath、JSONPath等)
- **简单高效**: 
  - 统一的数据访问接口
  - 内存友好的数据处理
  - 便于调试和数据检查

### 3.3 映射引擎(Mapping Engine)

- **字段映射**: 源格式字段到目标格式字段的映射
- **值转换**: 数据类型转换、格式转换、编码转换等
- **条件映射**: 基于条件的映射规则
- **默认值处理**: 当源数据不存在时的默认值设置

### 3.4 转换规则配置(Mapping Configuration)

使用YAML作为主要配置格式，并由统一的`MapperConfig`对象进行绑定：

```yaml
# 统一的 MapperConfig 配置
preProcessor: com.example.MyPreProcessor # 可选的预处理器类名
postProcessor: com.example.MyPostProcessor # 可选的后处理器类名

source:
  format: json
  validator:
    type: jsonSchema  # 明确指定验证器类型为 schema
    config:
      # schema 验证器需要的参数
      schemaPath: "customer-schema.json"

target:
  format: xml
  validator:
    type: xsd  # 明确指定验证器类型为 xsd
    config:
      # schema 验证器需要的参数
      schemaPath: "transaction-schema.xsd"

# --- 其他格式验证器配置示例 --- 
# 例: 定长格式 (如果 target 是 fixedLength):
# target:
#   format: fixedLength
#   validator:
#     type: rules # 明确指定验证器类型为 rules
#     config:
#       # rules 验证器需要的参数
#       rules:
#         - { field: 'field1', length: 10, type: 'AN' }
#         - { field: 'field2', length: 5, type: 'N' }

# 例: ISO8583 (如果 target 是 iso8583):
# target:
#   format: iso8583
#   validator:
#     type: spec # 明确指定验证器类型为 spec
#     config:
#       # spec 验证器需要的参数
#       specPath: "iso8583-spec.xml" # 指向 j8583 配置文件

rules:
  - source: $.customer.name        # JSONPath表达式
    target: /Document/Customer/Name  # XPath表达式
  - source: $.amount
    target: /Document/Transaction/Amount
    transform:
      type: multiply
      factor: 100
  - source: $.transactionDate
    target: /Document/Transaction/Date
    transform:
      type: dateFormat
      sourceFormat: "yyyy-MM-dd"
      targetFormat: "dd/MM/yyyy"
  - target: /Document/Transaction/Currency
    value: "CNY"    # 默认值设置
```

### 3.5 格式生成器(Format Generator)

为每种支持的格式提供专门的生成器实现：

- **XMLGenerator**: 生成XML格式输出
- **JSONGenerator**: 生成JSON格式输出
- **FixedLengthGenerator**: 生成定长格式输出
- **ISO8583Generator**: 生成ISO8583金融报文
- **ISO20022Generator**: 生成ISO20022 XML格式报文

## 4. 扩展性设计

### 4.1 扩展接口

#### 4.1.1 预处理器和后处理器

通过实现PreProcessor和PostProcessor接口，可以添加自定义的数据处理逻辑：

```java
public interface PreProcessor {
    // 使用 MapperConfig 统一管理配置
    String process(String input, MapperConfig config);
}

public interface PostProcessor {
    // 使用 MapperConfig 统一管理配置
    String process(String output, MapperConfig config);
}
```

#### 4.1.2 新格式支持

通过实现Parser和Generator接口，可以轻松添加新的格式支持：

```java
public interface FormatParser {
    // 使用 MapperConfig 统一管理配置
    Map<String, Object> parse(String input, MapperConfig config);
}

public interface FormatGenerator {
    // 使用 MapperConfig 统一管理配置
    String generate(Map<String, Object> data, MapperConfig config);
}
```

### 4.2 自定义转换器

支持自定义转换逻辑：

```java
public interface ValueTransformer {
    Object transform(Object value, Map<String, Object> parameters);
}
```

## 5. 可行性分析

### 5.1 技术可行性

- **XML处理**: 使用JAXB和DOM/SAX解析器
- **JSON处理**: 使用Jackson库
- **ISO8583处理**: 使用j8583库
- **ISO20022处理**: 使用Prowide ISO20022库
- **配置处理**: 使用Apache Commons Configuration

### 5.2 性能考虑

- 对于大型文档，采用流式处理减少内存占用
- 使用缓存优化频繁使用的配置和转换规则
- 支持并行处理提高吞吐量

### 5.3 安全考虑

- 输入验证防止注入攻击
- 敏感数据处理(如加密、掩码)
- 错误处理不泄露敏感信息

## 6. 使用示例

```java
// 创建映射引擎
MappingEngine engine = new MappingEngine();

// 加载统一配置
MapperConfig config = MapperConfigLoader.load("mapping-config.yaml"); // 假设加载方法和文件名已更新

// 读取输入字符串 (示例)
String inputJson = readFileToString("input.json"); // 假设有此辅助方法

// 执行转换
String outputXml = engine.transform(inputJson, config);

// 写入输出字符串 (示例)
writeStringToFile("output.xml", outputXml); // 假设有此辅助方法
```

## 7. 后续发展

- 图形化配置工具
- 更多格式支持(CSV, YAML, Protobuf等)
- 实时转换监控和性能指标
- 分布式处理支持