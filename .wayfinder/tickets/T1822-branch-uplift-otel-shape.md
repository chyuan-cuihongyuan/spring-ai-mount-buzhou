---
id: T1822
title: R8 分支缺口批次 1 选题与形态（observe-otel 本地可测靶点 + store-redis 环境约束入档）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 8 轮：R7 裁决的「store-redis / observe-otel 分支缺口批次」依据逐类分支数据如何选题？store-redis 缺口大头（covered=0 的容器门控类）在无 Docker 环境如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 8 轮 = effort #1207 / spec 1207 / impl 910）：

1. **选题证据（R7 worktree jacoco.xml 逐类 BRANCH 聚合）**：store-redis 缺口大头 = RedisSemanticVectorCache（missed 84/covered 0）、RedisSessionIndexStore（42/0）、RedisBulkheadStateBackend（42/0）——全部容器门控（Testcontainers，无 Docker 即 skip），本地不可 uplift；observe-otel = OtelBridgeSink（41/65，本地 InMemorySpanExporter hermetic 可测）+ OtelProperties（7/17，纯构造分支）。
2. **R8 靶点 = observe-otel 两类**（本地可测性是硬选题门槛——证据在本地产出）；store-redis 缺口**环境约束入档**：其 uplift 依赖 Docker 在场（CI）或 fake 化（已被 T1809 否决——60+ 方法 stub 是 Mockito 手工复刻），无 Docker 环境不硬凑，BRANCH 硬门开启评估时按「容器门控缺口」单列口径。
3. **OtelBridgeSink 补测面（按未覆盖分支行号精确制导）**：重复开启防御 end、驱逐护栏（4 参构造 maxOpenSpans=1）、spanName 全 kind 回退分支、sessionTrace null/blank/上界驱逐、属性类型适配器（Boolean/Float/Double/Integer/Long/对象/null）、空/缺 payload 事件、无 spanId 事件丢弃、endedAt=null、旁路故障隔离吞 RuntimeException（Proxy Tracer 抛错 ×100 覆盖限频分支）；防御性不可达分支（SpanRecord compact 已归一 attributes 的 null 检查）诚实记录不硬凑。
4. **OtelProperties 补测面**：全 null 默认、blank 回退、headers 防御拷贝、非法 exporter-mode fail-fast、非正 timeout fail-fast。
5. **边界**：不改主代码；主代码缺陷单列票。
