# Oman DCG Connector Design

## Overview
The Oman DCG connector is a lightweight API gateway used to centrally manage and forward calls to third-party APIs. Its main goal is to simplify the integration process of third-party APIs, unify authentication management, and provide basic routing and forwarding capabilities.

## Core Features
- Configuration-driven: Integrate new third-party systems through configuration files without modifying the code
- Unified authentication: Centrally manage authentication information for third-party systems
- Extensibility: Easy to add new third-party systems and functional extensions

## System Architecture

### Core Components
1. **Configuration Manager (ConfigManager)**
   - Load and manage third-party system configurations
   - Support dynamic configuration updates
   - Configuration format uses YAML, which is easy to read and maintain

2. **Router Manager (RouterManager)**
   - Dynamically generate routing rules based on configuration
   - Support retry on failure

3. **Authentication Manager (AuthManager)**
   - Manage authentication information for third-party systems
   - Support multiple authentication methods (Token, OAuth, etc.), implement Token method for now
   - Token auto-refresh mechanism
   - Support multiple refresh strategies (OAuth2 refresh_token, reacquire, etc.), implement reacquire method for now
   - Token validity monitoring
   - Concurrent refresh control
   - Refresh failure retry mechanism

### Unified Interface Format

#### Request Format
```json
// if downstream system content type is xml
{
    "scheme": "third_party_service_name",
    "service": "service_name",
    "data": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><name>john_doe</name><email>john@example.com</email></request>"
}

// if downstream system content type is json
{
    "scheme": "third_party_service_name",
    "service": "service_name",
    "data": {
        // downstream request body
    }
}
```

#### Response Format
```json
// if downstream system content type is xml
{
    "code": "200",
    "message": "response_message",
    "data": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response><status>success</status><user_id>12345</user_id></response>"
}

// if downstream system content type is json
{
    "code": "200",
    "message": "response_message",
    "data": {
        // downstream system response body
    }
}
```

## Configuration Example

### System Configuration (config.yaml)
```yaml
dcg:
  vas:
    schemes:
      mala:  # service name, corresponds to the service field in the request
        base_url: "https://api.mala.com"  # Base URL of the third-party system
        content_type: "json"  # json or xml
        services:  # service mapping
          list:
            endpoint: "https://api.mala.com/list"
          delete:
            endpoint: "https://api.mala.com/delete"
        auth:
          type: "bearer_token"
          token: ""
          token_refresh:
            type: "reacquire"  # supports: oauth2, reacquire
            url: "https://api.mala.com/api/token"
            method: "POST"
            body:
              username: "${USERNAME}"
              password: "${PASSWORD}"
            token_path: "$.token"
            refresh_before_expiry: "300"  # Trigger refresh 5 minutes before token expiry
            max_retry_times: 3
            retry_interval: "10"  # 10s
```

## Usage

### 1. Add a new third-party system
1. Add the basic configuration (base_url, authentication information) of the new system in the configuration file
2. Configure routing rules
3. Restart or hot update the service

### 2. Call Example

#### XML Content Example
```http
POST http://oman-connector/
Content-Type: application/json
X-Custom-Header: custom-value
Authorization: Bearer user-token

{
    "scheme": "mala",
    "service": "list",
    "data": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><name>john_doe</name><email>john@example.com</email></request>"
}

# Transformed downstream request
POST https://api.mala.com/list
Content-Type: application/xml
X-Custom-Header: custom-value
Authorization: Bearer system-token

<?xml version="1.0" encoding="UTF-8"?>
<request>
    <name>john_doe</name>
    <email>john@example.com</email>
</request>
```

#### JSON Content Example
```http
POST http://oman-connector/
Content-Type: application/json

{
    "scheme": "mala",
    "service": "delete",
    "data": {
        "name": "john_doe",
        "email": "john@example.com",
        "age": 25,
        "type": "vip"
    }
}

# Transformed downstream request
POST https://api.mala.com/delete
Content-Type: application/json
X-Custom-Header: custom-value
Authorization: Bearer system-token

{
    "name": "john_doe",
    "email": "john@example.com",
    "age": 25,
    "type": "vip"
}
```

## Request Forwarding Instructions

### Routing Rules
- Base on the scheme and service in the request body find the downstream endpoint from the config
- Pass the data field to the downstream system as the request body
- Put the downstream response body to the data field and pass to the client

### Token Refresh Mechanism

#### Reacquisition Process
1. The system periodically checks the token validity period
2. Trigger refresh before the token expires
3. Call the configured token acquisition interface
4. Update the token information in the local cache

### Error Handling
- Unified error code system
- Detailed error log recording
- Request timeout and retry mechanism

## Recent Updates

### 1. Configuration Auto-refresh
- Added support for configuration auto-refresh using Spring Cloud Config
- Added @RefreshScope to DcgConfig for dynamic configuration updates
- Added actuator endpoint for manual refresh trigger
- Configuration changes will not affect runtime states (like token cache)

### 2. HTTP Client Retry Mechanism
- Added centralized HTTP client retry configuration
```yaml
dcg:
  http:
    retry:
      max-attempts: 3
      initial-backoff: 1000  # Initial retry interval (ms)
      max-backoff: 5000     # Maximum retry interval (ms)
      multiplier: 2         # Retry interval multiplier
      retry-on-status:      # HTTP status codes that trigger retry
        - 502
        - 503
        - 504
```
- Implemented retry mechanism for both WebClient and RestTemplate
- Added support for configurable retry status codes
- Added exponential backoff with jitter

### 3. Token Management Improvements
- Centralized token management using TokenManager
- Prevent token loss during configuration refresh
- Added concurrent refresh control using locks
- Each scheme maintains its own TokenCache instance

### 4. Content Handling Simplification
- Removed content conversion between JSON and XML
- Request handling:
  - Forward request.data directly as request body to downstream system
  - Content-Type is set based on scheme configuration
- Response handling:
  - For JSON responses: Parse to Object and put in response.data
  - For XML responses: Keep as String and put in response.data
  - Always return JSON response to client