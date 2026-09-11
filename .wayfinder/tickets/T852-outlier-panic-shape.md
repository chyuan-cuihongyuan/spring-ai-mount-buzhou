---
id: T852
title: 离群驱逐恐慌阈值的形态与默认值裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

spec 149 把「驱逐比例上限」列入 Out of Scope。Envoy panic threshold 的思想是：健康池跌破占比阈值时路由忽略驱逐（全逐比试坏端点更糟）。本仓引入时应取什么形态与默认值？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 2 轮 = effort #600 / spec 601 / impl 454）：

1. **取 panic 而非 max-ejection-percent**：panic（过滤侧忽略驱逐）直接保可用性；比例上限（驱逐侧封顶）留雾区。
2. `Config` 扩第三参 `panicThresholdPercent ∈ [0,100]`；两参构造保留 = 0（**默认关闭，零行为变化**；Envoy 路由侧常用 50，入 Javadoc 供参考）。阈值下限 = ceil(候选数 × percent / 100)，严格低于才触发（恰在阈值不触发）。
3. 触发时：返回全量候选 + `buzhou.outlier.panic` 计数 + WARN 留痕（可用性优先于隔离，但必须可见）。
4. 空候选不触发；`withPanicAll(…)` 便捷预设 = 100。
