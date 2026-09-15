# L 会话 1700 系滚动台账（progress-effort-1700）

> 恢复工作先读本文件。每轮一行：轮号 / effort / 提交哈希 / 状态备注。
> 号段：efforts #1700–#1749、specs 1700–1749、票 T2601–T2700、impl 1300–1349；分支 `l-session-1700-series`。

| 轮 | effort | commit | 备注 |
|----|--------|--------|------|
| R1 | #1700 | ad9912f9 | EvalScoreMad MAD 离散度+1700 系对账门落位；core 测试 8/8 绿 |
| R2 | #1701 | 11e64fad | EvalOrderRotator 轮换消序；eval 五测批跑绿 |
| R3 | #1702 | 7d62c19a | EvalCoverageMatrix 覆盖矩阵+熵 |
| R4 | #1703 | e100d8bf | JudgePositionBias 位置偏差 |
| R5 | #1704 | f98dc7a3 | EvalSetFingerprint 内容指纹 |
| R6 | #1705 | af9ab1d3 | EvalGateMargin 门限边际 |
| R7 | #1706 | 5182ef7c | ForkShapeStats fork 形态；session 六测批跑绿 |
| R8 | #1707 | df0c5ca9 | SessionAgeHistogram 年龄直方 |
| R9 | #1708 | 49d47623 | LeaseRenewalStats 续租抖动 |
| R10 | #1709 | f2e98389 | MigrationOutcomeStats 迁移普查 |
| R11 | #1710 | 094b9489 | EventGapDetector 时间间隙 |
| R12 | #1711 | 5e1c6929 | TurnInterArrivalStats 轮间隔 |
| R25 | #1724 | (见 git log effort #1724) | FactReadHistogram 读热四档 |
| R26 | #1725 | 7b03112e | RetrievalRankStats MRR |
| R27 | #1726 | 553c7b7d | EpisodeRetentionStats 保留普查 |
| R28 | #1727 | acf3b04a | SummaryBalanceStats 段落均衡 |
| R29 | #1728 | 085f04f4 | CompactionTriggerStats 触发分布 |
| R30 | #1729 | abce368c | TodoCycleStats 返工周期 |
| R31 | #1730 | a75fa536 | FileWriteKindStats 写型分类 |
| R32 | #1731 | 3b609caf | ThinkingRatioStats 思考占比 |
| R33 | #1732 | d4e8ee28 | SpanAttributeBudget 属性预算 |
| R34 | #1733 | ee9f4f98 | PendingAgeHistogram 待决年龄 |
| R35 | #1734 | c7e6e500 | SkillFunnelStats 技能漏斗 |
| R36 | #1735 | 63c8a63f | SkillRankAgreement 排序一致 |
| R37 | #1736 | c4169f18 | McpReconnectStats 重连实效 |
| R38 | #1737 | ba9ed987 | McpNamespaceAudit 命名空间 |
| R39 | #1738 | 08142b7e | PiiScanLatency 扫描分位 |
| R40 | #1739 | cf581513 | ExemptionTtlHistogram 豁免 TTL |
| R41 | #1740 | 63a2e1c6 | PiiChannelMatrix 通道矩阵 |
| R42 | #1741 | fac5d6d7 | InjectionCalibrationProbe 校准探针 |
| R43 | #1742 | 3748f4a7 | RangeLocalityStats 读局部性 |
| R44 | #1743 | a6b12c11 | SpillHandleAgeHistogram 句柄年龄 |
| R45 | #1744 | 435014da | HedgeStats 对冲节省 |
| R46 | #1745 | ef465772 | RetrySpreadStats jitter 实效 |
| R47 | #1746 | c42f485b | BudgetEtaProjection ETA 投影 |
| R48 | #1747 | 4be8449e | IdempotencyCollisions 幂等冲突 |
| R49 | #1748 | 7c87263b | 快照再生+README 补行+1133 死链吸收补登+三门全绿 |
| R50 | #1749 | 509992a1 | 收口工件落位；全仓 verify 隔离 worktree 进行中 |
