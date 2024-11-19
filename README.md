# GCP Pub/Sub Utils

这是一个简单的 Google Cloud Pub/Sub 工具库，提供了便捷的消息发布和订阅功能。

## 前提条件

1. JDK 11 或更高版本
2. Maven 3.6 或更高版本
3. Google Cloud 项目和相应的认证配置

## 认证配置

在使用此库之前，请确保已经正确配置了 Google Cloud 认证。您可以通过以下方式之一进行配置：

### 方式一：使用服务账号密钥文件

1. 在 Google Cloud Console 中创建服务账号并下载密钥文件（JSON格式）
2. 在代码中初始化认证：
```java
PubSubCredentials.initializeFromFile("/path/to/service-account-key.json");
```

### 方式二：使用默认凭证（推荐）

这种方式会按以下顺序尝试获取凭证：
1. `GOOGLE_APPLICATION_CREDENTIALS` 环境变量指定的服务账号密钥文件
2. gcloud 默认凭证
3. App Engine 默认凭证
4. Compute Engine 默认凭证

```java
// 设置环境变量（如果使用服务账号密钥文件）
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"

// 或者使用 gcloud 登录（推荐开发环境使用）
gcloud auth application-default login

// 在代码中初始化认证
PubSubCredentials.initializeDefault();
```

## 使用方法

### 1. 发布消息

```java
// 初始化认证
PubSubCredentials.initializeDefault();

// 创建发布者
String projectId = "your-project-id";
String topicId = "your-topic-id";
PubSubPublisher publisher = new PubSubPublisher(projectId, topicId);

// 发布带属性的消息
Map<String, String> attributes = new HashMap<>();
attributes.put("key1", "value1");
String messageId = publisher.publish("Hello, World!", attributes);

// 发布简单消息
String messageId = publisher.publish("Hello, World!");

// 使用完后关闭
publisher.shutdown();
```

### 2. 订阅消息

```java
// 初始化认证
PubSubCredentials.initializeDefault();

// 创建订阅者
String projectId = "your-project-id";
String subscriptionId = "your-subscription-id";
PubSubSubscriber subscriber = new PubSubSubscriber(projectId, subscriptionId);

// 启动订阅并处理消息
subscriber.startSubscription((message, attributes) -> {
    System.out.println("Received message: " + message);
    if (attributes != null) {
        System.out.println("Attributes: " + attributes);
    }
});

// 停止订阅
subscriber.stopSubscription();
```

## 注意事项

1. 确保在使用任何 Pub/Sub 功能之前先初始化认证
2. 确保在使用完PubSubPublisher和PubSubSubscriber后调用相应的shutdown/stop方法以释放资源
3. 消息处理器中的异常会导致消息被nack，消息会重新投递
4. 默认情况下，订阅者会自动确认消息（ack）

## 依赖

```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-pubsub</artifactId>
    <version>1.123.12</version>
</dependency>
