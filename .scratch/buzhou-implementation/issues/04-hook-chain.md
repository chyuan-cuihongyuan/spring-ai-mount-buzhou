# 04 — Hook 链框架

**What to build:** 六切面（before/afterTool、before/afterModel、before/afterTurn、onEvent）在 ToolCallback 包装层与两处 advisor 挂通；BuzhouHook 接口 + 密封三态 HookResult（CONTINUE/BLOCK/REPLACE）；Spring Bean 自动收集 + order 编排（内置 0–999/业务 1000 起）+ yml 可禁用指定 Hook；HookContext 支持同链中间态传递与 state 读写句柄。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] 示例 Hook 的 BLOCK 阻断与 REPLACE 替换端到端生效
- [ ] 同切面多 Hook 按 order 执行、yml 禁用生效
- [ ] onEvent 纯通知不可短路有编译期/运行期保证
- [ ] 并行调用下每个 tool_call 独立完整过链（上下文显式传递）
