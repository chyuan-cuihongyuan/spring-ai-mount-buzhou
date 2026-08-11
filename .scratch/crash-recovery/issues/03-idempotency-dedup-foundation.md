# 03 — 幂等去重地基: 声明扩展 + 框架默认键 + 去重记录原子语义（同键第二次不重执行）

**What to build:** 工具可声明幂等性（既有 `@BuzhouTool.idempotent` 扩到全部工具，副作用工具默认非幂等）；框架按 `{sessionId, turnSeq, toolCallId}` 生成**默认幂等键**；执行脊柱在工具调用前后包一层**去重记录**（调用前 reserve、成功后回填，复用 per-session 存储 SPI 的原子 put-if-absent 语义）。同会话内同键的第二次调用**命中去重记录、返回首次结果、不重执行**——用工具调用计数器从外部证明恰好一次。

**Blocked by:** 无 — 可立即开始（与 01/02 并行）

**Status:** done

- [ ] `@BuzhouTool.idempotent` 的收集从「仅原子工具」扩到全部工具；装配方沿用 `RuntimeConfig.idempotentToolNames` 通道；副作用工具默认非幂等
- [ ] 框架默认幂等键 = `{sessionId, turnSeq, toolCallId}`；纯函数单测覆盖默认键派生
- [ ] 执行脊柱在工具调用前以幂等键 reserve 一条 pending 记录、调用成功后回填结果（「工具已执行、消息未落库」窗口内结果已被捕获）
- [ ] per-session 存储 SPI 扩展**原子 put-if-absent / reserve-then-fill** 语义；契约测试覆盖内存（CAS）/ jdbc（行级）/ redis（Lua）三后端
- [ ] 同会话内同键的第二次调用命中去重记录、返回首次结果、不重执行；工具调用计数器断言 == 1
- [ ] 去重命中进 observability 事件流（`dedup-hit`，含键 + 工具名）
