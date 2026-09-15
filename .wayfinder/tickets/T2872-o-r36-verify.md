---
id: T2872
title: O 系 R36 对账轮的验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2871]
created: 2026-09-16
---

## Question]

全仓 clean verify BUILD SUCCESS 且三门全绿吗？（spec 1835 / effort #1835 / R36）

## Resolution

**全仓 clean verify BUILD SUCCESS**（主树口径）+ 快照门五类型一致 +
对账门四断言绿。过程注记：①worktree 提交态验证曾红快照门（次序工件——
快照更新在本轮文书内，先提交后验即绿）；②worktree 死链红为并行会话
README 行指向其未跟踪 spec 文件（1136/1211 主树实存）——并行工件非本系
欠账，主树口径验证通过；③K 会话在途测试编辑期主树 verify 一度红，其
收尾后复验绿。
