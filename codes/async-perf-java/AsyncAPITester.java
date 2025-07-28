package com.example.asynctester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 异步API性能测试工具 - Java 21版本
 * 使用虚拟线程和现代Java特性
 */
public class AsyncAPITester {
    
    public record TestConfig(
        String apiUrl,
        int callbackPort,
        int totalRequests,
        int concurrency,
        Integer rateLimit,  // 每秒请求数限制
        Duration callbackTimeout,
        Duration requestTimeout,
        Map<String, Object> payload
    ) {
        public static TestConfig defaultConfig(String apiUrl) {
            return new TestConfig(
                apiUrl,
                3000,
                100,
                10,
                null,
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                Map.of("data", "test data", "priority", "high")
            );
        }
    }
    
    public static class TestResult {
        private final String testId;
        private final Instant startTime;
        private volatile Instant endTime;
        private volatile Duration duration;
        private volatile int requestStatus;
        private volatile Map<String, Object> callbackData;
        private volatile String error;
        private volatile boolean completed = false;
        
        public TestResult(String testId) {
            this.testId = testId;
            this.startTime = Instant.now();
        }
        
        public void complete(Instant endTime, Map<String, Object> callbackData) {
            this.endTime = endTime;
            this.duration = Duration.between(startTime, endTime);
            this.callbackData = callbackData;
            this.completed = true;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public void setRequestStatus(int status) {
            this.requestStatus = status;
        }
        
        // Getters
        public String getTestId() { return testId; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public Duration getDuration() { return duration; }
        public int getRequestStatus() { return requestStatus; }
        public Map<String, Object> getCallbackData() { return callbackData; }
        public String getError() { return error; }
        public boolean isCompleted() { return completed; }
        public boolean hasDuration() { return duration != null; }
    }
    
    private final TestConfig config;
    private final ConcurrentHashMap<String, TestResult> results = new ConcurrentHashMap<>();
    private final AtomicInteger completedTests = new AtomicInteger(0);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private HttpServer callbackServer;
    
    public AsyncAPITester(TestConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor()) // Java 21 虚拟线程
            .connectTimeout(config.requestTimeout())
            .build();
    }
    
    /**
     * 启动回调服务器
     */
    private void startCallbackServer() throws IOException {
        callbackServer = HttpServer.create(new InetSocketAddress(config.callbackPort()), 0);
        
        // 回调接收端点
        callbackServer.createContext("/callback", new CallbackHandler());
        
        // 健康检查端点
        callbackServer.createContext("/health", new HealthHandler());
        
        // 统计信息端点
        callbackServer.createContext("/stats", new StatsHandler());
        
        // 使用虚拟线程执行器
        callbackServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        callbackServer.start();
        
        System.out.println("🚀 回调服务器已启动，端口: " + config.callbackPort());
    }
    
    /**
     * 回调处理器
     */
    private class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }
            
            String path = exchange.getRequestURI().getPath();
            String testId = path.substring(path.lastIndexOf('/') + 1);
            
            Instant receivedAt = Instant.now();
            
            try {
                // 读取请求体
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> callbackData = objectMapper.readValue(requestBody, Map.class);
                
                TestResult result = results.get(testId);
                if (result != null) {
                    result.complete(receivedAt, callbackData);
                    int completed = completedTests.incrementAndGet();
                    
                    System.out.printf("✅ 测试 %s 完成，耗时: %d ms (总完成: %d)%n", 
                        testId, result.getDuration().toMillis(), completed);
                }
                
                sendResponse(exchange, 200, Map.of("status", "received", "testId", testId));
                
            } catch (Exception e) {
                System.err.println("❌ 处理回调失败: " + e.getMessage());
                sendResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }
    
    /**
     * 健康检查处理器
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = Map.of(
                "status", "ok",
                "completed", completedTests.get(),
                "total", results.size()
            );
            sendResponse(exchange, 200, response);
        }
    }
    
    /**
     * 统计信息处理器
     */
    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long completed = results.values().stream().mapToLong(r -> r.isCompleted() ? 1 : 0).sum();
            long total = results.size();
            
            Map<String, Object> response = Map.of(
                "totalRequests", total,
                "completed", completed,
                "pending", total - completed,
                "completionRate", total > 0 ? (double) completed / total : 0.0
            );
            sendResponse(exchange, 200, response);
        }
    }
    
    /**
     * 发送HTTP响应
     */
    private void sendResponse(HttpExchange exchange, int statusCode, Object responseData) throws IOException {
        String jsonResponse = objectMapper.writeValueAsString(responseData);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * 发送单个异步请求
     */
    private CompletableFuture<Void> sendRequest(String testId) {
        TestResult result = new TestResult(testId);
        results.put(testId, result);
        
        // 构建请求载荷
        Map<String, Object> requestPayload = new HashMap<>(config.payload());
        requestPayload.put("callback_url", 
            "http://localhost:" + config.callbackPort() + "/callback/" + testId);
        requestPayload.put("test_id", testId);
        
        try {
            String jsonPayload = objectMapper.writeValueAsString(requestPayload);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.apiUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(config.requestTimeout())
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    result.setRequestStatus(response.statusCode());
                    System.out.printf("📤 发送请求 %s，状态: %d%n", testId, response.statusCode());
                })
                .exceptionally(throwable -> {
                    String errorMessage = throwable.getMessage();
                    result.setError(errorMessage);
                    System.err.printf("❌ 请求 %s 失败: %s%n", testId, errorMessage);
                    return null;
                });
                
        } catch (Exception e) {
            result.setError(e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * 运行负载测试
     */
    public void runLoadTest() throws Exception {
        // 启动回调服务器
        startCallbackServer();
        
        System.out.printf("🎯 开始负载测试：%d 个请求，%d 并发%n", 
            config.totalRequests(), config.concurrency());
        
        Instant startTime = Instant.now();
        
        // 使用信号量控制并发
        Semaphore semaphore = new Semaphore(config.concurrency());
        
        // 使用虚拟线程执行器
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < config.totalRequests(); i++) {
                String testId = "test_" + System.currentTimeMillis() + "_" + i;
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        semaphore.acquire();
                        sendRequest(testId).join();
                        
                        // 如果设置了请求频率限制
                        if (config.rateLimit() != null) {
                            Thread.sleep(1000 / config.rateLimit());
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release();
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // 等待所有请求发送完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        
        // 等待所有回调完成
        System.out.println("⏳ 等待所有回调完成...");
        waitForCompletion();
        
        Duration totalTime = Duration.between(startTime, Instant.now());
        generateReport(totalTime);
        
        // 关闭服务器
        callbackServer.stop(0);
        httpClient.close();
    }
    
    /**
     * 等待所有回调完成
     */
    private void waitForCompletion() throws InterruptedException {
        Instant deadline = Instant.now().plus(config.callbackTimeout());
        
        while (completedTests.get() < config.totalRequests() && Instant.now().isBefore(deadline)) {
            Thread.sleep(1000);
            int pending = config.totalRequests() - completedTests.get();
            System.out.printf("⏳ 等待中... 剩余 %d 个回调%n", pending);
        }
        
        if (completedTests.get() < config.totalRequests()) {
            System.out.println("⚠️  等待超时，生成部分结果报告");
        }
    }
    
    /**
     * 生成性能测试报告
     */
    private void generateReport(Duration totalTime) {
        List<TestResult> allResults = new ArrayList<>(results.values());
        List<TestResult> completed = allResults.stream()
            .filter(TestResult::isCompleted)
            .collect(Collectors.toList());
        List<TestResult> failed = allResults.stream()
            .filter(r -> r.getError() != null)
            .collect(Collectors.toList());
        
        if (completed.isEmpty()) {
            System.out.println("❌ 没有完成的测试");
            return;
        }
        
        // 计算统计数据
        List<Long> durations = completed.stream()
            .map(r -> r.getDuration().toMillis())
            .sorted()
            .collect(Collectors.toList());
        
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        long minDuration = durations.get(0);
        long maxDuration = durations.get(durations.size() - 1);
        
        // 计算百分位数
        long p50 = durations.get((int) (durations.size() * 0.5));
        long p95 = durations.get((int) (durations.size() * 0.95));
        long p99 = durations.get((int) (durations.size() * 0.99));
        
        double throughput = (double) completed.size() / totalTime.toSeconds();
        
        // 输出报告
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 异步API性能测试报告");
        System.out.println("=".repeat(60));
        System.out.println("测试时间: " + Instant.now());
        System.out.println("API地址: " + config.apiUrl());
        System.out.println("总耗时: " + totalTime.toSeconds() + "秒");
        System.out.println("并发数: " + config.concurrency());
        System.out.println();
        
        System.out.println("📈 请求统计:");
        System.out.println("  总请求数: " + config.totalRequests());
        System.out.println("  完成数: " + completed.size());
        System.out.println("  失败数: " + failed.size());
        System.out.printf("  成功率: %.2f%%%n", (double) completed.size() / config.totalRequests() * 100);
        System.out.printf("  吞吐量: %.2f 请求/秒%n", throughput);
        System.out.println();
        
        System.out.println("⏱️  端到端延迟统计 (ms):");
        System.out.printf("  平均值: %.2f%n", avgDuration);
        System.out.println("  最小值: " + minDuration);
        System.out.println("  最大值: " + maxDuration);
        System.out.println("  中位数: " + p50);
        System.out.println("  P95: " + p95);
        System.out.println("  P99: " + p99);
        
        if (!failed.isEmpty()) {
            System.out.println("\n❌ 失败详情 (" + failed.size() + " 个):");
            failed.stream().limit(10).forEach(result -> 
                System.out.println("  " + result.getTestId() + ": " + result.getError()));
            if (failed.size() > 10) {
                System.out.println("  ... 还有 " + (failed.size() - 10) + " 个失败");
            }
        }
        
        System.out.println("=".repeat(60));
    }
    
    /**
     * 主方法 - 示例用法
     */
    public static void main(String[] args) throws Exception {
        TestConfig config = new TestConfig(
            "https://httpbin.org/delay/2",  // 测试用延迟API
            3000,                           // 回调端口
            50,                            // 总请求数
            5,                             // 并发数
            2,                             // 每秒最多2个请求
            Duration.ofSeconds(30),        // 30秒回调超时
            Duration.ofSeconds(10),        // 10秒请求超时
            Map.of(                        // 请求载荷
                "data", "test data",
                "priority", "high",
                "user_id", "test_user"
            )
        );
        
        AsyncAPITester tester = new AsyncAPITester(config);
        tester.runLoadTest();
    }
}