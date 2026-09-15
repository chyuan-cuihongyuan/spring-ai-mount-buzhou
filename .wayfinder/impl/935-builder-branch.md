# 935 — GuardModule$Builder 开关组合矩阵补测（R36）

**What to build:** GuardModuleBuilderBranchTest（10 用例：全关最小面/spotlighting/injectionDefense/taintTracking/piiRedaction/canaryGuard/enabled=false/dangerousTool entry/fromYml 等价/allOn ⊇ allOff）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] GuardModuleBuilderBranchTest（10 用例）
- [x] spec 1231 + README 行
- [x] 验证：定向绿 + guard 426 用例全量绿

## Done

验证：定向 10 用例全绿；guard 426 用例全量绿。commit 见本轮 `test(guard)` 提交。
