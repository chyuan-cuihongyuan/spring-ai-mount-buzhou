---
id: T1550
title: read_file 读量水位与拒绝分桶读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1549
created: 2026-09-14
---

## Question

J 会话第 47 轮：ReadFileStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ReadFileStatsTest，TempDir 沙箱骨架）：成功整读 → reads=1 bytesRead=文件 UTF-8 字节数；不存在路径 → notFileRejects=1；超 8MB 文件（写超大文件后触发预检）→ oversizeRejects=1；`../` 逃逸 → failures=1；守恒 attempts = reads + totalRejects（混合调用后恒等式成立）；resetForTest 归零。定向 `mvn -pl buzhou-tools -am test -Dtest='ReadFileStatsTest'` 绿 + 既有 FileToolsTest/NoclobberTest 回归绿。
