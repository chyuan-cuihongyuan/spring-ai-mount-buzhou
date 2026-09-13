---
id: T1498
title: 密钥扫描计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1497
created: 2026-09-14
---

## Question

J 会话第 24 轮：密钥扫描计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SecretScanStatsTest，熵关构造 + AWS 示例键样本——复用既有熵测试样本）：scan 计调用与命中；redact 三计数齐动；无命中 scan 计调用零命中；已脱敏文本幂等早返不计；空文本不计；实例隔离。定向 `mvn -pl buzhou-guard test -Dtest='SecretScanStatsTest,SecretScannerEntropyTest'` 绿。
