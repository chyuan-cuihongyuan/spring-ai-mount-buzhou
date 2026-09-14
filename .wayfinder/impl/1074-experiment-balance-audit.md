# 1074 — 实验分桶均衡审计

**What to build:** ExperimentBalanceAudit 纯函数（两段式入口：声明桶集合+喂计数→BalanceReport 均衡判定）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] ExperimentBalanceAudit（core/experiment，两段式 builder，零桶计入）
- [x] ExperimentBalanceAuditTest 五测（A/A 均衡/失衡/缺桶/哨兵/容差端点）
- [x] spec 1421 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ExperimentBalanceAuditTest'` 5/5 绿。
