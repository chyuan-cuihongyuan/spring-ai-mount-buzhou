---
id: T6134
title: S 会话 S17 Two-Phase Commit 协调器的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6133]
created: 2026-09-24
---

## Question

S17 合同怎么逐一验绿？（spec 5016 / effort #5016 / S17）

## Resolution

**验证通过**：TwoPhaseCoordinatorTest 五测全绿——全票
PREPARED→COMMITTED；一票否决→ABORTED 且后续 commit IAE；
PREPARING 直接 abort；重复投票/未知参与者/非法迁移
fail-fast；确定性回放。
