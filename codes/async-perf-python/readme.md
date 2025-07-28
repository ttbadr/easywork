安装依赖
bash
pip install aiohttp flask asyncio
使用方法
基本使用
python
# 直接运行
python async_api_tester.py
自定义配置
python
import asyncio
from async_api_tester import AsyncAPITester, TestConfig

async def custom_test():
    config = TestConfig(
        api_url='https://your-api.com/async-endpoint',
        callback_port=3001,
        total_requests=100,
        concurrency=10,
        rate_limit=5,  # 每秒5个请求
        callback_timeout=60,  # 60秒超时
        payload={
            'user_id': 'test_user',
            'action': 'process_data',
            'data': {'key': 'value'}
        }
    )
    
    tester = AsyncAPITester(config)
    await tester.run_load_test()

# 运行自定义测试
asyncio.run(custom_test())
Python版本的优势
更简洁的语法：使用dataclass和type hints
异步支持更好：原生的asyncio和aiohttp
统计功能更强：内置statistics模块
错误处理更优雅：Python的异常处理机制
数据分析友好：结果可以直接用pandas分析
扩展功能
如果你需要更高级的功能，可以这样扩展：

python
# 添加到主类中
class AsyncAPITester:
    # ... 现有代码 ...
    
    def export_to_csv(self):
        """导出结果到CSV"""
        import pandas as pd
        
        completed = [r for r in self.results.values() if r.completed]
        df = pd.DataFrame([
            {
                'test_id': r.test_id,
                'duration_ms': r.duration,
                'request_status': r.request_status,
                'timestamp': r.start_time
            } for r in completed
        ])
        
        filename = f'results_{int(time.time())}.csv'
        df.to_csv(filename, index=False)
        print(f'📊 结果已导出到: {filename}')
    
    def plot_results(self):
        """绘制性能图表"""
        import matplotlib.pyplot as plt
        
        completed = [r for r in self.results.values() if r.completed]
        durations = [r.duration for r in completed]
        
        plt.figure(figsize=(12, 4))
        
        # 延迟分布图
        plt.subplot(1, 2, 1)
        plt.hist(durations, bins=20, alpha=0.7)
        plt.xlabel('响应时间 (ms)')
        plt.ylabel('频次')
        plt.title('响应时间分布')
        
        # 时序图
        plt.subplot(1, 2, 2)
        timestamps = [(r.start_time - min(r.start_time for r in completed)) for r in completed]
        plt.scatter(timestamps, durations, alpha=0.6)
        plt.xlabel('时间 (秒)')
        plt.ylabel('响应时间 (ms)')
        plt.title('响应时间时序图')
        
        plt.tight_layout()
        plt.savefig(f'performance_chart_{int(time.time())}.png')
        plt.show()
这个Python版本比Node.js版本更简洁，异步处理更优雅，而且扩展性更好。你觉得怎么样？需要我解释某个部分或者添加其他功能吗？