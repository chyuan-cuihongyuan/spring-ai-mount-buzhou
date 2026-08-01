# 会话入口 API 形态

Type: grilling
Status: resolved
Blocked by: 03

## Question

业务方如何拿到一个挂好 Harness 的 Agent 会话：文章提到 `spawn()` 拉取 `(appId, agentName)` 绑定的技能清单——入口 API 是 `AgentRuntime.spawn(appId, agentName, sessionId?)` 形态，还是 Spring 风格的 Builder/自动装配？与 Spring AI `ChatClient` 的关系（包装、委托还是 Advisor 注入）？会话对象的生命周期方法（close/cancel/timeout）如何暴露？同步/流式两种调用形态是否都支持？

## Answer

**定案：双层 API + 显式生命周期 + 事件监听器通道 + 会话租约互斥。**

1. **双层入口**
   - 高层（主表面）：`AgentRuntime.spawn(appId, agentName)` / `spawn(appId, agentName, sessionId)` → `AgentSession`。spawn 时拉取 `(appId, agentName)` 绑定的技能清单、工具集与策略配置，内部组装好 ChatClient + 记忆 + Hook 链。
   - 低层（渐进采用路径）：`Buzhou.enhance(ChatClient.Builder)` / Advisor 组合包，高级用户自行装配。两层共享同一套装配逻辑（core 内的 `HarnessAssembler`）。
2. **AgentSession 方法表面与生命周期**
   - `String chat(String input)`、`Flux<ChatResponse> stream(String input)`——体感对齐 ChatClient。
   - `close()` / `cancel()`：显式谢幕。core 维护**会话作用域资源注册表**（spill 文件、内存缓存、临时 MCP 连接、租约），close/cancel/idle 超时均触发成套清理；idle 超时由框架后台回收，时长可配。
   - close 后同 sessionId 可再 spawn 续接（资源重建、历史从库加载）。
3. **异步事件透出**：会话级事件监听器 `session.addEventListener(...)`——HITL 确认请求、护栏通知等经监听器透出；SSE/WS 传输由业务桥接，库不绑 Web 框架。
4. **sessionId 语义**：传入已有 sessionId = 续接（加载历史 + 摘要 + 会话 state）；缺省由框架生成。sessionId 直接作为 ChatMemory 的 conversationId（Spring AI 2.0 起必填，正好对齐）。
5. **并发互斥**：同一会话同时只允许一个活跃 AgentSession——第二个 spawn 同 sessionId 默认失败（可选 `steal` 夺权），由持久层**会话租约**实现（租约存储归 ticket 06 的持久化 SPI 范围）。

### 影响面

- ticket 06（持久化 SPI）范围扩大：除消息/摘要/evidence 外，还需覆盖**会话 state**（联动闭环、HITL 授权标记）与**会话租约**两类存储。
- ticket 25（HITL 守卫）的确认事件透出通道已定：会话事件监听器，不绑 Web 框架。
