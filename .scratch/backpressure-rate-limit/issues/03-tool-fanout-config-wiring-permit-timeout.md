# 03 — 脊柱扇出闸: 硬编码参数配置接线 + 许可超时降级

**What to build:** 执行脊柱扇出参数消除硬编码并接线配置——`HarnessAssembler` 中写死的每轮并发上限（8）与工具超时（60s）改由 `buzhou.backpressure.tool.*` properties 注入，**默认保持现值**（行为不变，现值抽命名常量，禁魔法数字）。新增行为：扇出许可获取从无限 `acquire()` 改为有界 `tryAcquire(permit-acquire-timeout)`，超时后该工具调用返回**错误结果**（走既有工具错误结果通道，模型可见「工具过载未执行」语义），不阻断同轮其他工具、不吊死轮次；许可超时独立配置项，两档策略词汇复用 01（fail-fast 档等价许可超时=0）。`backpressure.tool-permit-timeout`（工具名 + 已等待时长）事件进既有通道。既有 serialGroups 串行组语义、`cancelInFlight` 取消传播不动。

**Blocked by:** 01（两档策略词汇与事件语义；机制上与 02 可并行）

**Status:** ready-for-agent

- [ ] e2e：小每轮并发上限下并行工具调用被许可串行化（工具计数器/执行时序佐证）
- [ ] e2e：许可超时后该工具返回错误结果（模型可见失败语义），轮次正常完结、其他工具不受影响
- [ ] 不配置时行为与现状一致（每轮 8 / 60s 现值回归断言）
- [ ] `buzhou.backpressure.tool.max-concurrent-per-turn` / `tool-timeout` / `permit-acquire-timeout` / `overload-policy` 绑定生效
- [ ] `backpressure.tool-permit-timeout` 事件带工具名与等待时长
