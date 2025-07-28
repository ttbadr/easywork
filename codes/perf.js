const express = require('express');
const axios = require('axios');
const { Worker, isMainThread, parentPort, workerData } = require('worker_threads');

class AsyncAPITester {
    constructor(config) {
        this.config = config;
        this.results = new Map();
        this.completedTests = 0;
        this.app = express();
        this.setupCallbackServer();
    }

    setupCallbackServer() {
        this.app.use(express.json());
        
        // 回调接收端点
        this.app.post('/callback/:testId', (req, res) => {
            const testId = req.params.testId;
            const receivedAt = Date.now();
            
            if (this.results.has(testId)) {
                const result = this.results.get(testId);
                result.endTime = receivedAt;
                result.duration = receivedAt - result.startTime;
                result.callbackData = req.body;
                result.completed = true;
                
                this.completedTests++;
                console.log(`✅ 测试 ${testId} 完成，耗时: ${result.duration}ms`);
            }
            
            res.json({ status: 'received', testId });
        });

        // 健康检查端点
        this.app.get('/health', (req, res) => {
            res.json({ status: 'ok', completed: this.completedTests });
        });
    }

    async startCallbackServer() {
        return new Promise((resolve) => {
            this.server = this.app.listen(this.config.callbackPort, () => {
                console.log(`🚀 回调服务器启动在端口 ${this.config.callbackPort}`);
                resolve();
            });
        });
    }

    async sendRequest(testId) {
        const startTime = Date.now();
        
        // 记录测试开始
        this.results.set(testId, {
            testId,
            startTime,
            completed: false
        });

        try {
            const response = await axios.post(this.config.apiUrl, {
                ...this.config.payload,
                callback_url: `http://localhost:${this.config.callbackPort}/callback/${testId}`,
                test_id: testId
            }, {
                timeout: this.config.requestTimeout || 5000
            });

            this.results.get(testId).requestStatus = response.status;
            console.log(`📤 发送请求 ${testId}，状态: ${response.status}`);
            
        } catch (error) {
            console.error(`❌ 请求 ${testId} 失败:`, error.message);
            this.results.get(testId).error = error.message;
        }
    }

    async runLoadTest() {
        await this.startCallbackServer();
        
        console.log(`🎯 开始负载测试：${this.config.totalRequests} 个请求，${this.config.concurrency} 并发`);
        
        const startTime = Date.now();
        const promises = [];
        
        // 控制并发数
        for (let i = 0; i < this.config.totalRequests; i++) {
            const testId = `test_${Date.now()}_${i}`;
            
            promises.push(this.sendRequest(testId));
            
            // 控制并发数
            if (promises.length >= this.config.concurrency) {
                await Promise.all(promises);
                promises.length = 0;
            }
            
            // 控制请求频率
            if (this.config.rateLimit) {
                await new Promise(resolve => setTimeout(resolve, 1000 / this.config.rateLimit));
            }
        }
        
        // 等待剩余请求
        if (promises.length > 0) {
            await Promise.all(promises);
        }
        
        // 等待回调完成
        console.log('⏳ 等待所有回调完成...');
        await this.waitForCompletion();
        
        const totalTime = Date.now() - startTime;
        this.generateReport(totalTime);
        
        this.server.close();
    }

    async waitForCompletion() {
        const maxWaitTime = this.config.callbackTimeout || 30000;
        const startWait = Date.now();
        
        while (this.completedTests < this.config.totalRequests) {
            if (Date.now() - startWait > maxWaitTime) {
                console.log('⚠️  等待超时，生成部分结果报告');
                break;
            }
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            const pending = this.config.totalRequests - this.completedTests;
            console.log(`⏳ 等待中... 剩余 ${pending} 个回调`);
        }
    }

    generateReport(totalTime) {
        const results = Array.from(this.results.values());
        const completed = results.filter(r => r.completed);
        const failed = results.filter(r => r.error);
        
        if (completed.length === 0) {
            console.log('❌ 没有完成的测试');
            return;
        }

        const durations = completed.map(r => r.duration);
        const avg = durations.reduce((a, b) => a + b, 0) / durations.length;
        const min = Math.min(...durations);
        const max = Math.max(...durations);
        
        // 计算百分位数
        const sorted = durations.sort((a, b) => a - b);
        const p95 = sorted[Math.floor(sorted.length * 0.95)];
        const p99 = sorted[Math.floor(sorted.length * 0.99)];

        console.log('\n📊 性能测试报告');
        console.log('=' .repeat(50));
        console.log(`总请求数: ${this.config.totalRequests}`);
        console.log(`完成数: ${completed.length}`);
        console.log(`失败数: ${failed.length}`);
        console.log(`成功率: ${(completed.length / this.config.totalRequests * 100).toFixed(2)}%`);
        console.log(`总耗时: ${totalTime}ms`);
        console.log('\n⏱️  端到端延迟统计:');
        console.log(`平均值: ${avg.toFixed(2)}ms`);
        console.log(`最小值: ${min}ms`);
        console.log(`最大值: ${max}ms`);
        console.log(`P95: ${p95}ms`);
        console.log(`P99: ${p99}ms`);
        
        if (failed.length > 0) {
            console.log('\n❌ 失败详情:');
            failed.forEach(f => {
                console.log(`${f.testId}: ${f.error}`);
            });
        }
    }
}

// 使用示例
const config = {
    apiUrl: 'https://your-api.com/async-endpoint',
    callbackPort: 3000,
    totalRequests: 100,        // 总请求数
    concurrency: 10,           // 并发数
    rateLimit: 5,              // 每秒请求数限制
    callbackTimeout: 30000,    // 等待回调超时时间
    requestTimeout: 5000,      // 单个请求超时时间
    payload: {                 // 请求载荷
        data: 'test data',
        priority: 'high'
    }
};

// 运行测试
const tester = new AsyncAPITester(config);
tester.runLoadTest().catch(console.error);