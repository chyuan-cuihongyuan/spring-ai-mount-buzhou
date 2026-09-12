---
id: T1043
title: 嵌入超限分批装饰器验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1042]
created: 2026-09-13
---

## Question

切块/拼接/直通如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 22 轮 = effort #721）：①5 条 max=2 → 3 次 delegate、输出 5 条顺序与索引正确；②≤max 单次直通；③maxBatchSize<1 拒绝；④delegate 异常透传。buzhou-resilience 全模块零回归（C 会话排除集）。
