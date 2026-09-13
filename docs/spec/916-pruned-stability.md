# 916 — pruned×稳定性×gate 联动补验

> 来源：I 会话第 17 轮 = effort #916（[T1283](../../.wayfinder/tickets/T1283-pruned-stability-shape.md) / [T1284](../../.wayfinder/tickets/T1284-pruned-stability-verify.md) / impl 669）。G 会话补验轮模式（r39 抓实现缺陷先例）。

## 背景

评估域三件（901 剪枝 / 908 k 稳定性 / 914 gate 历史）单测各自绿，但 pruned 状态跨组件流动的语义未实证：`analyzeK` 红绿映射（pass=绿、fail/error=红）**不含 pruned 分支**——pruned 落入「非红即绿」的绿侧，会把「真 fail + 多次 pruned」的项误判稳定绿。

## 目标

- `EvalFlakinessDetector.analyzeK`：对齐时 `pruned` 状态视为**该 run 无有效样本**（置 null）→ 走既有单侧排除路径（有效样本 <2 的项不 compared）；
- 两 run 版 `analyze` 同语义加固：含 pruned 的项不入比对分母（单侧处理）；
- `isRed` 本体不动（"pruned 非红"只影响对齐排除，不影响红绿映射的既有调用点语义）；
- e2e 断言：`真 fail + 2×pruned` 项不 compared（无假稳定）；`pass + fail + 2×pruned` 项按仅有的 2 个有效样本判定翻转（不假稳定）；
- gate 历史记录剪枝 run 正常（GateDecision 不含项级状态——天然无交互，测试固化）。

## 兼容性

行为修复：pruned 项从稳定性分母排除——此前会污染判定的错误行为即本次修复对象；纯 pass/fail/error 项的既有判定零变化。
