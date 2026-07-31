# 会话入口 API 形态

Type: grilling
Status: open
Blocked by: 03

## Question

业务方如何拿到一个挂好 Harness 的 Agent 会话：文章提到 `spawn()` 拉取 `(appId, agentName)` 绑定的技能清单——入口 API 是 `AgentRuntime.spawn(appId, agentName, sessionId?)` 形态，还是 Spring 风格的 Builder/自动装配？与 Spring AI `ChatClient` 的关系（包装、委托还是 Advisor 注入）？会话对象的生命周期方法（close/cancel/timeout）如何暴露？同步/流式两种调用形态是否都支持？
