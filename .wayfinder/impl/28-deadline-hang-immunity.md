# 28 — core · Turn Deadline 贯穿 + 挂起免疫 + 故障注入构件

**What to build:** 一个永不返回且不响应中断的工具再也无法挂死会话：Turn 预算成为对象化 Deadline（绝对时刻、剩余时间传递），工具派发取 min(单工具超时, Turn 剩余)，外层 join / 组锁 / 许可等待全部限时化；超时按 TIMEOUT 语义回喂收尾。同时交付 FaultInjectingToolCallback 测试构件（delay/failRate/hangForever/leakResource），作为全 effort 韧性测试的唯一故障源。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] TurnDeadline 值对象（绝对时刻、remaining/isExpired、组合器）进 core api；SpawnOptions/TurnLoopPolicy 可配 turnDeadline 与 loopTimeout
     （入口选在 TurnLoopPolicy——其已随 RuntimeConfig→HarnessAssembler 流经 advisor/manager/session 全链，loopTimeout 双生参数同宿；SpawnOptions 只承载 steal/listeners 不达工具层）
- [x] 工具派发时限 = min(toolTimeout, deadline 剩余)；嵌套传递剩余时间
- [x] 三个永久阻塞点修复：外层 join 限时（超时=TIMEOUT outcome 回喂）、组锁 tryLock(timeout)、许可 tryAcquire(timeout)
- [x] 模型调用在 Deadline/loopTimeout 配置时受剩余时间兜底
      （Spring AI ChatClient 无 per-call 超时面：chat() 用守卫虚拟线程 + CompletableFuture 限时等待，硬上界 = 预算 + 5s 收尾宽限；stream() 用 Flux.timeout 同界兜底）
- [x] FaultInjectingToolCallback 随 core test-jar 发布（五类故障）
- [x] examples 端到端：hangForever 工具在有限时间内得到超时反馈、Turn 正常收尾不僵死（FakeChatModel 驱动）
- [x] ToolSetSpec 的 connect/request timeout 在工具执行器被消费
     （勘察结论：消费点在 buzhou-mcp 连接层——requestTimeout→McpSyncClient 按 RPC 生效（含工具调用请求）、connectTimeout→HTTP 传输建连；core 侧 ToolCallback SPI 无超时面，以 min(单工具超时, Deadline 剩余) 在派发层统一封顶，消费关系已在 ToolSetSpec Javadoc 文档化）
