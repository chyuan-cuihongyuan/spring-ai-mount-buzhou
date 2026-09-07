# Wayfinder Map — Buzhou 舱压伸缩建议（effort #319，C 会话第 20 轮）

> C 会话第 20 轮。舱拒绝（AgentBulkhead.rejections）只有计数没有行动面：
> 持续拒绝 = 容量不足，扩几个实例靠拍脑袋（K8s HPA 对 utilization 做同款
> 决策——Buzhou 的 utilization = 舱拒绝率）。

## Destination

`BulkheadScalingAdvisor`（core.concurrent，K8s HPA 思想收窄）：跨调用窗口
读舱拒绝增量 → 建议实例倍率（clamp 1..max；拒绝回零建议回落 1）。建议面
（advice snapshot + 计数）不自动扩容——执行归宿主/调度器（诚实边界）。

## Notes

- 号段：spec 319 / T629–T630 / impl-342。
- 借鉴：K8s HPA custom metrics（desired = current × metric/target 的阈值比简化）。

## Decisions so far

- 窗口 = 两次 advise() 调用之间（增量读——无内部定时）；建议 = clamp(
  1 + 窗口拒绝/scaleUpThreshold, 1, maxMultiplier)。
- 拒绝回零 → 建议 1（回落——scale-down 保守为倍率 1 不为 0）。

## Out of scope

- 自动扩容执行（宿主接 advice 调度）；全局舱（per-agent 建议聚合——按需）。

## Tickets

- [x] [T629 BulkheadScalingAdvisor（窗口增量/倍率/回落）](tickets/T629-hpa-advisor.md)（impl-342）
- [x] [T630 回归（拒绝升倍/回零回落/钳制/多 agent 各自建议）](tickets/T630-hpa-close.md)（impl-342）
