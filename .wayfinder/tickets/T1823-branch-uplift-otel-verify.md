---
id: T1823
title: R8 分支缺口批次 1 验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1822
created: 2026-09-15
---

## Question

R8 补测后：OtelBridgeSink / OtelProperties 分支覆盖提升多少？observe-otel 模块全量是否绿？store-redis 环境约束是否如实入档？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observe-otel 全量 + JaCoCo 复扫）：

1. **分支提升**：OtelBridgeSink branch covered 65→184 / missed 41→28（61%→87%）；OtelProperties →100%（48/0）。
2. **模块全绿**：observe-otel 32 用例 0 失败 0 错误（新增 16 用例）。
3. **显形主代码缺陷 T1824**（sessionTrace 驱逐 IllegalStateException 被隔离吞掉 → 超限后新会话 span 静默丢弃）——单列票独立 commit 修复；驱逐语义不变（任意条驱逐非 LRU）。
4. **store-redis 环境约束如实入档**：缺口大头（missed 84/42/42 全 covered=0）为容器门控类，本地不可 uplift；BRANCH 硬门开启评估按容器门控缺口单列口径。防御性不可达分支（record compact 已归一）记录不硬凑。
