---
Type: task
Status: closed
---
## Question

熔断半开参数自适应（effort #5 fog 毕业）：现 `ModelCircuitBreaker`（T81）冷却时长静态（`openDuration` 固定）、半开单探测恒定。反复跳闸场景（上游持续故障）下固定冷却会造成「冷却→探测→立刻再跳」的无效循环，放大故障传导。需要决策：自适应维度（连续跳闸次数→冷却指数退避 vs 失败率驱动）、参数边界（退避上限、衰减/复位条件）、半开成功率阈值是否随历史调整（恢复置信度）、指标暴露（当前退避倍数）、与 T82 降级链的交互（自适应只影响主备共同冷却口径吗）。产出 spec 25 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **自适应维度：连续跳闸次数驱动的冷却指数退避**——每次 HALF_OPEN 探测失败回 OPEN，`consecutiveTrips++`，冷却 = `openDuration * min(2^(trips-1), backoffCap)`；探测成功（半开通过）即 `trips=0` 复位。失败率驱动需要滑动窗口估算且与既有「计数窗口失败率跳闸」口径耦合，复杂度高净收益低。
2. **参数边界**：`backoffCap` 默认 8（即最长 8×openDuration），可配 `buzhou.resilience.circuit.backoff-cap`；trips 无上限（cap 封顶）。
3. **半开成功率阈值：不做二级自适应**——保持 T81 单探测口径（一次探测成功即 CLOSE）；恢复置信度多探测是 resilience4j halfOpenAdmitted... 的复杂度档，本仓单 Agent 场景收益不抵状态机复杂度。文档记录为 fog（若未来多探测，在此之上加）。
4. **指标/事件**：跳闸事件 payload 增 `consecutiveTrips`、`openDurationMs`（实际生效值）；`ResilienceStats` 增 `circuitBackoffMultiplier`（gauge，按 model 分桶）。
5. **与降级链交互**：自适应冷却完全局部于各 model 的熔断器实例（主备各自退避），T82「主模型 OPEN 恒触发降级」语义不变——无需跨熔断器协调。

### 闭合细化（实现期定稿）

- trips 计数在「进入 OPEN」时统一递增（CLOSED→OPEN 首跳=1，HALF_OPEN 探测失败→OPEN 续计），复位仅在回 CLOSED。
- 生效冷却贯穿 admit/占位拒绝/探测逃生窗口；`Circuit` record 保留 6 参便捷构造（既有调用点零改动）。
- spec 25 落档；fog 记录「半开多探测/失败率驱动动态阈值」为未来候选。
