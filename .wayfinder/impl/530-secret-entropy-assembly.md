# 530 — 秘密熵过滤 yml/Builder 装配

**What to build:** GuardModule.Builder.secretMinEntropy → SecretScanner(types, entropy) 直通 + SecretScanHook(SecretScanner) 构造 + hooksView 包内观测面。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Builder 字段/setter + 统一构造直通 + hook 构造
- [x] beforeTurn 真缝两态用例（滤/照常）+ 装配摘要
- [x] spec 727 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-guard -am test` 绿。commit 见本轮 `feat(guard)` 提交。
