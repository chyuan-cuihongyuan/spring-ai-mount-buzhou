# 34 — core · 事件背压 + 线程卫生

**What to build:** 慢/坏监听器不再影响主链路：监听器异常逐个隔离；opt-in 有界异步分发（容量 + 溢出策略，丢弃计数可见）；全部线程具名 + 未捕获异常处理器。

**Blocked by:** 29（日志基线）

**Status:** ready-for-agent

- [ ] 同步模式（默认）补逐监听器 try/catch + ERROR 日志 + 计数
- [ ] buffered 模式 opt-in：有界队列容量 + DropOldest|Block(pushTimeout) 策略，持久化/遥测类分别建议
- [ ] 丢弃可见：计数器 + 低频汇总事件（EventBusStats）
- [ ] BuzhouThreadFactory（buzhou-<role>-<seq> + uncaughtExceptionHandler）应用于全部线程创建点
- [ ] DbPolicyConfigProvider 轮询异常：WARN + 指数退避 + 连续失败告警
- [ ] examples：慢监听不拖慢 Turn（buffered）、坏监听被隔离其余照收、丢弃计数可断言
