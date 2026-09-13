# 700 — 能力门决策审计读数

> 来源：G 会话第 1 轮 = effort #700（502 能力门的读数面扩散）/ [T1000](../../.wayfinder/tickets/T1000-capability-decision-audit.md) / [T1001](../../.wayfinder/tickets/T1001-capability-decision-audit-verify.md) / impl 600。

## Problem

502 能力门把不合格模型调用拦成 `ARGS_VALIDATION_FAILED` 结构化异常——但「门最近拒了谁、拒了几次、什么能力不达标」只存在于异常抛出瞬间：没有读数面。排障路径是翻日志或复现请求；容量规划（哪些模型频繁被门挡——该换模型还是补声明）完全没有证据面。

## Solution

`CapabilityDecisionAudit`（resilience.capability，OPA Decision Logs 思想——策略决定结构化留痕+聚合读数）：

- **deny 逐条留痕**：固定容量环形（默认 64 条，`DEFAULT_CAPACITY`），每条 `Decision(model, capability, atEpochMs)`——capability ∈ {vision, tools}；超出容量覆盖最旧，`dropped` 计数留痕（读数消费者可感知不完整）。
- **admit 只计数**：放行量级=每请求，逐条会刷掉 deny 留痕——诚实折中：`admitted` 单调计数（未注册模型零门=不计，门未裁决）。
- **聚合读数**：`denyByModel`（model→deny 次数）随每次 deny 增量维护。
- **snapshot()**：不可变 `Report(recentDenies 最新在前, denied, admitted, dropped, capacity, denyByModel)`——列表防御性拷贝。
- **接线**：装配层同 `CapabilityPresentCondition` 条件装配 audit bean；`CapabilityGateAdvisor` 加 3 参构造（audit nullable），gate() 在 throw 前先 record——拒绝行为本身零变化（纯旁路）。

## User Stories

1. 排障：调用方报「请求被拒」——读 audit snapshot 即见最近 deny 序列（模型/能力/时刻），免复现。
2. 容量规划：denyByModel 显示 vision 拒绝集中在模型 X——决策是补 `vision=true` 声明还是路由改造，有数可依。

## Implementation Decisions

- admit 不逐条（量级折中，见上）；deny 容量 64 固定（不做 yml 键——读数面非行为面，后续有需求再加）。
- 线程安全：synchronized 环形（deny 频率低，锁竞争不构成瓶颈）。
- 无持久化：内存有界、进程重启清零——审计连续性归日志/审计链族，本面只管「进程内最近窗口」。
- 2 参构造委托 3 参（audit=null）——既有测试与直构调用零破坏。

## Testing Decisions

- 环形覆盖：容量 2 记 3 次 deny → recent 留最新 2 条、dropped=1、denied=3。
- admit 只计数：recordAdmit ×N → admitted=N、recentDenies 空。
- fail-fast：null model/capability → NPE；snapshot 列表修改不影响内部。
- 接线冒烟：advisor 带 audit，缺 vision 请求 → 异常照抛且 audit.snapshot().denied=1。

## Out of Scope

- 其它门（限流/预算/护栏）的决策审计（各族语义不同，扩散留后续轮）。
- deny 事件外发（webhook/指标导出）——本面只做进程内读数。
- 审计链/hash 完整性（guard AuditChain 族已覆盖跨面完整性）。

## Further Notes

借鉴定源：open-policy-agent/opa（≈10K star）Decision Logs——「策略决定的证据面」思想；F 会话 600 工具注解漂移是「目录面漂移」，本面是「决定面留痕」，正交。
