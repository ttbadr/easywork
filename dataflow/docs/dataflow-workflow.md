# Dataflow Pipeline 工作流程

本文档详细说明了 Dataflow pipeline 的打包、部署和执行过程，重点介绍了从本地开发到分布式执行的完整工作流程。

## 概述

Google Cloud Dataflow 是一个完全托管的服务，用于在 Google Cloud Platform 生态系统中执行 Apache Beam pipelines。了解代码如何从本地开发环境转移到分布式执行环境对于有效开发 pipeline 至关重要。

## Pipeline 生命周期

### 1. 代码打包阶段
当你运行 `mvn package` 时：
- Maven 将你的源代码、依赖项和资源文件编译打包成一个 fat JAR（uber JAR）
- JAR 包含：
  - 你的源代码（如 PubSubToAlloyDB.java, AlloyDBWriter.java）
  - 所有依赖项（如 beam-sdks-java-core, beam-runners-dataflow-java）
  - 资源文件（如 table-mapping.yaml）

### 2. 作业提交阶段
当你执行 `java -jar` 或通过 IDE 启动程序时：

```java
Pipeline pipeline = Pipeline.create(options);
// ... configure pipeline ...
pipeline.run();
```

会发生以下步骤：

#### a) 本地准备
- Dataflow SDK 验证你的 Pipeline 配置
- 检查所有必需的选项（project, temp_location 等）
- 构建 Pipeline 的 DAG（有向无环图）

#### b) 代码上传
- SDK 将整个 JAR 文件上传到指定的 `staging_location`
- 上传路径：`gs://your-bucket/staging/your-jar-name.jar`
- 同时上传配置文件和元数据

#### c) 作业创建
- SDK 调用 Dataflow API 创建新作业
- 发送作业配置，包括：
  - JAR 文件位置
  - Pipeline 图信息
  - 运行时参数
  - 资源配置（worker 数量、机器类型等）

### 3. 运行时阶段
当作业在 Dataflow 服务上运行时：
- Dataflow 服务从 staging location 下载你的 JAR
- 在每个 worker 上解压并加载代码
- 执行你定义的 transforms（PubsubIO.Read, ParDo 等）

## 作业创建和管理

### 作业创建行为
- 每次执行 `java -jar` 都会创建一个**新的** Dataflow 作业
- 这意味着多次运行相同的 JAR 会创建多个作业
- 每个作业都有唯一的作业 ID 并独立运行

### 作业管理策略

1. **更新现有作业**
```java
DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
options.setJobName("your-fixed-job-name");
options.setUpdate(true);  // Enable job update mode
```
- 使用固定的作业名称并启用更新模式
- 不会创建新作业，而是更新现有作业
- 适用于持续部署场景

2. **取消之前的作业**
```java
DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
options.setJobName("your-fixed-job-name");
options.setCancellationToken("unique-token");  // Set cancellation token
```
- 使用取消令牌管理作业生命周期
- 具有相同令牌的新作业会取消之前的作业
- 有助于防止重复运行的作业

3. **替换运行中的作业**
```java
DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
options.setJobName("your-fixed-job-name");
options.setReplace(true);  // Enable replace mode
```
- 新作业将替换具有相同名称的现有作业
- 确保同一时间只运行一个作业实例
- 实现作业版本之间的平滑过渡

### 作业管理最佳实践

1. **开发/测试**
- 使用 `DirectRunner` 进行本地测试
- 使用唯一的作业名称避免冲突
- 完成后清理测试作业

2. **生产环境**
- 使用固定的作业名称便于跟踪
- 实施适当的作业更新/替换策略
- 监控作业状态和资源使用情况

3. **成本控制**
- 取消未使用的作业
- 使用作业模板进行重复执行
- 监控 worker 数量和自动扩缩容设置

4. **版本控制**
```java
options.setJobName(String.format("your-job-name-v%s", version));
```
- 在作业名称中包含版本信息
- 帮助跟踪不同版本的运行情况
- 便于需要时进行回滚

### 常见场景

1. **CI/CD 流水线**
```java
// In your deployment script
options.setJobName("production-pipeline");
options.setUpdate(true);
options.setLabels(ImmutableMap.of(
    "version", "v1.2.3",
    "environment", "production"
));
```
- 使用更新模式进行持续部署
- 添加标签用于跟踪和管理
- 在部署过程中保持一致的作业名称

2. **开发/测试环境**
```java
// In your test environment
options.setJobName("test-pipeline-" + UUID.randomUUID());
options.setRunner(DirectRunner.class);
```
- 为测试作业使用唯一名称
- 使用 DirectRunner 加快测试速度
- 测试完成后进行清理

3. **多环境支持**
```java
String env = System.getenv("ENVIRONMENT");
options.setJobName(String.format("pipeline-%s", env));
options.setLabels(ImmutableMap.of("environment", env));
```
- 基于环境的动态作业名称
- 针对不同环境的配置
- 分离监控和告警

## 代码分布

### Main 方法与 Worker 执行
Main 方法作为"蓝图"创建者，而不是执行点：

```java
public static void main(String[] args) {
    // 1. Runs only on your local machine
    Options options = PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(Options.class);

    // 2. Builds Pipeline definition
    Pipeline pipeline = Pipeline.create(options);
    
    // 3. Configures Pipeline graph
    pipeline
        .apply("Read From PubSub", 
            PubsubIO.readStrings().fromSubscription(subscription))
        .apply("Write To AlloyDB",
            ParDo.of(new AlloyDBWriter(jdbcUrl, tableMappings)));

    // 4. Submits Pipeline to Dataflow service
    pipeline.run();
}
```

### Worker 执行点
Worker 执行带有 Beam 注解的特定方法：

```java
public class AlloyDBWriter extends DoFn<String, Void> {
    @Setup
    public void setup() {
        // 1. Executes once when worker starts
        // Example: Initialize database connection
    }

    @StartBundle
    public void startBundle() {
        // 2. Executes at the start of each bundle
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        // 3. This is the worker's actual execution entry point!
        // Called for each message
        String message = c.element();
        // Process message...
    }

    @FinishBundle
    public void finishBundle() {
        // 4. Executes at the end of each bundle
    }

    @Teardown
    public void teardown() {
        // 5. Executes once when worker shuts down
        // Example: Close database connection
    }
}
```

## 执行流程

```
Local Machine                  Dataflow Service                 Worker
   |                              |                            |
   |-- Run main method ---------->|                            |
   |   Build Pipeline definition  |                            |
   |                              |                            |
   |                              |-- Create Worker instance -->|
   |                              |                            |
   |                              |                            |-- Execute @Setup
   |                              |                            |
   |                              |-- Send data chunk -------->|
   |                              |                            |-- Execute @StartBundle
   |                              |                            |-- Loop @ProcessElement
   |                              |                            |-- Execute @FinishBundle
   |                              |                            |
   |                              |-- Send more chunks ------->|
   |                              |                            |   (Repeat above)
   |                              |                            |
   |                              |-- Notify job complete ---->|
   |                              |                            |-- Execute @Teardown
```

## 设计原理

1. **分布式执行**：Pipeline 可以在多个 worker 上并行运行
2. **状态管理**：每个 worker 维护自己的状态（如数据库连接）
3. **容错处理**：worker 可以随时失败和重启，不影响整体 Pipeline
4. **资源效率**：worker 只加载必要的代码和资源

## 示例场景

对于处理 Pub/Sub 消息并写入 AlloyDB 的 Pipeline：
```
[Pub/Sub] --> [Worker 1: @ProcessElement] -----> [AlloyDB]
          --> [Worker 2: @ProcessElement] -----> [AlloyDB]
          --> [Worker 3: @ProcessElement] -----> [AlloyDB]
```

- 每个 worker 运行相同的 @ProcessElement 代码
- 它们处理不同的数据分片
- 它们共享配置（来自 main 方法）但维护独立的运行时状态

## 最佳实践

1. 确保所有依赖都在 pom.xml 中正确声明
2. 适当打包资源文件
3. 为序列化类实现 Serializable 接口
4. 通过 Pipeline options 传递配置参数
5. 为分布式执行和容错设计
