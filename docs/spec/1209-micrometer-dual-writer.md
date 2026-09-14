# 1209 — R10：MicrometerDualWriter 补测（双写适配器指标口径合同）

> 来源：K 会话第 10 轮 = effort #1209（[T1827](../../.wayfinder/tickets/T1827-micrometer-dual-writer-shape.md) / [T1828](../../.wayfinder/tickets/T1828-micrometer-dual-writer-verify.md) / impl 912）。方法论：批次化延续——「无测试文件 > 缺分支」优先级；指标口径类借 **Micrometer TagViolation 抑制/基数纪律** 断言。

## Problem Statement

MicrometerDualWriter（BRANCH 15 missed / 67%）整类零测试文件。它是指标家族口径的执行面：timer/counter 名、tag 值 bounded 截断（32/64/16 三档）、unknown 回退、非正时长与 count≤0 不记——口径回归会直接污染 /metrics 语义。

## 目标

- MicrometerDualWriterTest（11 用例）：NOOP 哨兵全 no-op；recordSpanClose MODEL_CALL/TOOL_CALL 双路径（duration.ms 0ms 口径、provider/name/status unknown 回退）；recordTokens count≤0 跳过 + unknown 回退；recordTtft/recordTpot null/负/零三态不记与正时长记录；recordQueueWait/recordPersistError；tag 值 32/64/16 有界截断全档。

## 实现决策

- CapturingMetrics 计数/计时分离队列，tag 对以 `k=v,k2=v2` 规范化字符串（对间 `,`、键值 `=`）——首版 join 串格式错误即刻暴露假红，fake 格式即合同的一部分。
- @AfterEach BuzhouMetricsHolder.reset()（全局 Holder 测试纪律）。

## 测试决策

- 断言只对可观察行为：捕获的指标名/delta/毫秒/tag 值三元组字符串；不测 registry 细节（registry 参数已被 impl-46 收敛忽略——构造器兼容哨兵语义也入测）。
- 先例：ToolDurationTimerTest（CapturingMetrics）、R2 PolicyGateHookTest（outcome 分桶）。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- ObservabilityAdvisor 流式路径分支（68 missed，需流式 harness，R11 单列深做）。
- 其他 observability 类（BaseSpanRecorder/DefaultSpanHandle 等批次 4 候选）。

## Further Notes

- MicrometerDualWriter 分支 67%→93%（86/6，残余 = bounded 截断边界组合）；observability 94 用例全绿。
