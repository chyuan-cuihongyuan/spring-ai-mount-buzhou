# 12 — HITL 危险守卫

**What to build:** 危险工具清单配置驱动（name+required-state+hint+confirmation 多选项/带输入控件，通配匹配）；beforeTool 阻断→「等待人工确认」回注→确认事件经会话监听器透出→写回 SessionStateStore→重发或 resume() 放行；授权=工具名+参数指纹、一次性默认可配长效、授权/撤销记 Event、跨实例放行；取消响应 Hook（beforeModel 查取消标记）。

**Blocked by:** 04, 02, 03

**Status:** ready-for-agent

- [ ] 未授权危险调用物理走不通（BLOCK+确认事件）有端到端测试
- [ ] 确认写回 state 后重发放行；一次性授权第二次再问、长效不再问
- [ ] 续跑打到另一实例（共享 store）仍正确放行
- [ ] 确认事件 schema 含多选项+单输入+hint 嵌 diff 文本
