# 745 — G 会话收口预检轮（台账核查）

> 来源：G 会话第 49 轮 = effort #745（D/E/F 会话收口预检模式）/ [T1047](../../.wayfinder/tickets/T1047-g-session-audit-shape.md) / [T1048](../../.wayfinder/tickets/T1048-g-session-audit-verify.md) / impl 551。

## 背景

第 50 轮终验前——台账完整性核查：spec 编号连续性、票/impl 对账、守门测试（SpecCoverage + 快照门比对）复跑。核查发现 **spec 745 自身缺位**（r45 用 744、r46 跳 746）——本轮即填补。

## 目标

- spec 700–748 连续 49 份（本轮补 745 后完整）；
- 票 T951–T1046（96 张）全闭环（status: closed）；
- impl 535–550 连续 16 片对账；
- SpecCoverageTest + ApiSurfaceSnapshotTest（比对模式）复跑绿。

## 兼容性

纯文档/台账域。
