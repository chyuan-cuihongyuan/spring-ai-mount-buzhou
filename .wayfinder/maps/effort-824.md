# effort #824 — 取消原因分布读数

- 会话：H 会话 800 系第 25 轮 ｜ spec [824](../../../docs/spec/824-cancel-cause-distribution.md) ｜ 票 [T1149](../tickets/T1149-cancel-cause-distribution.md)/[T1150](../tickets/T1150-cancel-cause-distribution-verify.md) ｜ impl577
- 借鉴：Temporal cancellation 语义观测（temporalio/temporal ≈14K star）；606 CancelCause 闭集分布面

## 勘察（排重）

- CancelCause（606）：枚举闭集——无分布读数。
- CancelMode：模式（graceful/immediate）非原因。
- grep -i `cause.*distribution|cancel.*count`：无命中。

## 决定

`CancelCauseDistribution`（core.session，synchronized 记账）：record(cause, atMillis)——闭集枚举天然有界（无封顶问题）；计数+lastSeen（同因取 max）+share；snapshot counts 降序+dominant（平局=枚举声明序先者——EnumMap 迭代序确定）；null 忽略；空报告三 null/零。喂点归取消路径装配侧。

## 测试

计数降序+份额 0.75+lastSeen 取 max/dominant/null 忽略+空报告/平局声明序（首跑预期笔误修正：声明序 LEASE_LOST 先于 RUNAWAY）——3 例绿。

## 诚实边界

喂点手动（不改 cancel 路径行为）；进程内存有界；lastSeen 为 Long.MIN_VALUE 哨兵（理论不可达——记录即有时间）。
