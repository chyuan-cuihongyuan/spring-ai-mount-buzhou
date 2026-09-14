# 1208 — R9 分支缺口批次 2：observability 两小类（ObservabilitySessionState × ObservableToolCallback）

> 来源：K 会话第 9 轮 = effort #1208（[T1825](../../.wayfinder/tickets/T1825-branch-uplift-observability-shape.md) / [T1826](../../.wayfinder/tickets/T1826-branch-uplift-observability-verify.md) / impl 911）。方法论：R8 分支定向补测的批次化延续——R7 逐类分支数据 → 小类先清（共享 fake 基建）→ 大类（ObservabilityAdvisor 68 missed）留批次 3。

## Problem Statement

observability 为 BRANCH 第二差模块（66.7%）。两小类缺口：ObservabilitySessionState（46%）承载会话作用域 span 生命周期（SESSION/TURN 开关、usage 聚合、carrier 同步）；ObservableToolCallback（29%）为工具可观测包装（span 开关、TOOL_INPUT/OUTPUT 事件、parent 三级解析）。前者是抗串味载体的同步枢纽，后者是 fan-out 并发下 parent 正确性的执行面——分支语义均行为敏感。

## 目标

- **ObservabilitySessionStateTest**（9 用例）：onOpen 开 SESSION 根（5 参 explicit ctx + agent/app/model 属性 + carrier 绑定）；onTurnStart 无 session 早退、300 字输入截断 200、null 输入省略 preview、计数器重置；accumulateTurnUsage null 侧忽略；onTurnEnd usage/completed/iteration 属性 + 二次幂等；onTurnError 标记失败 + close；onClose flush 与 onCancel CANCELLED 终态。
- **ObservableToolCallbackTest**（8 用例）：成功路径（开 TOOL_CALL span→TOOL_INPUT→委托→TOOL_OUTPUT→close、parent=载体 turn 快照）；委托异常 error+close+rethrow 且无 TOOL_OUTPUT；parent 三级解析（ToolContext 载体 > 字段载体 > hooks.sessionSpan 兜底）与 toolContext null 分支；tool.parallel.index 由载体计数；单参 call 委托；getToolDefinition/Metadata/delegate 委托。

## 实现决策

- 录制型 fake（RecordingRecorder + RecordingHandle，无 Mockito）**必须把 openSpan 的初始 attributes 落到 handle**（真实 recorder 持久化属性袋——假件丢初始袋曾致断言假红，fake 行为须对齐真实语义）；两测试类各自内聚小 fake（仓库惯例）。
- parent 解析断言经 fake 记录的 parent SpanContext 精确比对（turn 快照身份）。
- store-redis 批次（容器门控缺口）保持 R8 环境约束口径；ObservabilityAdvisor / MicrometerDualWriter 归批次 3。

## 测试决策

- 断言只对外部行为：recorder 收到的 open/emit 序列、handle 属性袋与关闭状态、parent SpanContext 身份、委托调用与返回值透传。
- seam：构造器单点（state/callback 直接构造，fake 注入）。
- 先例：R8 OtelBridgeSinkBranchTest（分支定向）、TaintLifecycleStatsTest（手写 fake）。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- ObservabilityAdvisor（68 missed）/ MicrometerDualWriter（15）归批次 3。
- store-redis 容器门控缺口（R8 环境约束口径不变）。

## Further Notes

- 两靶点提升：ObservabilitySessionState 46%→88%、ObservableToolCallback 29%→86%——分支定向补测的批次化复制成功。
