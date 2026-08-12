# 04 — SmartLifecycle 装配: `buzhou.shutdown.*` 配置 + 相位 + 超时派生

**What to build:** core autoconfig 注册 drain 生命周期 bean（`@AutoConfiguration` + `@ConditionalOnProperty("buzhou.shutdown.enabled")` 默认开，safe-by-default；`@ConditionalOnMissingBean` 允许用户覆盖）；新增 `buzhou.shutdown` 配置属性 record（`enabled` / `drain-timeout`，boxed 类型 + compact constructor 兜默认值，对齐 `BuzhouRecoveryProperties` 模板，**禁止**裸读 Environment）；超时缺省派生自 `spring.lifecycle.timeout-per-shutdown-phase`、显式配置优先、两者皆无用保守默认常量（禁魔法数字）；SmartLifecycle `stop()` 触发与编程式入口**同一** drain 编排实现（Spring 只是触发器）；相位先于观测异步管线排空（drain 事件不丢）、与 web 容器关闭相位的先后关系按 Boot 4 相位常量定值并文档化。装配测试走 `ApplicationContextRunner` 既有形态。

**Blocked by:** 03（完整 drain 协议就绪后才接 Spring 触发器）

**Status:** ready-for-agent

- [ ] 默认装配 drain 生命周期 bean；`buzhou.shutdown.enabled=false` 时不装配
- [ ] `buzhou.shutdown.drain-timeout` 绑定生效；缺省时从 `spring.lifecycle.timeout-per-shutdown-phase` 派生；两者皆无走默认常量
- [ ] `ApplicationContextRunner` 测试：`context.close()`（或生命周期 stop）触发 drain——预先 spawn 的会话被 close、其后 spawn 抛拒新异常
- [ ] drain 相位先于观测管线排空（drain 事件全部进管线后再排空），相位定值在代码注释注明 Boot 4 依据
- [ ] 与 web 容器关闭相位的先后关系文档化（默认取值 + 理由）
- [ ] 重复 stop / 停机中再收信号幂等（复用 01 的 drain 幂等）
- [ ] 配置属性经 `@ConfigurationProperties` record，无裸 `Environment` 读取，无魔法数字
