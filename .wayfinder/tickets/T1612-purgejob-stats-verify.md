---
id: T1612
title: 归档清理任务读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1611
created: 2026-09-15
---

## Question

J 会话第 79 轮：PurgeJobStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（PurgeJobStatsTest，BuzhouStores+SessionArchiver 骨架——见 ArchivePurgeJobTest）：有超龄归档 → purgedTotal 增；锁被占 → skippedLocked=1；resetForTest 归零。定向 `mvn -pl buzhou-core -am test -Dtest='PurgeJobStatsTest'` 绿 + 既有 ArchivePurgeJob 回归绿。
