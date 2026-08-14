# 46 — observability 管线生命周期与指标单口径

**What to build:** 装配路径下观测管线随 Spring context 关闭排空关闭（反复刷新不泄漏）；流取消后 span 落 CANCELLED 终态；落库/sink 失败有 WARN 日志与计数；指标只存在 core MeterBinder 预注册单一家族；非法配置启动失败。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] AsyncObservabilityPipeline bean 化（destroyMethod=close）；Synchronous pipeline doEnqueue 语义对齐（吞+计数+WARN）
- [ ] adviseStream 补 doOnCancel/doFinally → CANCELLED 终态（测试：订阅取消后查 store 无 RUNNING 孤儿）
- [ ] ObservabilityConfig → @ConfigurationProperties + JSR-303 fail-fast
- [ ] MicrometerDualWriter 平行家族删除；queue.wait/persist.errors/duration 并入 core BuzhouMetricsBinder；ObservableToolCallback 走 core 家族
- [ ] 注入快照 evidence 提取最小实现；死代码删除；provider 显式键
- [ ] 测试：Micrometer 家族断言、stream 取消、sink 失败隔离、非法配置、context close 排空
