# 15 — MCP 热插拔

**What to build:** ToolSetProvider SPI（清单：名称/传输/端点/超时/绑定）+properties/DB 实现；starter 之上 McpClientRegistry；差量刷新只动变化项（spec 变更=删旧增新）；引用计数+延迟关闭（引用归零或 grace 30s 关闭、5min 强杀+Error Event、新调用走新清单）；热更事件进可观测层。

**Blocked by:** 02, 04

**Status:** ready-for-agent

- [ ] 配置变更后新工具可调、被删工具新调用不可见（不重启）
- [ ] 在途调用持旧连接完成后才关闭；超时强杀记 Error Event
- [ ] 未变化条目连接零重建有断言
- [ ] 绑定级清单变更不影响连接（bindings 变更不动连接）
