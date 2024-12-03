# Dataflow 死信队列（Dead Letter Queue，DLQ）

## 概述
Dataflow 死信队列是一种处理机制，用于处理那些经过多次尝试后仍无法成功处理的消息。它提供了一种可靠的方式来处理失败的消息，确保数据不会丢失。

## 工作原理

### 1. 配置方式
```java
// Pipeline 选项配置
public interface Options extends PipelineOptions {
    @Description("死信队列主题")
    String getDeadLetterTopic();
    void setDeadLetterTopic(String topic);
}

// 在管道中使用
PubsubIO.readMessages()
    .fromSubscription(options.getSubscription())
    .withDeadLetterTopic(options.getDeadLetterTopic())
    .withMaxRetries(3)  // 可选：配置最大重试次数
```

### 2. 自动消息路由
Dataflow 在以下情况下自动将消息路由到死信队列：
- 消息处理在达到最大重试次数后仍然失败
- 处理过程中发生不可恢复的错误
- DoFn 抛出 RuntimeException

### 3. 错误处理机制
```java
@ProcessElement
public void processElement(@Element PubsubMessage msg) {
    try {
        // 处理消息
        processMessage(msg);
    } catch (RecoverableException e) {
        // Dataflow 将重试
        throw new RuntimeException("临时失败，将重试", e);
    } catch (UnrecoverableException e) {
        // 重试后消息将发送到死信队列
        throw new RuntimeException("永久性失败", e);
    }
}
```

### 4. 死信队列消息格式
死信队列中的消息包含：
- 原始消息内容
- 错误信息
- 处理元数据：
  - 时间戳
  - 重试次数
  - 错误类型
  - 堆栈跟踪

## 最佳实践

### 1. 错误分类
- 明确区分可恢复和不可恢复错误
- 使用适当的异常类型
- 不要捕获和抑制应该触发死信队列的异常

### 2. 重试配置
- 设置适当的重试次数
- 配置重试延迟
- 根据业务需求设置重试策略

### 3. 监控
- 监控死信队列消息量
- 设置死信队列消息突增的告警
- 定期检查死信队列内容

### 4. 消息恢复
- 实现死信队列消息审查流程
- 创建消息重放工具
- 记录恢复程序

## 实现指南

### 1. 适用场景
应该使用死信队列的情况：
- 消息格式无效
- 违反业务规则
- 外部系统永久性故障
- 数据一致性问题

### 2. 不适用场景
不应使用死信队列的情况：
- 临时网络问题
- 速率限制
- 临时数据库错误
- 连接超时

### 3. 代码示例

#### 基本配置
```java
Options options = PipelineOptionsFactory.create().as(Options.class);
options.setDeadLetterTopic("projects/my-project/topics/my-dlq");
```

#### 错误处理
```java
@ProcessElement
public void processElement(@Element PubsubMessage msg) {
    try {
        // 尝试处理
        processMessage(msg);
    } catch (Exception e) {
        if (isRecoverable(e)) {
            // 将重试
            throw new RuntimeException("临时失败", e);
        } else {
            // 重试后将进入死信队列
            throw new RuntimeException("永久性失败", e);
        }
    }
}
```

## 故障排除

### 常见问题
1. 消息未到达死信队列
   - 检查重试配置
   - 验证异常处理
   - 检查死信队列主题权限

2. 死信队列消息过多
   - 检查错误模式
   - 检查外部依赖
   - 验证业务逻辑

### 监控指标
- 失败消息计数
- 重试尝试次数
- 死信队列消息量
- 处理延迟

## 补充资源
- [Dataflow 文档](https://cloud.google.com/dataflow/docs)
- [错误处理最佳实践](https://cloud.google.com/dataflow/docs/guides/common-errors)
- [Pub/Sub 死信主题](https://cloud.google.com/pubsub/docs/dead-letter-topics)

## 安全考虑
- 死信队列可能包含敏感数据
- 需要适当的访问控制
- 考虑数据加密
