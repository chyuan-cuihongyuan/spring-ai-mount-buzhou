# 755 — Hook 链解析顺序快照读面

**What to build:** ChainComposition record（派发序 + 幽灵禁用集）+ HookChain.composition() 构造期快照 + 排序稳定性与幽灵检测测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ChainComposition（List/Set.copyOf 不可变）
- [x] HookChain 构造期解析（resolved + ghost 两面）
- [x] composition() 只读快照
- [x] HookChainCompositionTest（稳定序/禁用滤出/幽灵检测/不可变/空链）
- [x] spec 1002 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='HookChainCompositionTest,HookChainTest'` 全绿。commit 见本轮 `feat(core)` 提交。
