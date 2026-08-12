# 03 — 超时强杀: 预算耗尽走取消传播，会话仍正常关闭

**What to build:** drain 总预算耗尽时仍在轮次中的会话，经既有 `session.cancel()` → `HarnessToolCallingManager.cancelInFlight()` 取消传播强杀当前轮次（工具层 `future.cancel(true)` 中断语义），随后**仍走正常 close 路径**（flush → 停心跳 → 释租约）——被强杀会话与正常关闭会话的可接管性一致，同 sessionId 可立即在另一 runtime spawn 续接。`drain-timeout-force-kill` 事件带被强杀 sessionId 列表；`drain-finished` 计数区分等完/强杀。**诚实边界**：模型调用阶段无取消句柄（探查事实），强杀只能等模型层自身超时返回——此边界写进代码注释与事件语义，不伪造能力。

**Blocked by:** 02（等待机制——强杀是等待预算耗尽的分支，复用其 latch 计数与汇总）

**Status:** ready-for-agent

- [ ] e2e：不释放 latch、drain 超时后取消传播到达工具层（工具收到中断语义），drain 在预算附近返回
- [ ] 强杀后会话仍被正常 close：租约已释放，同 sessionId 可立即在新 runtime spawn 续接（双 runtime 共享 stores 验证）
- [ ] EXIT 档下被强杀会话的缓冲写仍被 flush（close 路径一致）
- [ ] `drain-timeout-force-kill` 事件带全部被强杀 sessionId；`drain-finished` 等完数/强杀数正确
- [ ] 预算耗尽与「最后一会话恰好完结」的竞争边界有确定性测试（latch 控制时序，无 sleep）
- [ ] 模型调用阶段不可强中断的边界在代码注释 + 机制文档口径中如实表达
