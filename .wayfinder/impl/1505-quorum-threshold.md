# impl 1505 — QuorumThreshold 票数下限判定（R105 = effort #1904 / spec 1904 / T3009-T3010）

**What**：`QuorumThreshold`（core/transaction 静态纯函数）——
majority（⌊N/2⌋+1 简单多数）+ byzantineTolerance（⌊(N−1)/3⌋ 坏票
容忍）+ byzantineSize（3f+1 最小投票者）；voters≥1/faults≥0
fail-fast。

**Why**：Paxos/BFT 票数公式——崩溃容错与拜占庭容错两档混淆（2f+1
想容拜占庭远远不够）是共识配置高频错误；一行公式让「能容几个坏
票、要多少投票者」直读。与 QuorumConsistency（副本交集）互补。

**Verify**：`QuorumThresholdTest` 4 用例全绿（多数派三例/拜占庭
容忍三例/最小规模三例/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
