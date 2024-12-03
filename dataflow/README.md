# Pub/Sub to AlloyDB Dataflow Pipeline

这个模块实现了一个 Google Cloud Dataflow pipeline，用于将 Pub/Sub 消息实时写入到 AlloyDB 数据库中。

## 功能特点

- 支持订阅多个 Pub/Sub topic
- 支持将不同类型的消息写入不同的数据库表
- 灵活的消息到表的映射配置
- 支持多种数据类型的转换

## 技术文档

- [Dataflow Pipeline 工作流程](docs/dataflow-workflow.md) - 详细解释了 Dataflow pipeline 的打包、部署和执行过程
- [消息去重机制](docs/message_deduplication.md) - 详细说明了如何处理重复消息，确保数据一致性

## 前置条件

1. Java 11 或更高版本
2. Maven 3.6 或更高版本
3. Google Cloud 项目配置：
   - 启用 Dataflow API
   - 启用 Pub/Sub API
   - 配置好 AlloyDB 实例
   - 设置好服务账号和权限：
     - `roles/dataflow.developer`：创建和管理 Dataflow jobs
     - `roles/dataflow.worker`：运行 Dataflow worker
     - `roles/pubsub.subscriber`：读取 Pub/Sub 消息
     - `roles/alloydb.client`：连接 AlloyDB 实例
     - `roles/storage.objectUser`：读写 GCS 中的文件（用于 jar、配置和临时文件）

   注意：也可以创建自定义角色，仅包含以下必要权限：
   - Dataflow: `dataflow.jobs.*`, `dataflow.worker.*`
   - Pub/Sub: `pubsub.subscriptions.consume`, `pubsub.subscriptions.get`
   - AlloyDB: `alloydb.clusters.connect`, `alloydb.instances.connect`
   - Storage: `storage.objects.create`, `storage.objects.get`, `storage.objects.list`

### 数据库权限配置

除了 Google Cloud IAM 权限外，还需要配置 AlloyDB 数据库权限：

1. 创建数据库用户：
```sql
CREATE USER dataflow_user WITH PASSWORD 'your-password';
```

2. 授予数据库权限：
```sql
-- 连接权限
GRANT CONNECT ON DATABASE your_database TO dataflow_user;

-- Schema 权限
GRANT USAGE ON SCHEMA public TO dataflow_user;

-- 表权限（对所有目标表）
GRANT INSERT ON user_events TO dataflow_user;
GRANT INSERT ON orders TO dataflow_user;
-- 如果需要，添加其他表的权限
```

在运行 Pipeline 时，需要在 JDBC URL 中使用这个用户的凭证：
```
jdbc:postgresql://your-alloydb-instance/your-database?user=dataflow_user&password=your-password&...
```

建议使用 Google Cloud Secret Manager 存储数据库凭证。

## 认证配置

### 本地运行（使用 java 命令）
如果在本地环境运行，需要：
1. 下载服务账号密钥文件（JSON 格式）
2. 设置环境变量：
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### Google Cloud 环境运行
如果在 Google Cloud 环境（如 Compute Engine、Cloud Run）中运行：
1. 确保运行环境的默认服务账号或指定的服务账号有足够权限
2. 无需额外的认证配置，会自动使用实例的默认凭证

### 使用 gcloud 命令运行
如果使用 `gcloud` 命令运行：
1. 确保已登录：
```bash
gcloud auth login
gcloud config set project your-project-id
```
2. 无需额外的认证配置

## 配置说明

### 表映射配置

在 `src/main/resources/table-mapping.yaml` 中配置消息类型到数据库表的映射：

```yaml
- messageType: user_event
  tableName: user_events
  columns:
    - jsonPath: "$.user_id"
      columnName: user_id
      columnType: VARCHAR
    - jsonPath: "$.event_type"
      columnName: event_type
      columnType: VARCHAR
    - jsonPath: "$.created_at"
      columnName: created_at
      columnType: TIMESTAMP
      format: "yyyy-MM-dd HH:mm:ss.S"
    - jsonPath: "$.event_date"
      columnName: event_date
      columnType: DATE
      format: "yyyy-MM-dd"
```

配置说明：
- `messageType`: Pub/Sub 消息中的类型字段值
- `tableName`: 目标数据库表名
- `columns`: 列映射配置
  - `jsonPath`: JSON 消息中的字段路径（使用 JsonPath 语法）
  - `columnName`: 数据库表中的列名
  - `columnType`: 数据类型（支持 AlloyDB/PostgreSQL 的标准类型）
  - `format`: 日期时间类型的格式（可选，仅适用于 DATE 和 TIMESTAMP 类型）

支持的数据类型：
1. 字符类型：
   - `CHAR`/`CHARACTER`
   - `VARCHAR`/`CHARACTER VARYING`
   - `TEXT`

2. 数值类型：
   - `SMALLINT`/`INT2`：2字节整数
   - `INTEGER`/`INT`/`INT4`：4字节整数
   - `BIGINT`/`INT8`：8字节整数
   - `NUMERIC`/`DECIMAL`：精确数值
   - `REAL`/`FLOAT4`：4字节浮点数
   - `DOUBLE PRECISION`/`FLOAT8`：8字节浮点数

3. 布尔类型：
   - `BOOLEAN`/`BOOL`

4. 日期时间类型：
   - `DATE`：日期
   - `TIMESTAMP`/`TIMESTAMP WITHOUT TIME ZONE`：时间戳
   - `TIMESTAMP WITH TIME ZONE`/`TIMESTAMPTZ`：带时区的时间戳

日期时间格式说明：
- 年：`yyyy`（四位年份）或`yy`（两位年份）
- 月：`MM`（两位月份）
- 日：`dd`（两位日期）
- 时：`HH`（24小时制）或`hh`（12小时制）
- 分：`mm`
- 秒：`ss`
- 毫秒：`S`或`SSS`

默认格式：
- DATE类型：`yyyy-MM-dd`
- TIMESTAMP类型：`yyyy-MM-dd HH:mm:ss.S`

### 数据库表创建

需要在 AlloyDB 中预先创建对应的表。例如：

```sql
CREATE TABLE user_events (
    user_id VARCHAR(255),
    event_type VARCHAR(50),
    created_at TIMESTAMP,
    event_date DATE
);

CREATE TABLE orders (
    order_id VARCHAR(255),
    customer_id VARCHAR(255),
    total_amount NUMERIC(10,2),
    created_at TIMESTAMP WITH TIME ZONE
);
```

## 构建和运行

### 构建项目

```bash
mvn clean package
```

### 运行 Dataflow Pipeline

#### 方式一：使用 java 命令（适合本地开发和测试）

1. 确保已配置认证（参考上面的认证配置章节）
2. 运行命令：
```bash
java -jar target/dataflow-1.0-SNAPSHOT.jar \
  --project=your-project-id \
  --subscriptions=projects/your-project/subscriptions/subscription1,projects/your-project/subscriptions/subscription2 \
  --jdbcUrl=jdbc:alloydb:your-alloydb-connection-string \
  --tableMappingConfig=src/main/resources/table-mapping.yaml \
  --runner=DataflowRunner \
  --region=your-region
```

#### 方式二：使用 gcloud 命令（推荐用于生产环境）

1. 确保已登录 gcloud（参考上面的认证配置章节）
2. 将 jar 包和配置文件上传到 Google Cloud Storage：
```bash
gsutil cp target/dataflow-1.0-SNAPSHOT.jar gs://your-bucket/jars/
gsutil cp src/main/resources/table-mapping.yaml gs://your-bucket/config/
```

3. 运行命令：
```bash
gcloud dataflow jobs run job-name \
  --gcs-location=gs://your-bucket/jars/dataflow-1.0-SNAPSHOT.jar \
  --project=your-project-id \
  --region=your-region \
  --parameters \
  subscriptions=projects/your-project/subscriptions/subscription1,projects/your-project/subscriptions/subscription2 \
  --parameters \
  jdbcUrl=jdbc:alloydb:your-alloydb-connection-string \
  --parameters \
  tableMappingConfig=gs://your-bucket/config/table-mapping.yaml
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
