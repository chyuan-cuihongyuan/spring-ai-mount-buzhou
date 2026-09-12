# 736 — ConfigDiff×快照端点同源补验

> 来源：G 会话第 37 轮 = effort #736（spec 719 补验）/ [T1023](../../.wayfinder/tickets/T1023-config-diff-e2e-shape.md) / [T1024](../../.wayfinder/tickets/T1024-config-diff-e2e-verify.md) / impl 539。

## 背景

ConfigDiff（spec 719）单元已验——与 BuzhouConfigSnapshotEndpoint 掩码快照的同源链路未闭环。

## 目标（测试域补验轮）

- 端点真快照（含掩码键）两份对比 → diff 输出稳定且掩码键按掩码语义判定；
- 端点端到端：ApplicationContext 不可用处以直调 buzhouConfig() 逻辑同构断言（Environment 注入）。

## 兼容性

纯测试域增量。
