# 微服务Request耗时分析Dashboard

## 1. 动态发现服务 - 基于app label查找涉及的服务

```splunk
index=your_index request_id="$request_id$" 
| stats earliest(_time) as earliest_time by app
| eval earliest_time_readable=strftime(earliest_time, "%Y-%m-%d %H:%M:%S.%3N")
| sort earliest_time
| eval service_order=mvrange(1, mvcount(app)+1)
```

## 2. 服务发现与验证查询

```splunk
index=your_index request_id="$request_id$" 
| stats earliest(_time) as start_time, latest(_time) as end_time, count as log_count by app
| eval duration_ms=round((end_time-start_time)*1000, 2)
| eval start_time_readable=strftime(start_time, "%Y-%m-%d %H:%M:%S.%3N")
| eval end_time_readable=strftime(end_time, "%Y-%m-%d %H:%M:%S.%3N")
| sort start_time
| table app, start_time_readable, end_time_readable, log_count, duration_ms
| rename app as "服务名称", start_time_readable as "首次出现", end_time_readable as "最后出现", log_count as "日志数量", duration_ms as "服务内耗时(ms)"
```

## 3. 计算服务间耗时的完整查询（动态发现版本）

```splunk
index=your_index request_id="$request_id$" 
| stats earliest(_time) as start_time by app
| sort start_time
| streamstats current=f last(start_time) as prev_time, last(app) as prev_service
| eval processing_time=if(isnull(prev_time), 0, start_time-prev_time)
| eval processing_time_ms=round(processing_time*1000, 2)
| eval start_time_readable=strftime(start_time, "%Y-%m-%d %H:%M:%S.%3N")
| eval processing_time_readable=if(processing_time=0, "起始服务", processing_time_ms." ms")
| eval processed_by=if(isnull(prev_service), "N/A", prev_service)
| table app, start_time_readable, processing_time_readable, processing_time_ms, processed_by
| rename app as "当前服务", processed_by as "处理耗时归属服务"
```

## 4. 两阶段查询方案 - 先发现服务再计算耗时

### 阶段1：发现所有相关服务
```splunk
index=your_index request_id="$request_id$" 
| stats count by app 
| sort app
| eval service_list=mvjoin(mvappend(app), ",")
| stats list(app) as all_services, values(service_list) as service_summary
```

### 阶段2：基于发现的服务计算耗时
```splunk
index=your_index request_id="$request_id$" 
| stats earliest(_time) as start_time, latest(_time) as end_time, count as event_count by app
| sort start_time
| eval service_sequence=mvrange(1, mvcount(app)+1)
| streamstats current=f last(start_time) as prev_start_time, last(app) as prev_app
| eval inter_service_time=if(isnull(prev_start_time), 0, start_time-prev_start_time)
| eval inter_service_time_ms=round(inter_service_time*1000, 2)
| eval intra_service_time_ms=round((end_time-start_time)*1000, 2)
| eval start_time_readable=strftime(start_time, "%Y-%m-%d %H:%M:%S.%3N")
| eval end_time_readable=strftime(end_time, "%Y-%m-%d %H:%M:%S.%3N")
| eval inter_service_desc=if(inter_service_time=0, "请求开始", prev_app." → ".app." : ".inter_service_time_ms." ms")
| table app, start_time_readable, end_time_readable, event_count, inter_service_time_ms, intra_service_time_ms, inter_service_desc
| rename app as "服务", start_time_readable as "开始时间", end_time_readable as "结束时间", event_count as "日志条数", inter_service_time_ms as "服务间耗时(ms)", intra_service_time_ms as "服务内耗时(ms)", inter_service_desc as "流转描述"
```

## 3. Dashboard XML配置

```xml
<dashboard>
  <label>微服务Request链路耗时分析</label>
  
  <fieldset submitButton="true" autoRun="false">
    <input type="text" token="request_id">
      <label>Request ID</label>
      <default>your-request-id-here</default>
    </input>
    
    <input type="time" token="time_picker">
      <label>时间范围</label>
      <default>
        <earliest>-24h@h</earliest>
        <latest>now</latest>
      </default>
    </input>
  </fieldset>

  <row>
    <panel>
      <title>服务发现 - 涉及的所有服务</title>
      <table>
        <search>
          <query>
            index=your_index request_id="$request_id$" $time_picker.earliest$ $time_picker.latest$
            | stats earliest(_time) as start_time, latest(_time) as end_time, count as log_count by app
            | eval duration_ms=round((end_time-start_time)*1000, 2)
            | eval start_time_readable=strftime(start_time, "%Y-%m-%d %H:%M:%S.%3N")
            | eval end_time_readable=strftime(end_time, "%Y-%m-%d %H:%M:%S.%3N")
            | sort start_time
            | table app, start_time_readable, end_time_readable, log_count, duration_ms
            | rename app as "服务名称", start_time_readable as "首次出现", end_time_readable as "最后出现", log_count as "日志数量", duration_ms as "服务内耗时(ms)"
          </query>
        </search>
      </table>
    </panel>
  </row>

  <row>
    <panel>
      <title>服务调用链路时序图</title>
      <viz type="timeline_viz">
        <search>
          <query>
            index=your_index request_id="$request_id$" $time_picker.earliest$ $time_picker.latest$
            | stats earliest(_time) as start_time by app
            | sort start_time
            | streamstats current=f last(start_time) as prev_time
            | eval processing_time=if(isnull(prev_time), 0, start_time-prev_time)
            | eval processing_time_ms=round(processing_time*1000, 2)
            | eval start_time_readable=strftime(start_time, "%Y-%m-%d %H:%M:%S.%3N")
          </query>
        </search>
      </viz>
    </panel>
  </row>

  <row>
    <panel>
      <title>服务间耗时分析表</title>
      <table>
        <search>
          <query>
            index=your_index request_id="$request_id$" $time_picker.earliest$ $time_picker.latest$
            | stats earliest(_time) as start_time, latest(_time) as end_time, count as event_count by app
            | sort start_time
            | streamstats current=f last(start_time) as prev_start_time, last(app) as prev_app
            | eval inter_service_time=if(isnull(prev_start_time), 0, start_time-prev_start_time)
            | eval inter_service_time_ms=round(inter_service_time*1000, 2)
            | eval intra_service_time_ms=round((end_time-start_time)*1000, 2)
            | eval start_time_readable=strftime(start_time, "%Y-%m-%d %H:%M:%S.%3N")
            | eval inter_service_desc=if(inter_service_time=0, "请求开始", prev_app." → ".app." : ".inter_service_time_ms." ms")
            | table app, start_time_readable, event_count, inter_service_time_ms, intra_service_time_ms, inter_service_desc
            | rename app as "服务", start_time_readable as "开始时间", event_count as "日志条数", inter_service_time_ms as "服务间耗时(ms)", intra_service_time_ms as "服务内耗时(ms)", inter_service_desc as "流转描述"
          </query>
        </search>
      </table>
    </panel>
  </row>

  <row>
    <panel>
      <title>服务间耗时分布柱状图</title>
      <viz type="column_chart">
        <search>
          <query>
            index=your_index request_id="$request_id$" $time_picker.earliest$ $time_picker.latest$
            | stats earliest(_time) as start_time by app
            | sort start_time
            | streamstats current=f last(start_time) as prev_time
            | eval processing_time=if(isnull(prev_time), 0, start_time-prev_time)
            | eval processing_time_ms=round(processing_time*1000, 2)
            | where processing_time_ms > 0
            | table app, processing_time_ms
          </query>
        </search>
        <option name="charting.axisTitleX.text">服务</option>
        <option name="charting.axisTitleY.text">处理耗时 (ms)</option>
        <option name="charting.chart">column</option>
        <option name="charting.legend.placement">none</option>
      </viz>
    </panel>
  </row>

  <row>
    <panel>
      <title>总体性能指标</title>
      <single>
        <search>
          <query>
            index=your_index request_id="$request_id$" $time_picker.earliest$ $time_picker.latest$
            | stats earliest(_time) as start_time, latest(_time) as end_time by app
            | stats min(start_time) as overall_start, max(end_time) as overall_end
            | eval total_time_ms=round((overall_end-overall_start)*1000, 2)
            | eval total_time_readable=total_time_ms." ms"
            | fields total_time_readable
          </query>
        </search>
        <option name="drilldown">none</option>
        <option name="refresh.display">progressbar</option>
        <option name="underLabel">总耗时</option>
      </single>
    </panel>
  </row>
</dashboard>
```

## 5. 高级查询 - 支持多个Request ID比较（动态发现版本）

```splunk
index=your_index (request_id="req-001" OR request_id="req-002" OR request_id="req-003")
| stats earliest(_time) as start_time by request_id, app
| sort request_id, start_time
| streamstats current=f last(start_time) as prev_time by request_id
| eval processing_time=if(isnull(prev_time), 0, start_time-prev_time)
| eval processing_time_ms=round(processing_time*1000, 2)
| where processing_time_ms > 0
| stats avg(processing_time_ms) as avg_time, 
        min(processing_time_ms) as min_time, 
        max(processing_time_ms) as max_time by app
| eval avg_time=round(avg_time, 2)
| sort avg_time desc
```

## 6. 服务链路发现与验证查询

```splunk
# 查看某个request_id涉及了哪些app，以及调用顺序
index=your_index request_id="$request_id$" 
| stats earliest(_time) as first_seen, latest(_time) as last_seen, count as total_events by app
| sort first_seen
| eval sequence_order=mvrange(1, mvcount(app)+1)
| eval first_seen_readable=strftime(first_seen, "%H:%M:%S.%3N")
| eval last_seen_readable=strftime(last_seen, "%H:%M:%S.%3N") 
| eval duration_in_service=round((last_seen-first_seen)*1000, 2)
| table sequence_order, app, first_seen_readable, last_seen_readable, total_events, duration_in_service
| rename sequence_order as "调用顺序", app as "服务名", first_seen_readable as "首次出现", last_seen_readable as "最后出现", total_events as "日志总数", duration_in_service as "停留时长(ms)"
```

## 7. 实时监控查询 - 动态发现版本

```splunk
index=your_index request_id="$request_id$" 
| stats earliest(_time) as start_time by app
| sort start_time
| streamstats current=f last(start_time) as prev_time
| eval processing_time=if(isnull(prev_time), 0, start_time-prev_time)
| eval processing_time_ms=round(processing_time*1000, 2)
| eval status=case(
    processing_time_ms = 0, "起始",
    processing_time_ms < 100, "正常",
    processing_time_ms < 500, "注意", 
    processing_time_ms >= 500, "异常"
)
| eval start_time_readable=strftime(start_time, "%H:%M:%S.%3N")
| table app, start_time_readable, processing_time_ms, status
| rename app as "服务", start_time_readable as "开始时间", processing_time_ms as "耗时(ms)", status as "状态"
```

## 8. 服务依赖关系分析

```splunk
# 分析某个时间段内，不同request_id的服务调用模式
index=your_index earliest=-1h latest=now 
| stats dc(request_id) as request_count, 
        values(request_id) as sample_requests by app
| sort request_count desc
| eval sample_requests=mvjoin(mvindex(sample_requests, 0, 2), ", ")
| table app, request_count, sample_requests  
| rename app as "服务名", request_count as "处理请求数", sample_requests as "示例请求ID"
```

## 使用说明：

1. **修改索引名称**：将 `index=your_index` 替换为你实际的索引名
2. **调整服务识别规则**：根据你的日志格式修改 `service` 字段的 `case` 语句
3. **设置Request ID字段**：确保你的日志中包含 `request_id` 字段
4. **时间字段**：确保使用正确的时间字段（通常是 `_time`）

这个方案可以：
- 追踪单个request在各个微服务中的流转时间
- 计算每个服务的实际处理时间
- 提供可视化的时间线和性能分析
- 支持实时监控和历史数据分析