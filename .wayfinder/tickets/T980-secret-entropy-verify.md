---
id: T980
title: 秘密扫描熵阈值过滤的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T979
created: 2026-09-13
---

## Question

高熵真过、低熵/示例键真滤、阈值边界一致、PRIVATE_KEY_BLOCK 豁免、默认关零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 15 轮）：① 随机形态 AWS 键（高熵）+熵过滤 → 仍命中；② AKIAIOSFODNN7EXAMPLE + 默认 4.0 → 滤；同串熵关 → 命中（对照）；③ ghp_+aaaa… 低熵 → 滤；④ PRIVATE_KEY_BLOCK BEGIN 行 + 熵过滤 → 仍命中（豁免实证）；⑤ 默认构造全套既有用例零回归。`mvn -pl buzhou-guard -am test` 全绿。
