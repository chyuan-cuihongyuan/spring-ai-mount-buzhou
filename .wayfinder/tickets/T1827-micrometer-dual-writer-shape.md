---
id: T1827
title: R10 选题——MicrometerDualWriter 零测试类补测（双写适配器指标口径）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 10 轮：observability 批次 3 选题。MicrometerDualWriter（branch 15 missed，且整个类无测试文件）如何补测？ObservabilityAdvisor（68 missed）为何顺延？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 10 轮 = effort #1209 / spec 1209 / impl 912）：

1. **本批 = MicrometerDualWriter 单类**：双写适配器是指标家族口径的执行面（timer/counter 名、tag 值 bounded 截断、unknown 回退、非正时长不记），且整个类零测试文件——批次内优先「无测试文件 > 缺分支」的类。
2. **补测面**：NOOP 哨兵全 no-op；recordSpanClose MODEL_CALL/TOOL_CALL 双路径（duration.ms 缺失/非 Number → 0ms、provider/name/status unknown 回退与 32/64 截断）；recordTokens count≤0 跳过与 model null → unknown；recordTtft/recordTpot null/负/零不记与正时长记录；recordQueueWait/recordPersistError。CapturingMetrics + @AfterEach reset（R2 先例）。
3. **ObservabilityAdvisor 顺延 R11**：68 missed 集中在流式路径（accumulateStreamChunk/recordModelCallOutcome），需流式 harness——单独立轮深做，不在本轮夹带。
4. **边界**：不改主代码；适配器行为合同逐字锁定（指标名/tag 键序由 CapturingMetrics 字符串化断言）。
