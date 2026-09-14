---
id: T1548
title: write_file 写入量水位与拒绝分桶读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1547
created: 2026-09-14
---

## Question

J 会话第 46 轮：WriteFileStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（WriteFileStatsTest，TempDir 沙箱骨架）：成功写入 → writes=1 bytesWritten=字符 UTF-8 字节数 attempts=1；缺 content → paramRejects=1；超 8MB（构造超限串）→ oversizeRejects=1；noclobber 开启二次写同路径 → noclobberRejects=1；非法路径（`../` 逃逸）→ failures=1；守恒 attempts = Σ分桶（含 bytesWritten 与 attempts 同步增长）；resetForTest 归零。定向 `mvn -pl buzhou-tools -am test -Dtest='WriteFileStatsTest'` 绿 + 既有 WriteFileTool 回归绿。
