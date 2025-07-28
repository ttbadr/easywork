import asyncio
import aiohttp
import time
import json
import statistics
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor
from flask import Flask, request, jsonify
import threading
import uuid
from dataclasses import dataclass, field
from typing import Dict, List, Optional
import logging

@dataclass
class TestResult:
    test_id: str
    start_time: float
    end_time: Optional[float] = None
    duration: Optional[float] = None
    request_status: Optional[int] = None
    callback_data: Optional[dict] = None
    error: Optional[str] = None
    completed: bool = False

@dataclass
class TestConfig:
    api_url: str
    callback_port: int = 3000
    total_requests: int = 100
    concurrency: int = 10
    rate_limit: Optional[int] = None  # 每秒请求数限制
    callback_timeout: int = 30  # 等待回调超时时间(秒)
    request_timeout: int = 5  # 单个请求超时时间(秒)
    payload: Dict = field(default_factory=dict)

class AsyncAPITester:
    def __init__(self, config: TestConfig):
        self.config = config
        self.results: Dict[str, TestResult] = {}
        self.completed_tests = 0
        self.app = Flask(__name__)
        self.server_thread = None
        self.setup_callback_server()
        
        # 设置日志
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s'
        )
        self.logger = logging.getLogger(__name__)
    
    def setup_callback_server(self):
        """设置Flask回调服务器"""
        
        @self.app.route(f'/callback/<test_id>', methods=['POST'])
        def callback_handler(test_id):
            received_at = time.time()
            
            if test_id in self.results:
                result = self.results[test_id]
                result.end_time = received_at
                result.duration = (received_at - result.start_time) * 1000  # 转换为毫秒
                result.callback_data = request.get_json()
                result.completed = True
                
                self.completed_tests += 1
                self.logger.info(f"✅ 测试 {test_id} 完成，耗时: {result.duration:.2f}ms")
            
            return jsonify({'status': 'received', 'test_id': test_id})
        
        @self.app.route('/health', methods=['GET'])
        def health_check():
            return jsonify({
                'status': 'ok', 
                'completed': self.completed_tests,
                'total': len(self.results)
            })
        
        @self.app.route('/stats', methods=['GET'])
        def get_stats():
            completed = [r for r in self.results.values() if r.completed]
            return jsonify({
                'total_requests': len(self.results),
                'completed': len(completed),
                'pending': len(self.results) - len(completed),
                'completion_rate': len(completed) / len(self.results) if self.results else 0
            })
    
    def start_callback_server(self):
        """在后台线程启动Flask服务器"""
        def run_server():
            self.app.run(
                host='0.0.0.0', 
                port=self.config.callback_port, 
                debug=False,
                use_reloader=False
            )
        
        self.server_thread = threading.Thread(target=run_server, daemon=True)
        self.server_thread.start()
        
        # 等待服务器启动
        time.sleep(2)
        self.logger.info(f"🚀 回调服务器启动在端口 {self.config.callback_port}")
    
    async def send_request(self, session: aiohttp.ClientSession, test_id: str):
        """发送单个异步请求"""
        start_time = time.time()
        
        # 记录测试开始
        result = TestResult(test_id=test_id, start_time=start_time)
        self.results[test_id] = result
        
        # 构建请求载荷
        payload = {
            **self.config.payload,
            'callback_url': f'http://localhost:{self.config.callback_port}/callback/{test_id}',
            'test_id': test_id
        }
        
        try:
            async with session.post(
                self.config.api_url,
                json=payload,
                timeout=aiohttp.ClientTimeout(total=self.config.request_timeout)
            ) as response:
                result.request_status = response.status
                self.logger.info(f"📤 发送请求 {test_id}，状态: {response.status}")
                
        except Exception as e:
            error_msg = str(e)
            result.error = error_msg
            self.logger.error(f"❌ 请求 {test_id} 失败: {error_msg}")
    
    async def run_load_test(self):
        """运行负载测试"""
        # 启动回调服务器
        self.start_callback_server()
        
        self.logger.info(f"🎯 开始负载测试：{self.config.total_requests} 个请求，{self.config.concurrency} 并发")
        
        start_time = time.time()
        
        # 创建HTTP会话
        connector = aiohttp.TCPConnector(limit=self.config.concurrency)
        async with aiohttp.ClientSession(connector=connector) as session:
            
            # 创建信号量控制并发
            semaphore = asyncio.Semaphore(self.config.concurrency)
            
            async def bounded_request(test_id: str):
                async with semaphore:
                    await self.send_request(session, test_id)
                    
                    # 如果设置了请求频率限制
                    if self.config.rate_limit:
                        await asyncio.sleep(1.0 / self.config.rate_limit)
            
            # 生成所有测试任务
            tasks = []
            for i in range(self.config.total_requests):
                test_id = f"test_{int(time.time() * 1000)}_{i}"
                task = asyncio.create_task(bounded_request(test_id))
                tasks.append(task)
            
            # 执行所有请求
            await asyncio.gather(*tasks)
        
        # 等待所有回调完成
        self.logger.info('⏳ 等待所有回调完成...')
        await self.wait_for_completion()
        
        total_time = time.time() - start_time
        self.generate_report(total_time)
    
    async def wait_for_completion(self):
        """等待所有回调完成"""
        max_wait_time = self.config.callback_timeout
        start_wait = time.time()
        
        while self.completed_tests < self.config.total_requests:
            if time.time() - start_wait > max_wait_time:
                self.logger.warning('⚠️  等待超时，生成部分结果报告')
                break
            
            await asyncio.sleep(1)
            pending = self.config.total_requests - self.completed_tests
            self.logger.info(f'⏳ 等待中... 剩余 {pending} 个回调')
    
    def generate_report(self, total_time: float):
        """生成性能测试报告"""
        results = list(self.results.values())
        completed = [r for r in results if r.completed]
        failed = [r for r in results if r.error]
        
        if not completed:
            self.logger.error('❌ 没有完成的测试')
            return
        
        durations = [r.duration for r in completed]
        
        # 计算统计数据
        avg_duration = statistics.mean(durations)
        min_duration = min(durations)
        max_duration = max(durations)
        
        # 计算百分位数
        sorted_durations = sorted(durations)
        p50 = statistics.median(sorted_durations)
        p95 = sorted_durations[int(len(sorted_durations) * 0.95)]
        p99 = sorted_durations[int(len(sorted_durations) * 0.99)]
        
        # 计算吞吐量
        throughput = len(completed) / total_time
        
        print('\n' + '='*60)
        print('📊 异步API性能测试报告')
        print('='*60)
        print(f'测试时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}')
        print(f'API地址: {self.config.api_url}')
        print(f'总耗时: {total_time:.2f}秒')
        print(f'并发数: {self.config.concurrency}')
        print()
        
        print('📈 请求统计:')
        print(f'  总请求数: {self.config.total_requests}')
        print(f'  完成数: {len(completed)}')
        print(f'  失败数: {len(failed)}')
        print(f'  成功率: {len(completed)/self.config.total_requests*100:.2f}%')
        print(f'  吞吐量: {throughput:.2f} 请求/秒')
        print()
        
        print('⏱️  端到端延迟统计 (ms):')
        print(f'  平均值: {avg_duration:.2f}')
        print(f'  最小值: {min_duration:.2f}')
        print(f'  最大值: {max_duration:.2f}')
        print(f'  中位数: {p50:.2f}')
        print(f'  P95: {p95:.2f}')
        print(f'  P99: {p99:.2f}')
        
        if failed:
            print(f'\n❌ 失败详情 ({len(failed)} 个):')
            for i, result in enumerate(failed[:10]):  # 只显示前10个
                print(f'  {i+1}. {result.test_id}: {result.error}')
            if len(failed) > 10:
                print(f'  ... 还有 {len(failed)-10} 个失败')
        
        print('='*60)
        
        # 保存详细结果到文件
        self.save_detailed_results(completed, failed, total_time)
    
    def save_detailed_results(self, completed: List[TestResult], failed: List[TestResult], total_time: float):
        """保存详细结果到JSON文件"""
        results_data = {
            'test_config': {
                'api_url': self.config.api_url,
                'total_requests': self.config.total_requests,
                'concurrency': self.config.concurrency,
                'rate_limit': self.config.rate_limit
            },
            'summary': {
                'total_time': total_time,
                'completed_count': len(completed),
                'failed_count': len(failed),
                'success_rate': len(completed) / self.config.total_requests,
                'throughput': len(completed) / total_time
            },
            'completed_tests': [
                {
                    'test_id': r.test_id,
                    'duration_ms': r.duration,
                    'request_status': r.request_status
                } for r in completed
            ],
            'failed_tests': [
                {
                    'test_id': r.test_id,
                    'error': r.error
                } for r in failed
            ]
        }
        
        filename = f'async_api_test_results_{int(time.time())}.json'
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(results_data, f, indent=2, ensure_ascii=False)
        
        print(f'📄 详细结果已保存到: {filename}')

# 使用示例
async def main():
    config = TestConfig(
        api_url='https://httpbin.org/delay/2',  # 测试用的延迟API
        callback_port=3000,
        total_requests=50,      # 总请求数
        concurrency=5,          # 并发数
        rate_limit=2,           # 每秒最多2个请求
        callback_timeout=30,    # 30秒回调超时
        request_timeout=10,     # 10秒请求超时
        payload={               # 请求载荷
            'data': 'test data',
            'priority': 'high'
        }
    )
    
    tester = AsyncAPITester(config)
    await tester.run_load_test()

if __name__ == '__main__':
    # 运行测试
    asyncio.run(main())