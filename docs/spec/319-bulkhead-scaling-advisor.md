# Spec 319 — 舱压伸缩建议（effort #319）

> wayfinder map：`.wayfinder319/MAP.md`（T629–T630）。借鉴：Kubernetes HPA
> custom metrics——desired = current × metric/target；Buzhou 的 utilization
> = 舱拒绝率（持续拒绝 = 容量不足）。

## Problem Statement

舱拒绝（per-agent 拒绝计数表）只有计数没有行动面：热点 agent 持续被拒 =
容量不足，运维想扩容只能拍脑袋估"加几个实例"——拒绝计数到扩容决策之间
缺一层可解释的建议。

## Solution

`BulkheadScalingAdvisor`（core.concurrent，建议面不执行面）：

- 跨调用窗口读舱拒绝**增量**（两次 `advise()` 之间 = 一个窗口，无内部
  定时——节奏归宿主）；建议实例倍率 = clamp(1 + 窗口拒绝 / scaleUpThreshold,
  1, maxMultiplier)（HPA 阈值比的整数简化：每 threshold 个窗口拒绝 = +1 倍）。
- 拒绝回零 → 建议 1（scale-down 保守回落到 1 不为 0——HPA minReplicas 语义）。
- 只建议不扩容：执行（真扩/真缩）归宿主/调度器——诚实边界。
- yml：`buzhou.bulkhead.scaling.scale-up-threshold`（≥1，未配不装配零变化）
  + `buzhou.bulkhead.scaling.max-multiplier`（默认 3）；依赖舱 bean 在场
  （舱没开拒绝恒 0，建议恒 1 无意义——不装配）。

## User Stories

1. 作为运维，我想拿到"该 agent 建议几倍实例"的可解释建议（窗口拒绝数
   亮出来），所以扩容决策不再拍脑袋。
2. 作为运维，拒绝高峰过去后我想看到建议自动回落 1，所以不会照旧建议
   过量扩容。
3. 作为运维，我想给建议设上限（max-multiplier），所以一次窗口的拒绝
   风暴不会被翻译成离谱的倍率。
4. 作为运维，多个热点 agent 我想各自拿建议，所以按 agent 精确扩容而
   非全实例齐扩。

## Implementation Decisions

- 输入缝复用舱既有观测面（拒绝计数表全量快照），不在舱上开新缝；窗口
  增量 = 本次快照 − 上次快照（per-agent 计数单调，快照稳定可比）。
- Advice 为不可变快照记录（agent、windowRejections、suggestedMultiplier）；
  有拒绝历史的 agent 才进建议面（从未被拒 = 隐式 1，无需建议）。
- 256 封顶折叠行（`__overflow__`）按普通名透传——诚实边界。
- 计数：scale-up / scale-down 建议事件（scale-down = 倍率从 &gt;1 回落 1）
  走 metrics 持久 + 类内观测计数。
- 进程内建议（部署单元语义，同 318 预算——跨实例共享族 316 已收口，
  建议按实例即按部署单元）。

## Testing Decisions

- `BulkheadScalingAdvisorTest`：外部行为测试（驱动真实舱制造拒绝，不 mock
  计数）——拒绝升倍 / 回零回落 / 倍率钳制 / 多 agent 各自建议 / 参数校验。
- 装配测试（ApplicationContextRunner 先例）：舱开 + threshold 配 → 装配；
  threshold 未配或舱未开 → 不装配。
- 先例：SessionDisruptionBudgetTest（318）——构造校验 + 行为 + 装配三层。

## Out of Scope

- 自动扩容执行（宿主接 advice 调度）；per-agent 建议聚合到全局建议；
- 窗口定时器（节奏归宿主）。

## Further Notes

- K8s 三件套（排水 155 / 维护门 205 / 扰乱预算 318）之后补上伸缩建议面
  ——容量族从"防守"到"建议进攻"。
