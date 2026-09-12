---
id: T1037
title: 评估通过率漂移基线验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1036]
created: 2026-09-12
---

## Question

基线取样与告警触发如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 19 轮 = effort #718）：①植 3 次满通过历史 run → 当前 0 通过 → 告警+lastDriftDelta=−1.0；②window=2 只取最近两次；③首跑无历史跳过；④默认关零告警。buzhou-core 全模块零回归（C 会话排除集）。
