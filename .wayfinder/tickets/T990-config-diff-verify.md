---
id: T990
title: 生效配置 diff 读面的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T989
created: 2026-09-13
---

## Question

三分类正确？字典序稳定？掩码值相等待遇为未变？null 拒绝？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 20 轮）：① before/after 各含新增/删除/变更/不变四类键 → 三分类恰各自命中、不变键不出现；② 输出按 key 字典序；③ 掩码键（***→***）不算 CHANGED（掩码语义同值）；④ null/缺 map 抛 IllegalArgumentException；⑤ 不可变输出。`mvn -pl buzhou-core -am test` 全绿。
