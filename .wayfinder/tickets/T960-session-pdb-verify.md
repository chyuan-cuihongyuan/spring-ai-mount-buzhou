---
id: T960
title: 最小可用水位闸（归档 PDB）的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T959
created: 2026-09-13
---

## Question

水位恰在 floor 时真拒绝？之上放行？未知（负数）fail-open？默认构造零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 5 轮）：① liveSessions=2、minAvailable=2 → archive 拒绝 false + pdb-rejected 计数 +1 且归档键未产生；② liveSessions=3 → 放行真归档；③ 供应商返回 -1（未知）→ 放行（fail-open）；④ null floor 既有 SessionArchiver 用例零回归；⑤ restore 不受闸影响。`mvn -pl buzhou-core -am test` 全绿。
