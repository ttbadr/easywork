# Google Cloud Storage (GCS) 使用说明

Dataflow pipeline 需要使用 Google Cloud Storage (GCS) 来支持其运行和管理。本文档详细说明了 GCS 的使用目的、所需权限和最佳实践。

## GCS 主要用途

### 1. 暂存文件 (Staging Files)
```java
options.setStagingLocation("gs://your-bucket/staging");
```
- 用途：
  - 上传编译后的 JAR 包
  - 上传所有依赖库
  - 上传配置文件和其他资源
  - 供 worker 节点下载运行所需的代码
- 建议：
  - 使用专门的 staging 目录
  - 定期清理旧文件
  - 设置适当的访问权限

### 2. 临时文件 (Temporary Files)
```java
options.setTempLocation("gs://your-bucket/temp");
```
- 用途：
  - 存储 pipeline 处理过程中的中间数据
  - 支持数据分片和 shuffle 操作
  - 保存检查点数据用于容错
  - 存储性能统计和监控数据
- 建议：
  - 使用生命周期策略自动清理
  - 监控存储使用量
  - 选择合适的存储类别

### 3. 作业模板 (Templates)
```java
options.setTemplateLocation("gs://your-bucket/templates");
```
- 用途：
  - 存储预配置的 pipeline 定义
  - 支持 pipeline 模板化和重用
  - 允许非开发人员启动预定义的 pipeline
- 建议：
  - 使用版本控制
  - 维护模板文档
  - 定期更新和清理

## 所需 GCS 权限

Pipeline 运行需要以下 GCS 权限：

1. **Bucket 级别**
   - `storage.buckets.get`：访问 bucket

2. **对象级别**
   - `storage.objects.create`：创建新文件
   - `storage.objects.get`：读取文件
   - `storage.objects.list`：列出文件
   - `storage.objects.update`：更新文件
   - `storage.objects.delete`：删除临时文件

## 最佳实践

### 1. 环境隔离
- 为不同环境使用不同的 bucket：
  ```
  gs://your-project-dev/
  gs://your-project-staging/
  gs://your-project-prod/
  ```
- 使用清晰的命名约定
- 适当设置访问权限

### 2. 资源管理
- 定期清理临时文件
- 设置文件生命周期策略：
  ```
  临时文件：7天后删除
  暂存文件：30天后删除
  模板文件：保留最新5个版本
  ```
- 监控存储使用量和成本

### 3. 性能优化
- 选择合适的 bucket 位置（靠近 Dataflow worker）
- 使用适当的文件命名模式
- 考虑设置对象版本控制

### 4. 安全考虑
- 使用最小权限原则
- 加密敏感数据
- 定期审查访问日志
- 设置适当的 IAM 策略

### 5. 成本控制
- 设置预算告警
- 使用自动清理策略
- 监控存储类别使用情况
- 选择合适的存储类别

## 配置示例

### 基本配置
```java
DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
options.setStagingLocation("gs://your-bucket/staging");
options.setTempLocation("gs://your-bucket/temp");
```

### 带环境区分的配置
```java
String env = System.getenv("ENVIRONMENT");
String projectId = "your-project-id";
String bucketName = String.format("gs://%s-%s", projectId, env);

options.setStagingLocation(bucketName + "/staging");
options.setTempLocation(bucketName + "/temp");
```

### 完整配置示例
```java
DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);

// 设置基本选项
options.setProject("your-project-id");
options.setRegion("us-central1");

// 设置 GCS 路径
String bucketBase = "gs://your-project-bucket";
options.setStagingLocation(bucketBase + "/staging");
options.setTempLocation(bucketBase + "/temp");
options.setTemplateLocation(bucketBase + "/templates");

// 设置作业选项
options.setJobName("your-job-name");
options.setRunner(DataflowRunner.class);
```

## 故障排除

### 常见问题
1. **权限不足**
   - 检查服务账号权限
   - 验证 bucket 访问权限
   - 确认 IAM 角色配置

2. **存储空间问题**
   - 监控配额使用情况
   - 检查清理策略
   - 验证预算设置

3. **性能问题**
   - 检查 bucket 位置
   - 优化文件命名
   - 调整存储类别

### 解决方案
1. **权限问题**
   ```bash
   # 验证服务账号权限
   gcloud projects get-iam-policy your-project
   
   # 授予所需权限
   gcloud projects add-iam-policy-binding your-project \
       --member="serviceAccount:your-sa@your-project.iam.gserviceaccount.com" \
       --role="roles/storage.objectViewer"
   ```

2. **存储管理**
   ```bash
   # 列出过期文件
   gsutil ls -l gs://your-bucket/temp
   
   # 清理临时文件
   gsutil rm gs://your-bucket/temp/**
   ```
