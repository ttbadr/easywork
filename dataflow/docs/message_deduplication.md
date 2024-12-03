# Dataflow 消息去重机制

## 概述
在 Pub/Sub 到 AlloyDB 的数据管道中，消息去重是确保数据一致性的关键机制。本文档详细说明了 Dataflow 中的消息去重实现方式。

## 去重机制

### 1. 系统默认去重
如果不指定任何去重配置，Dataflow 会使用 Pub/Sub 消息的原生 `messageId` 进行去重：
```java
pipeline.apply(PubsubIO.readStrings()
    .fromSubscription(subscription));
```
- Pub/Sub 为每条消息自动生成唯一的 `messageId`
- 同一条消息重复投递时 `messageId` 保持不变
- 适用于只需要确保消息不重复处理的场景

### 2. 自定义去重标识
可以通过 `withIdAttribute` 指定自定义的去重标识：
```java
pipeline.apply(PubsubIO.readStrings()
    .fromSubscription(subscription)
    .withIdAttribute("id"));
```

#### ID 查找顺序
1. 首先在消息的 attributes 中查找
2. 如果 attributes 中未找到，则在消息体（假设是 JSON）中查找
3. 如果都未找到，回退到使用 Pub/Sub 的 `messageId`

#### 消息格式示例

1. 使用 attributes 方式：
```json
Message {
    "data": {
        "type": "order",
        "orderId": "12345",
        "amount": 100
    },
    "attributes": {
        "id": "order-12345-20231201"
    }
}
```

2. 使用消息体方式：
```json
{
    "id": "order-12345-20231201",
    "type": "order",
    "orderId": "12345",
    "amount": 100
}
```

## 使用建议

### 1. 选择去重标识
- **系统级去重**：使用默认的 `messageId`
  - 优点：无需额外配置
  - 缺点：无法体现业务含义
  
- **业务级去重**：使用自定义 ID
  - 优点：可以基于业务逻辑去重（如订单ID）
  - 缺点：需要确保业务 ID 的唯一性

### 2. ID 放置位置
- **属性（attributes）中**：
  - 适用于：ID 是系统控制的元数据
  - 优点：与业务数据分离
  
- **消息体中**：
  - 适用于：ID 是业务数据的一部分
  - 优点：消息自描述性更好

### 3. 最佳实践
1. 为每条消息指定业务相关的唯一标识符
2. 保持 ID 生成策略的一致性
3. 在消息体中包含 ID 时使用统一的字段名（如 "id"）
4. 记录和监控重复消息的情况

## 注意事项

1. **ID 唯一性**：
   - 确保业务 ID 在相关业务范围内唯一
   - 考虑使用组合键（如 "orderType-orderId-timestamp"）

2. **性能考虑**：
   - ID 的长度会影响存储和处理性能
   - 避免使用过长的 ID 值

3. **监控建议**：
   - 监控重复消息的频率
   - 记录去重触发的次数
   - 分析重复消息的来源

## 故障排查

当遇到消息重复或丢失问题时：
1. 检查消息的 ID 生成逻辑
2. 验证消息格式是否正确
3. 确认 ID 字段的位置（attributes 还是消息体）
4. 查看 Dataflow 作业的日志

## 相关配置

```java
// 配置示例
PipelineOptions options = PipelineOptionsFactory.create();
options.setRunner(DataflowRunner.class);
Pipeline pipeline = Pipeline.create(options);

pipeline.apply("Read From Pub/Sub",
    PubsubIO.readStrings()
        .fromSubscription(subscription)
        .withIdAttribute("id")
        .withTimestampAttribute("timestamp"));
```
