# 709 — 实验到期自动停

> 来源：G 会话第 10 轮 = effort #709（505 分桶的生命周期深化）/ [T1018](../../.wayfinder/tickets/T1018-experiment-expiry.md) / [T1019](../../.wayfinder/tickets/T1019-experiment-expiry-verify.md) / impl 609。

## Problem

505 ExperimentBucketer 的实验表是静态的——实验只该跑两周，但下线靠人记得改 yml 重启。忘下线的实验继续分流曝光：用户被分进已经出结论的变体（体验不一致），效果归因数据被无意义的曝光稀释。GrowthBook/Statsig/LaunchDarkly 的 feature expiry 是标配语义：到期自动回归默认态。

## Solution

- 构造器扩两参：`ExperimentBucketer(experiments, expiresAt, clock)`——每实验可选 `Instant` 到期时刻；原单参构造委托空表+systemUTC（零变化）。
- **assign() 到期语义**：now > expiresAt → 按未入组处理（返回 null），但曝光计入独立 `__expired__` 桶（不混 `__unenrolled__`——「到期拦截」与「自然余量」口径分离）+ `buzhou.experiment.expired` 计数 + 每实验一次 WARN（不刷屏）。
- **读数**：`expiredExperiments()`（Set 视图）/ `expiresAt(name)`（Optional）。
- 未到期/无到期声明的实验语义逐字节不变。

## User Stories

1. 实验收尾：声明 expiresAt=下周五——到点自动停止分流，无人值守；`__expired__` 计数证明「到期后还有多少流量试图进入」。
2. 审计：expiredExperiments() 一屏看到历史实验集合（声明保留、行为停用）。

## Implementation Decisions

- 到期判定在 assign() 惰性执行（无后台线程——housekeeper 族同取舍）。
- Clock 注入（520/509 同先例——测试零真实等待）。
- expiresAt 值为 null → IllegalArgumentException（fail-fast——「想声明到期」与「忘填时刻」必须显式区分）。

## Testing Decisions

- MutableClock：未到期 assign 正常入变体；推进 1ms 后 assign 返回 null + `__expired__` 计数 + expiredExperiments 包含该实验。
- 无到期声明实验推进后照常。
- expiresAt 含 null 值构造拒绝；expiresAt(name) 读数往返。

## Out of Scope

- yml 装配集成（505 Binder 直读绑定面——需要键设计：expires-at 与变体权重同 namespace 有歧义，留后续轮专项）。
- 到期自动删声明（审计需要保留）。

## Further Notes

与 R11（505 的 A/A holdout 层）同族后续；本面先立生命周期原语。
