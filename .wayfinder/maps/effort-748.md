# effort #748 — 执行策略汇总读数

- 会话：G 会话 700 系第 49 轮 ｜ spec [748](../../../docs/spec/748-execution-policy-readout.md) ｜ 票 [T1098](../tickets/T1098-execution-policy-readout.md)/[T1099](../tickets/T1099-execution-policy-readout-verify.md) ｜ impl649
- 借鉴：—（EvalRunner 五件套的配置确认面）

## 勘察（排重）

- 五件套（预算 520/重试 535/超时 609/记忆化 708/漂移 718）各自 setter——「runner 当前策略态」无一屏确认面；排障「为什么有 [RUN-BUDGET]」要先翻代码。

## 决定

`EvalRunner.executionPolicy()`：Map 回显 runBudgetChars/errorRetryOnce/perItemTimeoutMs/memoizationKey/driftWindow/driftWarnShift 当前态（未设为 null/0）。

## 测试
默认全关形态/逐项设置回显。
