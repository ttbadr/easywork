# Pub/Sub to AlloyDB Dataflow Pipeline

这个模块实现了一个 Google Cloud Dataflow pipeline，用于将 Pub/Sub 消息实时写入到 AlloyDB 数据库中。

## 功能特点

- 支持订阅多个 Pub/Sub topic
- 支持将不同类型的消息写入不同的数据库表
- 灵活的消息到表的映射配置
- 支持多种数据类型的转换

## 前置条件

1. Java 11 或更高版本
2. Maven 3.6 或更高版本
3. Google Cloud 项目配置：
   - 启用 Dataflow API
   - 启用 Pub/Sub API
   - 配置好 AlloyDB 实例
   - 设置好服务账号和权限

## 配置说明

### 表映射配置

在 `src/main/resources/table-mapping.json` 中配置消息类型到数据库表的映射：

```json
[
  {
    "messageType": "user_event",
    "tableName": "user_events",
    "columns": [
      {
        "jsonPath": "/user_id",
        "columnName": "user_id",
        "columnType": "STRING"
      },
      {
        "jsonPath": "/event_type",
        "columnName": "event_type",
        "columnType": "STRING"
      }
    ]
  }
]
```

配置说明：
- `messageType`: Pub/Sub 消息中的类型字段值
- `tableName`: 目标数据库表名
- `columns`: 列映射配置
  - `jsonPath`: JSON 消息中的字段路径
  - `columnName`: 数据库表中的列名
  - `columnType`: 数据类型（支持 STRING、INTEGER、BIGINT、DOUBLE、BOOLEAN）

### 数据库表创建

需要在 AlloyDB 中预先创建对应的表。例如：

```sql
CREATE TABLE user_events (
    user_id STRING,
    event_type STRING,
    event_timestamp BIGINT
);

CREATE TABLE orders (
    order_id STRING,
    customer_id STRING,
    total_amount DOUBLE,
    created_at BIGINT
);
```

## 构建和运行

### 构建项目

```bash
mvn clean package
```

### 运行 Dataflow Pipeline

```bash
java -jar target/dataflow-1.0-SNAPSHOT.jar \
  --project=your-project-id \
  --subscriptions=projects/your-project/subscriptions/subscription1,projects/your-project/subscriptions/subscription2 \
  --jdbcUrl=jdbc:alloydb:your-alloydb-connection-string \
  --tableMappingConfig=src/main/resources/table-mapping.json \
  --runner=DataflowRunner \
  --region=your-region
```

参数说明：
- `--project`: Google Cloud 项目 ID
- `--subscriptions`: Pub/Sub 订阅列表，多个订阅用逗号分隔
- `--jdbcUrl`: AlloyDB JDBC 连接字符串
- `--tableMappingConfig`: 表映射配置文件路径
- `--runner`: Dataflow 运行器（DataflowRunner 或 DirectRunner）
- `--region`: Google Cloud 区域

### 可选的 Dataflow 参数

```bash
  --numWorkers=5 \                    # 工作节点数量
  --maxNumWorkers=10 \               # 最大工作节点数量
  --workerMachineType=n1-standard-2 \ # 工作节点机器类型
  --zone=us-central1-a               # 部署区域
```

## 监控和日志

1. 在 Google Cloud Console 的 Dataflow 页面查看 pipeline 运行状态
2. 使用 Cloud Logging 查看详细日志
3. 设置 Cloud Monitoring 告警监控任务状态

## 故障排除

1. 连接问题：
   - 检查 AlloyDB 连接字符串是否正确
   - 验证网络和防火墙配置
   - 确认服务账号权限

2. 消息处理错误：
   - 检查消息格式是否符合表映射配置
   - 验证数据类型是否匹配
   - 查看 Cloud Logging 中的错误日志

3. 性能问题：
   - 调整工作节点数量和类型
   - 优化数据库表索引
   - 考虑批量写入策略

## 最佳实践

1. 消息格式：
   - 使用统一的消息格式
   - 包含消息类型字段
   - 使用合适的数据类型

2. 数据库设计：
   - 创建适当的索引
   - 选择合适的列类型
   - 定期维护和优化

3. 监控和维护：
   - 设置合适的监控指标
   - 定期检查性能
   - 及时更新依赖版本
