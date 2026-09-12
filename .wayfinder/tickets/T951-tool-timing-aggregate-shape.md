---
id: T951
title: 工具执行 per-tool 耗时聚合读面的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

HookedToolCallback 是全部工具的统一执行点，已有 `buzhou.tool.duration` timer（micrometer，tag 仅 outcome）——但库内默认 no-op、无 micrometer 装配时，「哪个工具吃掉最多工具耗时」完全不可见（timer 也无 per-tool tag，基数守卫不允许）。per-tool 进程级聚合读面怎么做？与 F 会话 spec 646/647 hook 计时的关系？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 1 轮 = effort #700 / spec 700 / impl 503）：与 hook 侧完全同构——`ToolTimingAggregator`（Holder 模式：HookTimingAggregator 同款）挂在 HookedToolCallback 既有计时点（复用同一 nanoTime 窗口，零额外开销），per-tool 累计 count/totalNanos/maxNanos/failed（failed=错误即反馈路径计数）；`stats()` 返回不可变快照。`ToolTimingHealth`（BuzhouHealth：恒 UP + details=per-tool {count,totalMicros,maxMicros,avgMicros,failed}，TOP_LIMIT=20 有界 + _truncated，HookTimingHealth 同款纪律）挂 `/actuator/buzhou` tool-timing 段。Spring 装配默认开启（bean enable Holder）；编程式未装配=纯私有零变化。借鉴 PostgreSQL `pg_stat_statements`（语句级耗时聚合 top 读面）/ ClickHouse query log 思想——「换掉哪个工具能救回延迟」一查便知。不进 micrometer（per-tool tag 违基数守卫，spec 111 既有决策）。
