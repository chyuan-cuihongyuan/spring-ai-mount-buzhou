# effort #720 — 修正轮：span 状态分布撞车收敛

- 会话：G 会话 700 系第 21 轮（修正轮）｜ spec [712](../../../docs/spec/712-span-health-summary.md)（改写）｜ 票 [T1040](../tickets/T1040-fix-span-collision.md)/[T1041](../tickets/T1041-fix-span-collision-verify.md) ｜ impl620
- 借鉴：—（纯修正）

## 撞车根因

R13 在 core/observability 新建 SpanStatusDistribution——spec 543（observability/analytics 同名类，kind×status 聚合）已存在。勘察 grep 用小写 `statusDistribution` 漏检大写类名 `SpanStatusDistribution`。

## 修正动作

①删除 core/observability/SpanStatusDistribution + 测试；②增量价值（runningResidue/errorRate）收敛到既有 analytics 类新方法 `healthSummary`（嵌套 HealthSummary 记录，无新顶层类型）；③spec 712 改写为「543 补全+修正记录」；④README/api-surface 条目同步。

## 测试

healthSummary 三字段（total/residue/errorRate）精确+空表诚实零+null fail-fast；buzhou-observability 与 buzhou-core 全模块零回归。

## 诚实边界

712 的 spec/票号保留（工件的修复历史是台账一部分——不抹号）。
