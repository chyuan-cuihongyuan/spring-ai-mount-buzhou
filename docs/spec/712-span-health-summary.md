# 712 — span 健康摘要（543 补全；修正轮）

> 来源：G 会话第 19 轮 = effort #712 → 第 21 轮修正（R13 撞车修正）/ [T1024](../../.wayfinder/tickets/T1024-span-status-dist.md) / [T1025](../../.wayfinder/tickets/T1025-span-status-dist-verify.md) / impl 612。

## 撞车修正记录

R13 曾在 core/observability 新建 SpanStatusDistribution——**功能撞 spec 543**（observability/analytics 同名类：kind×status 聚合，Prometheus label 思想）。根因：勘察 grep 用小写 statusDistribution 漏检大写类名。修正：删除 core 重复类；R13 的增量价值（543 未覆盖的部分）收敛到 analytics 类新方法。

## 增量（相对 543）

- **runningResidue**——开启后未关闭的 span 计数：泄漏/进程崩溃残留的直接信号（543 只计数不判健康）。
- **errorRate**——ERROR/total（total=0 诚实 0.0；分母含 RUNNING/CANCELLED，口径显式无「终态率」歧义变体）。
- 落点：`SpanStatusDistribution.healthSummary(List<SpanRecord>)` → 嵌套 `HealthSummary(total, runningResidue, errorRate)`——不加新顶层类型、不破坏 543 既有 API。

## 诚实边界

纯读数（告警归 312 订阅）；分母口径显式（含未关闭——未关闭可能是坏也可能慢，交给消费者结合 residue 判断）。
