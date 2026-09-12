---
id: T1031
title: PII 格式保形掩码验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1030]
created: 2026-09-12
---

## Question

四型掩码形态与 fail-closed 降级如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 16 轮 = effort #715）：①四型标准输入形态逐字符+保长断言；②非法形状→等长全星不抛；③通用掩码 keep≥长度全保边界；④null fail-fast。buzhou-guard 全模块零回归（C 会话排除集）。
