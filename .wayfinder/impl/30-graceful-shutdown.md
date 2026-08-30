# 30 — core · 优雅停机与生命周期

**What to build:** 进程收到停机信号后：新 Turn 被拒绝、在途 Turn 按取消模式有序收尾（AFTER_CURRENT_TURN 取消 + 排空等待 + 超时硬截断），租约释放、后台任务 drain；流式订阅者取消后收尾照常；单个 listener 异常不再跳过其余清理。

**Blocked by:** 28（排空测试用故障注入构件）

**Status:** ready-for-agent

- [ ] BuzhouLifecyclePhases 常量；core AutoConfiguration 增 SmartLifecycle（phase 最大：拒新 Turn→对在途发 AFTER_CURRENT_TURN→排空）
- [ ] memory/spill/guard AutoConfiguration 各自 SmartLifecycle（后停：关后台任务/缓存）
- [ ] DefaultAgentRuntime 追踪活跃会话；executor shutdown()+awaitTermination(period)、显式 destroyMethod 防双触发
- [ ] buzhou.lifecycle.timeout-per-shutdown-phase 默认 30s 可配
- [ ] stream() 补 doFinally（cancel/timeout/正常同路收尾：span 关闭、turn 记账）
- [ ] close() 与事件分发逐 listener try/catch（收集异常不跳过清理）
- [ ] examples 端到端：停机期在途 Turn 完成 or 超时截断，资源注册表清空，无异常外溢
