# effort #823 — 启动阶段耗时读数

- 会话：H 会话 800 系第 24 轮 ｜ spec [823](../../../docs/spec/823-startup-phase-timing.md) ｜ 票 [T1147](../tickets/T1147-startup-phase-timing.md)/[T1148](../tickets/T1148-startup-phase-timing-verify.md) ｜ impl576
- 借鉴：Spring Boot ApplicationStartup StartupStep（spring-projects/spring-boot ≈78K star）

## 勘察（排重）

- BuzhouLifecyclePhases：phase 常量声明——无耗时维。
- BuzhouAssemblyReport：ready 时一次性日志——非步骤计时。
- HookTimingAggregator：hook 域运行期计时——装配期缺位。
- grep -i `startup.*step|phase.*timing`：无命中。

## 决定

`StartupPhaseTiming`（core.config）：start(phase)→Step 句柄+end()（volatile 首末幂等）——Clock 注入；快照按开始时刻升序（未结束 duration=-1 哨兵）；步骤封顶 64（超丢+truncated）；空白 phase 返回 null；null clock fail-fast。纯读数面——喂点归应用/装配侧（不侵入 SmartLifecycle 流程）。

## 测试

两阶段时长精确（120/45）/未结束 -1 哨兵+end 幂等首末/封顶 64+overflow null+truncated+升序/空白忽略/clock fail-fast——5 例全绿。

## 诚实边界

喂点手动（不自动埋点装配流程——读数纪律）；毫秒粒度（Clock.millis——启动级耗时足够）；无跨进程聚合（启动一次性语义）。
