项目结构和依赖
Maven pom.xml
xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>async-api-tester</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <!-- Jackson for JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.17.1</version>
        </dependency>
        
        <!-- JUnit 5 for testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <compilerArgs>
                        <arg>--enable-preview</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <argLine>--enable-preview</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
Gradle build.gradle (替代方案)
gradle
plugins {
    id 'java'
    id 'application'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.1'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

application {
    mainClass = 'com.example.asynctester.AsyncAPITester'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '--enable-preview'
}

tasks.withType(Test) {
    jvmArgs '--enable-preview'
    useJUnitPlatform()
}

tasks.withType(JavaExec) {
    jvmArgs '--enable-preview'
}
编译和运行
使用Maven
bash
# 编译
mvn clean compile

# 运行
mvn exec:java -Dexec.mainClass="com.example.asynctester.AsyncAPITester"

# 或者打包后运行
mvn clean package
java --enable-preview -jar target/async-api-tester-1.0.0.jar
使用Gradle
bash
# 编译
./gradlew build

# 运行
./gradlew run
Java 21版本的优势
虚拟线程 (Virtual Threads): 轻量级线程，非常适合高并发I/O操作
Record类: 简洁的数据类定义，减少样板代码
Pattern Matching: 更优雅的条件判断（在异常处理中体现）
现代HTTP Client: 内置的异步HTTP客户端
改进的并发API: 更好的CompletableFuture和ExecutorService
自定义使用示例
java
public class CustomTest {
    public static void main(String[] args) throws Exception {
        // 自定义配置
        TestConfig config = new TestConfig(
            "https://your-api.com/async-endpoint",
            3001,                           // 回调端口
            200,                           // 200个请求
            20,                            // 20并发
            10,                            // 每秒10个请求
            Duration.ofMinutes(2),         // 2分钟超时
            Duration.ofSeconds(15),        // 15秒请求超时
            Map.of(
                "user_id", "test_user_123",
                "operation", "data_process",
                "metadata", Map.of(
                    "source", "performance_test",
                    "version", "1.0"
                )
            )
        );
        
        AsyncAPITester tester = new AsyncAPITester(config);
        tester.runLoadTest();
    }
}
监控端点
测试运行时，你可以通过以下端点监控状态：

bash
# 健康检查
curl http://localhost:3000/health

# 统计信息
curl http://localhost:3000/stats
这个Java版本利用了Java 21的现代特性，特别是虚拟线程让高并发测试变得非常高效。相比传统线程，虚拟线程的内存开销极小，可以轻松创建成千上万个并发连接。