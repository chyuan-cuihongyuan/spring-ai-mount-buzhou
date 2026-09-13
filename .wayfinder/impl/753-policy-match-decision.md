# 753 — 工具策略匹配决策读面

**What to build:** ToolPolicyMatchDecision / ToolPolicyMatchStats 新公共类型 + match 判定单点决策累加（返回值不变）+ stats()/resetStats() 读面 + 有界最近决策环 + 全分类守恒测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ToolPolicyMatchDecision（嵌套 Outcome 枚举）
- [x] ToolPolicyMatchStats（守恒 total()）
- [x] ToolPolicyMatcher 判定单点累加 + stats()/resetStats()
- [x] ToolPolicyMatchDecisionTest（三分类/无效条目/守恒/有界环/reset）
- [x] spec 1000 + README 行 + API 快照再生

## Done

验证：`mvn -pl buzhou-core test -Dtest='ToolPolicyMatchDecisionTest,ToolPolicyMatcherTest'` 全绿。commit 见本轮 `feat(core)` 提交。
