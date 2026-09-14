---
id: T2117
title: 断路器状态时长分析器（CircuitStateDurationAnalyzer）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 9 轮（换题轮）：断路器状态时长分析面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题混淆矩阵与 JudgeCalibration（core/eval，TP/TN/FP/FN+precision/recall/F1）数学全同——半撞换入 R37 题。CircuitTransitionJournal 只有变迁环，无时长积分面。

形状裁决：纯函数 analyze(List<Transition>, nowEpochMs)——按模型分组积段（to 状态归属+末段延伸 now）+StateDuration(total/segments/max)+ModelDurations(openMillis/openShare 派生)+states 降序；无序容忍、空输入哨兵、采样窗口径如实入档；Clock 入参注入。

Out of scope：半开联动；全史积分；告警联动。
