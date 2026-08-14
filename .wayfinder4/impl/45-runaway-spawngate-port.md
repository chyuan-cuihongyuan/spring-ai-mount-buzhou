# 45 — runaway 失控检测与 SpawnGate 容量闸移植

**What to build:** 单轮步骤数/每工具调用数/墙钟/会话累计双窗口四层硬顶触发时模型收到软退出预算文案并有序收尾，确定性重复检测拦截复读机；会话并发满载时 spawn 抛 SessionCapacityExceededException 拒绝新会话；两者经 `buzhou.runaway.*`/`buzhou.backpressure.*` 可配且默认安全。

**Blocked by:** 44-resilience-module-port

**Status:** done

- [ ] core/runaway（RunawayHook/RunawayCounters/RunawayBudgetRenderer/双窗口）移植适配 main hook 链与 TurnDeadline 语义（预算合成不打架：deadline 优先硬停，runaway 软退出先行）
- [ ] core/backpressure（SpawnGate/OverloadPolicy/SessionCapacityExceededException）移植适配 main session API；refuse-new 在 drain/关闭态同样生效
- [ ] BuzhouRunawayProperties/BuzhouBackpressureProperties JSR-303 + 元数据 + AutoConfiguration 接线
- [ ] 健康（失控触发数/容量拒绝数）+ 指标 + 日志（软退出 WARN、容量拒绝 INFO）
- [ ] examples 端到端：失控脚本触发四层各一例（FakeChatModel 剧本）、重复检测、容量闸满载拒绝（移植分支 691 行 e2e 精选适配）
- [ ] 分支 spec 14-runaway/13-backpressure 有效内容并入 docs/spec/15（与 44 的 resilience 机制详设同篇或分节）


## Done

commit: 见 git log（impl/45）。验证：buzhou-core 248/248 绿（+23 runaway e2e/renderer + 6 SpawnGate e2e 新增）。
落地：core/runaway（RunawayHook/RunawayCounters/RunawayBudgetRenderer，阈值 safe-by-default）+ SpawnGate（适配 main：RuntimeDrainingException→SHUTDOWN_INTERRUPTED 结构化异常、steal 不占容量、装配失败归还空位、停机唤醒排队者）+ BuzhouRunaway/BackpressureProperties 装配（ObjectProvider 惰性取 store 不破坏 store.type 校验路径）+ 指标（buzhou.runaway.hard-stops / buzhou.backpressure.spawn-rejected 预注册）+ WARN/INFO 日志 + 12 条配置元数据。分支 crash-recovery/graceful-shutdown 未移植（决议 T70）。
