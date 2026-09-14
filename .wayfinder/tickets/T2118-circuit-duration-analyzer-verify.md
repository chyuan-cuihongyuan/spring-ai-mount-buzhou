---
id: T2118
title: 状态时长积分与 OPEN 占比的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2117
created: 2026-09-14
---

## Question

如何证明积段口径、无序容忍与占比派生正确？

## Resolution

**用户常设授权 AFK（可推翻）**

`CircuitStateDurationAnalyzerTest` 五测全绿（`mvn -pl buzhou-resilience -am test`）：空输入哨兵；三段积分精确断言（OPEN 200ms/CLOSED 200ms/窗 400ms/openShare=0.5）；无序输入升序后积分同结果；单变迁末段延伸 now（openShare=1.0）；多模型独立分组+states 降序。
