# Hook 链框架设计

Type: grilling
Status: resolved
Blocked by: —

## Question

Harness 的 Hook（Callback 切面）基础设施怎么设计：暴露哪些切面（参照 DECO 的 beforeTool/afterTool/beforeModel/afterModel/beforeAgent/afterAgent/onRunEvent——映射到 Spring AI 2.x 的 Advisor 链 + ToolCallingManager 包装，各自对应什么扩展点）？Hook 的注册模型（SPI 自动发现 vs 显式配置）、同切面多 Hook 的编排顺序（order 约定）、Hook 的短路语义（返非空结果阻断后续，如 Guard 的 Maybe.just()）？Hook 与 Harness 内部机制的关系——Spill、微压缩、可观测采集是否都实现为内置 Hook（吃自己的狗粮）？业务自定义 Hook 的 API 表面长什么样？

## Answer

**定案：六切面 + Bean 收集 order 编排 + 密封三态短路 + 全机制吃狗粮。**

1. **切面集合（六）与映射**：
   - `beforeTool` / `afterTool` → ToolCallback 包装层（14 已定挂接点）；
   - `beforeModel` / `afterModel` → 循环内 advisor（+400）；
   - `beforeTurn` / `afterTurn` → 会话入口 advisor（DECO beforeAgent/afterAgent 的映射，一轮用户输入的进出）；
   - `onEvent` → 会话事件透出（DECO onRunEvent 映射，HITL 请求/护栏通知等）。
2. **注册与编排**：Hook 为 Spring Bean 自动收集；接口带 `order()`，同切面按 order 升序执行；框架内置 Hook 占预留区间 0–999，业务 Hook 从 1000 起；yml 可显式禁用指定 Hook。
3. **短路语义**：Hook 返回密封三态结果 `CONTINUE` / `BLOCK(reason)`（阻断后续 Hook 与被挂接动作）/ `REPLACE(payload)`（替换入参或出参后继续）；类型显式，胜过 DECO 的 Maybe 空非空约定。
4. **狗粮原则**：Spill（afterTool 长结果 offload）、Onload（beforeTool 写侧加载）、微压缩记录、可观测采集、HITL 守卫、危险工具门禁全部实现为内置 Hook——机制与框架同构，业务可参照、禁用、替换。
5. **业务 API 表面**：`interface BuzhouHook { int order(); HookResult beforeTool(ToolCallContext ctx); ... }`（各切面方法默认 CONTINUE，业务按需重写）；ctx 携带 sessionId/turn/工具名/入参/会话 state 读写句柄（衔接 26）。
