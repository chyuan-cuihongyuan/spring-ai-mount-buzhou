# 01 — 韧性地基 tracer bullet: 租约心跳 + 崩溃交接 e2e 骨架 + VOID 恢复语义显式化/事件化

**What to build:** 进程在轮次中途崩溃后，新实例经租约交接获取同一会话、加载并经悬空修复历史、**正确等待用户下一次输入**（不擅自续跑），并在 observability 留下 `turn-recovered{action=voided}` 事件。配套落轮次执行期的**租约心跳**，让「租约过期=崩溃」的检测信号在长轮次上仍然成立（修补当前 `DefaultAgentRuntime` 取租约后从不续约的缺陷）。本票同时确立**崩溃恢复 e2e 测试骨架**（故障注入 ChatModel/工具 + 可调短 TTL 的测试租约 store + 双实例租约交接），后续票据复用。

**Blocked by:** 无 — 可立即开始

**Status:** done

- [ ] 轮次执行期持有租约的实例按心跳间隔 `renew()`，长轮次（> 原 90s TTL）期间租约不被过期回收、不被第二实例 steal（用可调短 TTL 的测试 store 断言，不 wall-clock sleep）
- [ ] 在途轮次「崩溃」（故障注入触发 / 实例放弃租约）后，第二实例 `spawn` 同 sessionId 经租约交接获取会话
- [ ] 新实例加载历史并经 `DanglingCallRepairer` 修复悬空调用后，**不自动发起模型调用**、等待用户下一次输入
- [ ] observability 事件流出现 `turn-recovered{action=voided}`（含被中断轮次标识）
- [ ] e2e 全程断言「最终回复 + 事件流」；计时用 `CountDownLatch` / 可控故障注入，**无 wall-clock sleep**
- [ ] 崩溃恢复 e2e 测试骨架（故障注入 ChatModel/工具 + 短 TTL 测试租约 store + 双实例交接）落 core test-jar，供 04/05 复用
