---
id: T2177
title: 评估数据集质量审计（DatasetQualityAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 38 轮：数据集退化条目审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：847 近重复轴/DatasetExpectations 期望格式轴已占——退化/信息量轴（空/超短条目）开放。

形状裁决：DatasetQualityAudit 纯函数（core/eval）——analyze(List<EvalItem>)→QualityReport（emptyInputs/emptyExpecteds/shortInputs<8 字符阈值+inputLengthP50/P95 R-7 秩插值）+degenerateRatio 派生（同条目双退化两桶同计可>1；空集 -1 哨兵）。

Out of scope：语义质量；期望评分；重复检测。
