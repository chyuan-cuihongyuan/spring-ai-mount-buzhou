---
id: T2148
title: 相似对序判定与病态提供者显形的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2147
created: 2026-09-14
---

## Question

如何证明序判定、病态显形与维度伴生信号？

## Resolution

**用户常设授权 AFK（可推翻）**

`EmbeddingSelfCheckTest` 四测全绿（`mvn -pl buzhou-core -am test`）：语义词包提供者全过（orderHolds+margin>0+dimension=4）；病态常数向量（cos 恒 1/margin 恒 0→序不成立）；负向量对照（数学不变量——序保持验证探针稳定）；维度伴生显形（4/8/0 三态）。评审修正：初版词哈希替身对中文无空格失效（整句单 token）→改关键词维替身+样本对关键词对齐。
