# 12 — HITL 危险守卫

**What to build:** 危险工具清单配置驱动（name+required-state+hint+confirmation 多选项/带输入控件，通配匹配）；beforeTool 阻断→「等待人工确认」回注→确认事件经会话监听器透出→写回 SessionStateStore→重发或 resume() 放行；授权=工具名+参数指纹、一次性默认可配长效、授权/撤销记 Event、跨实例放行；取消响应 Hook（beforeModel 查取消标记）。

**Blocked by:** 04, 02, 03

**Status:** resolved

- [x] 未授权危险调用物理走不通（BLOCK+确认事件）有端到端测试
- [x] 确认写回 state 后重发放行；一次性授权第二次再问、长效不再问
- [x] 续跑打到另一实例（共享 store）仍正确放行
- [x] 确认事件 schema 含多选项+单输入+hint 嵌 diff 文本

## Answer

buzhou-guard 模块落地（HITL 危险工具守卫）：

**配置模型**（`DangerousToolConfig`）：enabled / authTtl(once|session) / dangerousTools。`DangerousToolEntry(name, requiredState, hint, confirmation)`；`Confirmation(title, options)`；`ConfirmOption(id, label, value, hasInput, inputPlaceholder, inputType)`。通配匹配 = 单星号 glob（精确名 > 最长前缀通配 > `*`）。

**参数指纹**（`ArgumentFingerprint`）：`fingerprint(toolName, arguments) = SHA-256(canonicalJson) 前 16 hex`，规范化 JSON 用 TreeMap 排序键保证稳定；auth state key = `auth.{toolName}.{fingerprint}`。

**DangerousToolGuardHook**（beforeTool, order 300）：
- 命中危险清单后查 SessionStateStore 授权标记；
- 无授权 → BLOCK（工具结果="等待人工确认：{hint}"）+ emitEvent 确认请求事件（schema 含 confirmation.options 多选项+单输入控件+hint 嵌 ${param} 模板渲染）+ 阻断审计事件；
- 命中授权：once → delete auth key（一次性消费）+ 审计事件；session → 保留 + 审计事件 → CONTINUE 放行。

**GuardAuthApi**（业务侧 REST 写回）：approve/reject/revoke/isAuthorized。approve 写 `auth.{toolName}.{fingerprint}` 到 SessionStateStore（共享 store）→ 业务重发同一输入 → 守卫命中 → 放行。跨实例：state 走持久化 store，任意实例续跑可放行。

**GuardModule**：builder 风格（对齐 SpillGuardModule）+ `fromYml(Map)` 解析 `buzhou.guard.*`；`configure()` 返回 RuntimeConfig(hooks=[DangerousToolGuardHook])。

**验收**：四项 checklist 由 `HitlGuardEndToEndTest` 端到端覆盖（未授权阻断+确认事件 schema、一次性授权消费、长效授权多次放行、跨实例共享 store 放行）+ 通配匹配 + 参数指纹粒度；`ArgumentFingerprintTest` 覆盖规范化 JSON 稳定性/SHA-256 前 16 hex/同参同指纹/异参异指纹；`DangerousToolMatcherTest` 覆盖精确>通配>最长前缀。全量 mvn test 通过（core 57 / memory 23 / spill 59 / observability 23 / guard 19）。

**推演偏离（记入 Comments）**：
- `AgentSession.resume()` 框架级便利 API（spec 推演）未实现；本票走业务重发路径（等价语义：approve 后重发同一 chat()，守卫查 state 命中放行）。
- beforeModel 取消响应 Hook（spec 提及）未实现；cancel 已有 `AgentSession.cancel()` + `SessionObserver.onCancel`（ticket 09 已落），非 checklist 项。
- FactCollector + Attachment 注入闭环 = ticket 13。
- yml → ConfigurationProperties 自动装配 = starter（未就绪）；本票用 Map ymlConfig 手绑。
