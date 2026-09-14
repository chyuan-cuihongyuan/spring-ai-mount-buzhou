---
id: T2352
title: R1 语义缓存 LFU 采样驱逐的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2351
created: 2026-09-15
---

## Question

N 会话第 1 轮：如何验收？

## Resolution

行为测试五断言（新测试类 `SemanticCacheLfuEvictionTest`）：
① 默认 sampleSize=0 满容量驱逐仍取 eldest（高频触达老条目后写入，老条目仍出局）——零变化钉死；
② opt-in 采样开 + 老条目高频触达 → 淘汰低频新条目、高频老条目存活，`hotPreservedCount`≥1；
③ 命中计数封顶不无界；
④ 权重预算腾挪路径同款采样语义；
⑤ 负 sampleSize 构造/属性组双双 fail-fast IAE。
装配面：`ResilienceProperties.SemanticCache` 扩参与 `ResilienceModule` 传参，模块测试全绿后单轮 commit。
