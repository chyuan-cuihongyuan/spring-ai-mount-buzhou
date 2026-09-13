# 766 — Hook Replace 载荷应用/丢弃计数读面

**What to build:** applyReplace 返回 boolean + HookChain replaceApplied/replaceDropped 计数与读面 + 合法/非法载荷双路测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] applyReplace boolean 化（各分支应用语义不变）
- [x] replaceApplied/replaceDropped 计数 + 双 getter
- [x] HookChainReplaceStatsTest（合法应用/幽灵丢弃/分发继续/累计对账）
- [x] spec 1013 + README 行（无新公共类型，不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='HookChainReplaceStatsTest,HookChainTest'` 全绿。commit 见本轮 `feat(core)` 提交。
