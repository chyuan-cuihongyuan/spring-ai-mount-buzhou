---
id: T937
title: JSONL 轮转 yml 扩散的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T936
created: 2026-09-13
---

## Question

yml 键绑定生效？缺席默认、显式关两种口径钉住？多构造 record 绑定坑（canonical @ConstructorBinding）不复发？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 44 轮）：① 两个 AutoConfigTest 各加绑定用例：缺省 → DEFAULT_MAX_BYTES/DEFAULT_MAX_HISTORY；声明 0 → 0（关）透传；② Shadow record 5 参 canonical 变 7 参（4/5 参便捷构造兼容 + canonical @ConstructorBinding 保留——R39 坑第三次预防）；③ 既有装配/属性用例零回归。`mvn -pl buzhou-core -am test` + `-pl buzhou-resilience -am test` 全绿。
