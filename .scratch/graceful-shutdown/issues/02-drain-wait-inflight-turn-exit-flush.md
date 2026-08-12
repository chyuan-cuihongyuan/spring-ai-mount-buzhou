# 02 — 等在途轮次完结 + EXIT 档 flush 联动

**What to build:** drain 等待每个活跃会话的**当前轮次**完结（粒度=轮次，与微压缩「完结轮次」原子单位对齐；不等整个会话）。轮次在途信号复用既有 `SessionObserver` 轮次边界（onTurnStart/onTurnEnd/onTurnError，实现期验证覆盖 chat/stream/AUTO_RESUME 全部轮次形态）；等待用虚拟线程 + latch 计数（对齐 `HarnessToolCallingManager` fan-out 手法），**禁止**轮询 sleep；单会话等待失败不阻塞其他会话（收集异常、最后汇总）。轮次完结后 drain 主动 close 该会话。EXIT 档联动：drain 关闭会话触发既有 `DurabilityTieredStores.flush` 钩子（同步执行，不依赖后台虚拟线程），缓冲写落盘。`drain-session-completed` 事件带 sessionId + 处置方式（等完）+ 耗时。

**Blocked by:** 01（drain 地基：台账 + 编排入口 + e2e 骨架）

**Status:** ready-for-agent

- [ ] e2e：阻塞工具持有 latch 的在途轮次，drain 不返回；释放 latch 后轮次正常完结、drain 随后返回、工具计数器符合预期
- [ ] 未在轮次中的会话被 drain 直接 close（不等）
- [ ] 轮次在途信号覆盖 chat / stream / AUTO_RESUME 三种轮次形态（实现期核验并测试）
- [ ] `durability-tier=EXIT` 下 drain 完成后，缓冲消息在 store 中立即可见（flush 同步生效）
- [ ] `drain-session-completed` 事件带 sessionId + 等完语义 + 耗时
- [ ] 单会话 close 异常不阻塞其他会话的 drain（首异常收集后汇总抛出/记录）
- [ ] 全程确定性测试（latch + 有界轮询），无 wall-clock sleep
