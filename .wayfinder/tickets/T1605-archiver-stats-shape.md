---
id: T1605
title: 会话归档操作读面（ArchiveStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1603
created: 2026-09-15
---

## Question

J 会话第 75 轮：core/cleanup 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SessionArchiver.archive()（会话归档主入口）分支零计数——pdbRejected 有单点 micrometer 但 archived 成功量与 emptySkipped 空会话跳过量无进程内直读；「归档了多少会话、多少空跳过」无对账。S3 lifecycle 归档统计思想。

形状裁决：`SessionArchiver` 内静态 `AtomicLong` 四计数——archiveCalls（入口）/ archived（true=成功）/ emptySkipped（空会话诚实不动）/ pdbRejected（可用性下限拒——既有 micrometer 之外的进程内对账）；嵌套 `record ArchiveStats` + `stats()` + `resetForTest()`。口径诚实：异常外溢的入口不入桶（异常打破正常流程，既有异常语义不变），四桶覆盖全部正常结局。archive() 返回语义逐位不变。

Out of scope：restore 面独立计量（另轴）；归档字节量（store 层既有 bytesWritten 口径）。
