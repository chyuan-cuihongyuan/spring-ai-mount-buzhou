# 1127 — 配置错误显形双小项（M 系 R27）

**What to build:** hook 重名 WARN + observer 幂等注册。

**Blocked by:** T2299 / T2300（同轮 shape+verify）。

**Status:** done

- [x] HookChain 重复名检测 WARN（duplicated 集合）
- [x] addObserver contains 去重
- [x] 双用例 + 既有回归绿

## Done

验证：定向测试绿。commit 见本轮 fix 提交。
