---
id: T2192
title: 媒体摄入计数与 MIME 直方的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2191
created: 2026-09-14
---

## Question

如何证明摄入/回读/字节计数与 MIME 直方？

## Resolution

**用户常设授权 AFK（可推翻）**

`MediaIntakeStatsTest` 一测综合全绿（`mvn -pl buzhou-spill -am test`，真 DiskSpillStore 临时目录小载荷）：intake×2（bytes+text 复用漏斗）+readBack×2+bytesTotal 精确（UTF-8 字节口径）+MIME 直方含 image/png 与 text/plain；reset 归零。评审修正：临时目录 helper 命名重复修正。
