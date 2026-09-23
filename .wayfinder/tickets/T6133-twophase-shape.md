---
id: T6133
title: S 会话 S17 Two-Phase Commit 协调器的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

跨资源原子提交怎么显式阶段化防撕裂？（spec 5016 /
effort #5016 / S17）

## Resolution

**TwoPhaseCoordinator（core/transaction）**：2PC 思想——
begin→PREPARING，votePrepare 全 yes→PREPARED（一票否决
→ABORTED），commit 仅自 PREPARED；非法迁移/未知参与者
IAE fail-fast；嵌套 Phase 枚举不另立面。
