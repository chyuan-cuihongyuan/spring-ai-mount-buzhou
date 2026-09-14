# 1119 — spec 07 回写 + 序位常量化（M 系 R18）

**What to build:** spec 07 三处七切面回写 + 四处 order 常量化。

**Blocked by:** T2283 / T2284（同轮 shape+verify）。

**Status:** done

- [x] spec 07 六→七切面三处
- [x] 四处 ADVISOR_ORDER_OFFSET/HOOK_ORDER 常量（同值零行为变化）
- [x] resilience 384 + spill 180 用例零回归

## Done

验证：双模块测试绿。commit 见本轮 refactor 提交。
