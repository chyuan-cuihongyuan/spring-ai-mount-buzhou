# 44 — resilience 模块移植与装配接线

**What to build:** 引入 buzhou-resilience 后，`buzhou.resilience.*` 打开即获得模型调用重试（分类驱动+指数退避+deadline 取消在飞）、超时、onModelError、流式韧性与 RPM/TPM 限流（OverloadPolicy 两档）；模块进 reactor/BOM/starter，装配与编程式两条路径都可用。

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] 从 Future-needs-to-be-supplemented 分支移植 buzhou-resilience 全部主代码+测试，适配 main 当前 core API（编译通过、30+ 测试绿）
- [ ] 日志基线：重试 WARN（分类+下次退避）、限流拒绝 INFO、重试耗尽 ERROR
- [ ] ResilienceProperties JSR-303 校验 + additional-metadata；FailureAnalyzer（deadline < maxBackoff 等矛盾组合翻译）
- [ ] 健康（BuzhouHealthIndicator：重试耗尽/限流拒绝/最近错误分类）+ 指标并入 core MeterBinder 家族
- [ ] reactor（根 pom 模块表）+ BOM 条目 + starter 聚合 + AutoConfiguration.imports
- [ ] examples 端到端：429 Retry-After 退避、deadline 取消在飞、认证错不重试（移植分支 e2e 并验证）


## Done

commit: 见 git log（impl/44）。验证：buzhou-resilience 50/50 绿（含 6 个新 hardening 测试）、buzhou-core 219/219 绿。
落地：模块从分支移植 + core 最小适配（SessionAssemblyContext.emitEvent SPI / HookAdvisor onModelError 切面 / BuzhouHook.onModelError 默认方法 / ModelCallContext.error() / HookChain.onModelError / ScriptedChatModel.enqueueThrow / core.backpressure.OverloadPolicy）；
加固：ResilienceStats 运维面（健康委托+计数）、日志基线（重试 WARNING/耗尽 ERROR/限流拒绝 INFO/超时 WARNING）、指标族 buzhou.resilience.* 预注册进 core BuzhouMetricsBinder、ResilienceProperties fail-fast（JSR-303 + Duration 显式校验 + overload-policy 非法即抛）、BuzhouResilienceHealthAutoConfiguration、reactor/BOM（含 core test-jar）/starter 接线、配置元数据 additional-json。
