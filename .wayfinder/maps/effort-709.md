# effort #709 — 实验到期自动停

- 会话：G 会话 700 系第 10 轮 ｜ spec [709](../../../docs/spec/709-experiment-expiry.md) ｜ 票 [T1018](../tickets/T1018-experiment-expiry.md)/[T1019](../tickets/T1019-experiment-expiry-verify.md) ｜ impl609
- 借鉴：GrowthBook（≈7K；同思想 Statsig/LaunchDarkly 文档惯例）feature expiry——过期 flag 自动回归默认态

## 勘察（排重）

- ExperimentBucketer（505）静态实验表无生命周期——实验该下线时只能改 yml 重启；忘下线=曝光照跑+污染效果归因。
- grep expiry/ExpiresAt：零命中。

## 决定

构造器扩 `Map<String, Instant> expiresAt`（原单参构造委托空表零变化）+Clock 注入（既有构造 systemUTC）；assign() 过期实验→按未入组处理（返回 null、曝光计 `__expired__` 独立桶不混 `__unenrolled__`）+每实验一次 WARN+`buzhou.experiment.expired` 计数；读数 `expiredExperiments()`/`expiresAt(name)`。原语轮（yml 面集成留后续——505 装配是 Binder 直读，加键要动绑定面）。

## 测试

过期返回 null+计数入 __expired__/未到期照常分桶/无到期实验零影响/构造校验（expiresAt 值 null 拒绝）。

## 诚实边界

到期不删实验声明（观测面仍可见——审计需要）；时区=Clock 注入口径（默认 UTC）；原语轮不做 yml。
