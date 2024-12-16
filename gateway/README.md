# Gateway Module

## 概述
Gateway模块是一个轻量级的API网关，用于统一管理和转发对第三方API的调用。它的主要目标是简化第三方API的接入流程，统一认证管理，并提供基础的路由转发能力。

## 核心特性
- 配置驱动：通过配置文件方式接入新的第三方系统，无需修改代码
- 统一认证：集中管理第三方系统的认证信息
- 透明转发：直接转发请求数据，不做格式转换
- 可扩展性：易于添加新的第三方系统和功能扩展

## 系统架构

### 核心组件
1. **配置管理器 (ConfigManager)**
   - 加载和管理第三方系统配置
   - 支持动态配置更新
   - 配置格式采用YAML，便于阅读和维护

2. **路由管理器 (RouterManager)**
   - 根据配置动态生成路由规则
   - 支持请求的负载均衡和失败重试

3. **认证管理器 (AuthManager)**
   - 管理第三方系统的认证信息
   - 支持多种认证方式（Token、ApiKey、OAuth等）
   - 认证信息加密存储
   - Token自动刷新机制
     - 支持多种刷新策略（OAuth2 refresh_token、重新获取等）
     - Token有效期监控
     - 并发刷新控制
     - 刷新失败重试机制
     - 支持自定义刷新逻辑

### 统一接口格式

#### 请求格式
```json
{
    "service": "third_party_service_name",
    "action": "api_action_name",
    "data": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><name>john_doe</name><email>john@example.com</email></request>"
}
```

#### 响应格式
```json
{
    "code": "response_code",
    "message": "response_message",
    "data": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response><status>success</status><user_id>12345</user_id></response>"
}
```

## 配置示例

### 系统配置 (config.yaml)
```yaml
services:
  example_service:  # 服务名称，与请求中的service字段对应
    base_url: "https://api.example.com"  # 第三方系统的基础URL
    auth:
      type: "bearer_token"
      token: "${ENV_TOKEN}"
      token_refresh:
        type: "oauth2"  # 支持: oauth2, reacquire
        refresh_token: "${ENV_REFRESH_TOKEN}"
        refresh_url: "/oauth2/token"
        refresh_before_expiry: "5m"  # token过期前5分钟触发刷新
        max_retry_times: 3
        retry_interval: "10s"
        # OAuth2配置
        client_id: "${CLIENT_ID}"
        client_secret: "${CLIENT_SECRET}"
        # 重新获取token配置
        reacquire:
          url: "/api/token"
          method: "POST"
          body:
            app_id: "${APP_ID}"
            app_secret: "${APP_SECRET}"
```

## 使用方法

### 1. 添加新的第三方系统
1. 在配置文件中添加新系统的基本配置（base_url、认证信息）
2. 配置路由规则
3. 重启或热更新服务

### 2. 调用示例

#### XML内容示例
```http
POST /gateway/example_service/users
Content-Type: application/json
X-Custom-Header: custom-value
Authorization: Bearer user-token

{
    "service": "example_service",
    "action": "create_user",
    "data": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><name>john_doe</name><email>john@example.com</email></request>"
}

# 转换后的下游请求
POST https://api.example.com/users
Content-Type: application/xml
X-Custom-Header: custom-value
Authorization: Bearer system-token

<?xml version="1.0" encoding="UTF-8"?>
<request>
    <name>john_doe</name>
    <email>john@example.com</email>
</request>
```

#### JSON内容示例
```http
POST /gateway/example_service/users
Content-Type: application/json

{
    "service": "example_service",
    "action": "create_user",
    "data": {
        "name": "john_doe",
        "email": "john@example.com",
        "age": 25,
        "type": "vip"
    }
}
```

## 请求转发说明

### 路由规则
- 网关URL格式：/gateway/{service_name}/{path}
- service_name必须与配置文件中的服务名称对应
- path部分会直接拼接到对应服务的base_url后
- 不对URL路径做任何转换或映射

### Query参数处理
- URL中的query参数完全透传给下游系统
- 支持多值参数（例如：?key=value1&key=value2）
- 保持参数顺序和编码格式

### Header处理
- 除了特定的认证相关header外，其他header都会透传给下游系统
- 支持自定义header
- 保持header的大小写格式
- 支持多值header

### Token刷新机制

#### OAuth2刷新流程
1. 系统定时检查token有效期
2. 在token过期前触发刷新（可配置提前时间）
3. 使用refresh_token调用认证服务获取新token
4. 更新配置中的token信息
5. 后续请求使用新token

#### 重新获取流程
1. 系统定时检查token有效期
2. 在token过期前触发刷新
3. 调用配置的token获取接口
4. 使用app_id和app_secret获取新token
5. 更新配置中的token信息

#### 并发控制
- 使用分布式锁防止并发刷新
- 其他请求等待新token
- 支持降级策略（使用旧token）

#### 异常处理
- 刷新失败自动重试
- 超过重试次数后告警
- 记录详细的刷新日志

## 查询参数处理
- URL中的query参数会直接透传给下游系统
- 支持在请求URL中直接带入查询参数
- 不对参数做任何转换或映射

## 错误处理
- 统一的错误码体系
- 详细的错误日志记录
- 请求超时和重试机制
- 优雅的降级和熔断策略

## 扩展功能
- 请求限流
- 访问日志记录
- 监控指标收集
- 缓存支持
- 服务发现集成

## 部署说明
1. 配置文件准备
2. 环境变量设置
3. 服务启动和监控
4. 日志收集和分析
  </rewritten_file>