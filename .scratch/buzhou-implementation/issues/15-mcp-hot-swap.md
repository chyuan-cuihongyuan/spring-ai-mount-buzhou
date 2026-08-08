# 15 — MCP 热插拔

**What to build:** ToolSetProvider SPI（清单：名称/传输/端点/超时/绑定）+properties/DB 实现；starter 之上 McpClientRegistry；差量刷新只动变化项（spec 变更=删旧增新）；引用计数+延迟关闭（引用归零或 grace 30s 关闭、5min 强杀+Error Event、新调用走新清单）；热更事件进可观测层。

**Blocked by:** 02, 04

**Status:** done（454961d，复审双轴修复后全绿；buzhou-mcp 28 测试 + 全仓 mvn verify 通过）

- [x] 配置变更后新工具可调、被删工具新调用不可见（不重启）—— `refreshAddsAndRemovesWithoutRestart` / `snapshotCallbackRejectedAfterRemoval`
- [x] 在途调用持旧连接完成后才关闭；超时强杀记 Error Event —— `inFlightCallHoldsOldConnectionUntilComplete`（graceCompleted）/ `graceExpiryClosesEvenWithInFlight` / `forceCloseEmitsErrorEventWhenCloseStuck`
- [x] 未变化条目连接零重建有断言 —— `unchangedEntryKeepsConnectionUntouched`（connectCount==1）
- [x] 绑定级清单变更不影响连接（bindings 变更不动连接）—— `bindingChangeDoesNotTouchConnection` + `bindingLevelPolicyClipsVisibility`（绑定级清单裁剪）

**复审修复补充记录**（双轴评审后修复，同提交收口）：

- shutdown 泄漏：closeFuture 随 DRAINING 即刻就位，shutdown 等齐全部条目再停调度器（原实现在途条目 closeFuture 为 null 被跳过且 shutdownNow 取消兜底定时器）。
- 摘除条目不再重复记 mcp.removed（diff 循环跳过非 ACTIVE 条目）；关完驱逐出注册表防泄漏（身份比较防误删同名新条目）。
- mcp.closed reason 闭集不篡改：close 抛异常另发 ERROR Event（phase=close）；强杀成功补发 mcp.closed(reason=forceClosed)；spec 04 推演 13 收口。
- 坏配置（清单重名）整批拒绝时记 ERROR Event（phase=refresh），不再静默吞。
- 绑定级清单（buzhou.mcp.bindings.<appId>.<agentName>）落地为可选 PolicyConfigProvider 裁剪层。
- 时长解析提取 internal.Durations 共享助手（消除 McpModule→PropertiesToolSetProvider 的 Feature Envy）。
- 留待后续 ticket：Spring Boot AutoConfiguration 绑定（全仓一致留给 starter/20）；Nacos/Apollo 适配（community-extension）。
