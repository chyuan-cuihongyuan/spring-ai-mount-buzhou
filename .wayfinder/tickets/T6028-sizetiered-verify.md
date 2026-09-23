---
id: T6028
title: R 会话 R14 尺寸分层合并的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6027]
created: 2026-09-23
---

## Question

R14 合同怎么逐一验绿？（spec 4013 / effort #4013 / R14）

## Resolution

**验证通过**：SizeTieredMergePickerTest 五测全绿——四同量级成组
（尺寸升序）+ 巨块异层排除；桶员 3<min 空表与桶员 6>max 取最小
4 双门；两桶竞选 5 员胜 4 员；空/单候选空表；畸形七型 fail-fast。
