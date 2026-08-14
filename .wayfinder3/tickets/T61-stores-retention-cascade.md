---
id: T61
title: stores · 会话级联清理与保留策略族
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

「只进不出」如何治理？需裁决：① deleteSession 级联清理的 SPI 形状与覆盖面（messages/summaries/state/lease/spans/events/snapshots/tool_call_log/run_registry + spill 文件 + 向量缓存）；② SessionHistoryPolicy（封闭才计时、默认保留 PT72H、改短不追溯，Temporal 语义）；③ ObservabilityTtl（event/span TTL PT7D、sweep 节奏 PT4H、批量删，ClickHouse 语义）；④ MaintenanceTrigger 触发公式（PG autovacuum 四件套）；⑤ 摘要旧版本修剪、ToolCallLog 保留窗口（etcd compaction 语义：恢复窗口外可聚合）；⑥ 清理执行器归属（统一后台 sweeper vs 各 store 自持）。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §stores-6**：SPI 增 deleteSession（default no-op）+ prune(policy)；core SessionCleaner 协调器一次级联（含 spill 文件与 embedding 缓存）；RetentionSweeper 后台执行器（默认 PT1H 可关）；SessionHistoryPolicy 锚点=closedAt、默认 PT72H、改短不追溯（Temporal）；观测 TTL PT7D 批量限删（ClickHouse）；摘要保留最近 K=3 版；ToolCallLog 窗口 PT7D；MaintenanceTrigger 公式（base 50 + 0.2×N + 封顶 + hardFloor，PG autovacuum）。
