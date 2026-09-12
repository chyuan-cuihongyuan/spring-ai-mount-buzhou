---
id: T998
title: 路由金丝雀阶段标签的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T997
created: 2026-09-13
---

## Question

ARCHIVED 剔除、CANARY 按 visible 集条件可见、未标注零变化、cap 有界、view 不可变？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 24 轮）：① stable+canary+archived 三态 → visible={STABLE} 时 archived/canary 剔除、stable 保留权重不变；② visible 含 CANARY → canary 回归；③ 未标注路由恒保留（默认零变化）；④ filter 空结果（全剔除）返回空表不抛；⑤ tag 超 64 拒绝；⑥ view 不可变。`mvn -pl buzhou-resilience -am test` 全绿。
