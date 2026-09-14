# 1207 — R8 分支缺口批次 1：observe-otel（OtelBridgeSink × OtelProperties）

> 来源：K 会话第 8 轮 = effort #1207（[T1822](../../.wayfinder/tickets/T1822-branch-uplift-otel-shape.md) / [T1823](../../.wayfinder/tickets/T1823-branch-uplift-otel-verify.md) / impl 910）。方法论：**mutation-testing 式分支定向断言**（PIT 精神——每个分支两侧都有可观察行为，缺口行号从 jacoco.xml 逐类提取，测的是分支语义不是行数）。

## Problem Statement

R7 BRANCH 读面台账（13 模块实测）暴露 store-redis（52.1%）与 observe-otel（62.9%）为分支缺口最大模块。逐类下钻：store-redis 缺口大头为容器门控类（RedisSemanticVectorCache missed 84 / RedisSessionIndexStore 42 / RedisBulkheadStateBackend 42，全部 covered=0——Testcontainers 无 Docker 即 skip），本地不可 uplift；observe-otel 的 OtelBridgeSink（41 missed/65 covered）与 OtelProperties（7/17）可本地 hermetic 补测。

## 目标

- **OtelBridgeSinkBranchTest**（10 用例，InMemorySpanExporter + SimpleSpanProcessor hermetic 断言）：
  - 重复开启防御：同 spanId 两次 RUNNING → 旧 span 防御 end（不算驱逐）；
  - 驱逐护栏：4 参构造 maxOpenSpans=1 → 触发驱逐（evictedSpans=1 + buzhou.evicted 标记 + end）；
  - spanName 全 kind 回退：HARNESS_INTERNAL（有/无 action、name 带/不带 internal: 前缀、name null）、MODEL_CALL 无 model.name → chat、TOOL_CALL 无 tool.name → execute_tool、未知 kind → record.name()；
  - sessionTrace 回退与上界：null/blank → unknown 同派生；maxSessionTraces=1 驱逐重建无损；
  - 属性类型适配器：Boolean/Float/Double/Integer/Long/自定义对象/null 值/内容键门控（include-content=false）；
  - 空 payload 与无 spanId 事件防御；endedAt=null → end(now) 回退；未知 status → OK；
  - 故障隔离：Proxy Tracer spanBuilder 抛错 ×100 → 吞掉不抛穿（PipelineSink 契约 + 限频日志分支）。
- **OtelPropertiesTest**（6 用例）：全 null 默认链、blank 回退、headers 防御拷贝、显式值保留、非法 exporter-mode fail-fast、非正 timeout fail-fast。

## 实现决策

- 测试与被测类同包（OtelBridgeSink 包私有 + 4 参构造可达）；hermetic 用 OTel SDK 自带 InMemorySpanExporter + SimpleSpanProcessor（同步导出，断言确定性）。
- 补测显形主代码缺陷单列修复：**T1824**——sessionTrace 驱逐块 `iterator.remove()` 未先 `next()`，CHM 抛 IllegalStateException 被故障隔离吞掉，超限后每个新会话首个 span 静默丢弃（对照 evictIfOverBudget 同构代码先 next() 写法正确）；最小一行修复 + 语义不变。
- store-redis 缺口环境约束入档：容器门控类本地不可 uplift（fake 化已被 T1809 否决），BRANCH 硬门开启评估按「容器门控缺口」单列口径。

## 测试决策

- 断言只对可观察行为：导出的 span 名/属性/状态/traceId 与 sink 计数器（evictedSpans），不断言内部 Map 状态。
- 防御性不可达分支（SpanRecord/EventRecord compact 构造已归一 null 的下游 null 检查）诚实记录不硬凑。
- 验收门：observe-otel 全量绿 + OtelBridgeSink 分支覆盖 61%→87%、OtelProperties →100%（本轮实测）。

## 兼容性

一行主代码修复（T1824，iterator.next() 补齐，驱逐语义不变——「任意条驱逐非 LRU」Javadoc 语义照旧）+ 纯测试增量。

## Out of Scope

- store-redis 分支缺口（环境约束，入档不硬凑）。
- OtelBridgeSink 剩余 28 missed 中的限频 100 条日志分支（LOG_EVERY=100 需 10 万次调用，成本/价值不匹配——入档豁免）。

## Further Notes

- 「缺口=未执行路径=未验证路径」方法论有效性实证：T1824 驱逐缺陷（护栏从未工作）由分支补测第一轮即显形——R7 的逐类分支数据提取直接命中潜伏缺陷。
