---
id: T1624
title: fs 全链路四读面组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1623
created: 2026-09-15
---

## Question

J 会话第 84 轮：fs 链路组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（FsChainReadoutTest，TempDir 骨架）：全链路后四读面（FileSandbox/WriteFile/ReadFile/CommandBlacklist）各自守恒保持 + 跨面字节对称恒等。定向 `mvn -pl buzhou-tools -am test -Dtest='FsChainReadoutTest'` 绿。
