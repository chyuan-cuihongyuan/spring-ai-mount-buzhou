# 700 — 工具执行 per-tool 耗时聚合读面

> 来源：G 会话第 1 轮 = effort #700（借鉴 pg_stat_statements / ClickHouse query log——语句级耗时聚合读面）/ [T951](../../.wayfinder/tickets/T951-tool-timing-aggregate-shape.md) / [T952](../../.wayfinder/tickets/T952-tool-timing-aggregate-verify.md) / impl 503。

## 背景

HookedToolCallback 是全部机制工具的统一执行点（既有 `buzhou.tool.duration` timer——micrometer 装配后才有读数，且 tag 仅 outcome 无 per-tool 键：per-tool tag 违反 tag 基数守卫，spec 111 既有决策）。库内默认 no-op、无 micrometer 的部署里，「哪个工具吃掉最多工具耗时」完全不可见——Turn 慢时无法回答该换掉/裁剪哪个工具。F 会话 spec 646/647 已给 hook 链同款读面，工具侧为自然同构位。

## 目标

- `ToolTimingAggregator`（core/exec，Holder 模式：HookTimingAggregator 同款）：挂在 HookedToolCallback **既有计时点**（复用同一 nanoTime 窗口），per-tool 名累计 count / totalNanos / maxNanos / failed（failed = 错误即反馈路径计数）；`stats()` 返回不可变快照。
- `ToolTimingHealth`（BuzhouHealth）：恒 UP（观测辅助面——工具慢 ≠ 机制失能）；details = per-tool {count, totalMicros, maxMicros, avgMicros, failed}，TOP_LIMIT=20 有界 + `_truncated` 标记（HookTimingHealth 同款纪律）。挂 `/actuator/buzhou` 快照 `tool-timing` 段。
- Spring 装配默认开启（bean 构造时 enable Holder）；编程式 / 未装配 = 纯私有零变化（Holder.current() 判空跳过）。

## 非目标

- 不做 per-tool micrometer tag（基数守卫不变）；不做超时/拦截（纯观测，工具护栏语义敏感）；不上 agent 级维度（进程级聚合已答「哪个工具慢」）。

## 测试

- 快/慢工具混合：慢工具 stats 非零且 max ≥ 模拟耗时；多次调用 count 累计；多工具独立键。
- 失败工具：failed ≥1 且耗时仍累计（错误即反馈不改语义）。
- 未开启 Holder：既有 HookedToolCallback 用例零回归；stats() 不可变。

## 兼容性

纯增量观测；未装配路径逐字节不变；新公共类（Aggregator/Health/Holder）入 API 快照（收口轮随快照再生入档）。
