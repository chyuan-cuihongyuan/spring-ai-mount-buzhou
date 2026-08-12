# 14 — 死循环与失控检测（runaway detection）

> production-readiness gap 08。设计范围：单轮推理的「行为失控」（步数 / 调用次数 / 时长 / 重复）
> 检测与受控终止。蓝本：LangGraph `recursion_limit` + `RemainingSteps`、LangChain v1
> `ModelCallLimitMiddleware`/`ToolCallLimitMiddleware`、OpenAI Agents SDK `max_turns` +
> `MaxTurnsExceeded`（异常携带部分结果）。AutoGen 可组合终止条件代数评估后**不做**（过度设计）。
> 推演标注 `> 【推演】` 见「推演标注」节。

---

## 设计目标

- **数值闸门**：单轮步数 / 工具调用数 / wall-clock / 会话累计 — 双窗口各自可配、各自裁决。
- **按工具护栏**：昂贵/慢工具可单独限额（通配匹配），与廉价工具分流。
- **软退出通道**（差异化亮点）：达软阈值时经既有 Attachment 通道注入「剩余步数预算」信号，让模型**主动收尾**而非被硬切。
- **硬顶携带部分结果**：被终止 ≠ 前功尽弃——已完成的工具调用结果随轮次落库，终止原因作为最终回复回注。
- **确定性重复检测**（M2）：连续 N 次同工具同参数 → 失控；**不做语义相似度**（避免误杀合法分页/轮询）。
- **失控可观测、可转人工**：`runaway.*` 事件族经既有通道发出；硬顶事件携带部分结果指针，业务侧可经既有 HITL 通道转人工。
- **safe-by-default**：机制默认装配，各阈值默认 null=不限，显式配置才生效。

**与花费失控的分工**：本机制管「行为失控」（步数 / 调用次数 / 时长），「花费失控」（token 硬顶 / 预算）归 spec 11 成本治理，两者正交。

---

## 术语

| 术语 | 含义 |
|---|---|
| 轮次级窗口 | 单轮内的步数 / 工具调用数 / wall-clock 硬顶；内存计数，`beforeTurn` 重置 |
| 会话级窗口 | 会话生命周期的累计步数 / 工具调用数；持久化在 SessionStateStore，跨崩溃保留 |
| 软阈值 | 剩余预算占比低于阈值时注入软退出提醒（只注入信号，不递减计数、不阻断） |
| 硬顶 | 达上限强制终止；`beforeModel`/`beforeTool` 返回 `Block(reason)`，reason 成为最终回复 / 工具结果 |
| 部分结果 | 硬顶终止时本轮已完成的工具调用结果（随轮次 unit-of-work 落库） |
| 指纹 | 规范化 `(toolName, canonicalJson(arguments))` 的稳定字符串，用于重复检测 |

---

## API

### RunawayHook（`BuzhouHook` 实现）

挂在既有 Hook 切面，不新增 Advisor / SPI / 事件通道：

- **`beforeTurn`**：重置轮次级内存计数（步数 / 工具调用数 / 按工具计数 / 指纹环缓冲），记录 wall-clock 起点。
- **`beforeModel`**：递增步数；校验 wall-clock（步边界）→ 轮次级步数硬顶 → 会话级累计步数硬顶 → 软阈值事件。
- **`beforeTool`**：递增工具调用数；校验轮次级工具调用硬顶 → 按工具限额 → 确定性重复检测 → 会话级累计工具调用硬顶。

硬顶返回 `HookResult.block(reason)`：
- `beforeModel` Block → reason 经 `HookAdvisor` 既有路径成为本轮**最终回复**（跳过模型调用）。
- `beforeTool` Block → reason 回注为**工具结果文本**（工具未执行），模型据此决定下一步。

**裁决点**：所有计数 / 限额 / 硬顶均挂 `beforeModel`/`beforeTool` 步边界——增量在 `nextCall` / 工具执行**之前**。

### RunawayBudgetRenderer（`AttachmentRenderer` 实现）

软退出提醒渲染器，经既有 `CompositeAttachmentRenderer` 折进 `InjectionViewProcessor` 的 `<system-reminder>` 注入块（与事实块同位）。当 `remaining / maxSteps < softThresholdRatio` 时渲染「剩余步数预算：N/M，请尽快收尾并给出结论」，否则返回空。

**每步刷新**：memory advisor(+400) 每次模型调用重建注入视图 → renderer 每步触发。**诚实标注切面次序**：注入视图在 memory(+400) 构建、步数在 hook(+600) `beforeModel` 递增——本步注入用「上一步末」计数（一步滞后，可接受；模型看到「进入本次调用时的预算」）。

**软阈值语义**：只注入信号、不递减计数、不阻断。注入字符计入既有 `buzhou.facts.max-inject-chars` 共享总量。

---

## 配置项

前缀 `buzhou.runaway`，boxed 类型 null=不限（对齐 `BuzhouBackpressureProperties` 模板）。

| 属性 | 默认 | 语义 |
|---|---|---|
| `enabled` | `true` | 机制总开关；关则完全旁路（等价现状） |
| `per-turn.max-steps` | `null`=不限 | 单轮最大思考步数（模型调用次数硬顶） |
| `per-turn.max-tool-calls` | `null`=不限 | 单轮最大工具调用次数 |
| `per-turn.wall-clock` | `null`=不限 | 单轮 wall-clock 超时（步边界；诚实边界 = wallClock + 单步时长） |
| `per-session.max-steps` | `null`=不限 | 会话累计步数上限（跨崩溃持久化） |
| `per-session.max-tool-calls` | `null`=不限 | 会话累计工具调用数上限（跨崩溃持久化） |
| `per-tool.<glob>.max-calls` | 空=不限 | 按工具名通配的单轮调用上限（exact 优先 → 最长前缀 `*`） |
| `soft-threshold-ratio` | `0.2` | 剩余预算占比低于此值时注入软退出提醒；仅在有 `per-turn.max-steps` 时生效 |
| `repetition.consecutive` | `null`=关 | 连续同工具同参数调用次数阈值（M2；开启后如 3） |
| `repetition.action` | `block` | 重复检测处置：`block`（阻断）/ `flag-only`（仅告警） |
| `escalate-policy` | `emit-event` | 失控处置升级策略（未来 `hitl` / 转人工） |

---

## 事件清单

经既有 `emitEvent` 通道（与 `backpressure.*`/`guard.*`/`drain.*` 同命名约定），同时双重写入 ObservabilityStore（参照 `GuardAuthApi.emitAudit` 先例）。

| 事件类型 | payload | 触发时机 |
|---|---|---|
| `runaway.soft-threshold` | sessionId / turn / counter / limit / remaining | 软阈值触发（每轮仅首次），软退出提醒已注入 |
| `runaway.hard-stop` | sessionId / turn / reason / limit / value | 硬顶终止，携带部分结果 |
| `runaway.per-tool-exceeded` | sessionId / turn / toolName / limit / value | 按工具限额触发 |
| `runaway.repetition` | sessionId / turn / toolName / fingerprint / count | 重复检测触发（M2） |

`runaway.hard-stop` 的 `reason` 维度：`steps` / `tool-calls` / `wall-clock` / `session-steps` / `session-tool-calls` / `repetition`。wall-clock 的 `limit`/`value` 用毫秒数。

---

## 时序

```
用户输入 → beforeTurn（重置轮次计数、启动 wall-clock）
  → 每步 beforeModel（递增步数、校验 wall-clock / 步硬顶 / 会话步硬顶、达软阈值注入提醒、达硬顶 Block 携带部分结果）
  → 每次工具 beforeTool（递增工具计数 / 按工具计数、校验工具硬顶 / 按工具限额 / 重复检测 / 会话工具硬顶、超额 Block）
  → 失控处置全量事件留痕 → SRE 在事件流看到全部处置、业务侧可转人工
```

---

## 推演标注

| # | 位置 | 推演点 | 依据 |
|---|---|---|---|
| 1 | wall-clock 诚实边界 | 轮次时长上界 = wallClock + 单步时长（非中途精确打断） | 步边界检测是既有 Hook 能力；中途 watchdog-cancel 为潜在增强 |
| 2 | 确定性重复检测不做语义相似度 | 避免误杀合法分页翻读 / 轮询 / 批量处理循环 | 08 号票决策；LangChain/LangGraph 无内建语义检测 |
| 3 | 事件落 ObservabilityStore 走手动双重写入 | Hook 上下文不暴露 SpanContext/SpanRecorder | `GuardAuthApi.emitAudit` 先例；spanId=null（SessionEvent 不携带 span 身份） |
| 4 | 会话级计数跨崩溃保留 | SessionStateStore 持久化，AUTO_RESUME 不重置 | `recovery.autoresume.attempts` 先例；防崩溃-恢复循环重烧预算 |
| 5 | 软退出注入一步滞后 | renderer(+400) 读「上一步末」计数、hook(+600) 本步递增 | 切面次序；模型看到「进入本次调用时的预算」语义正确 |

---

## 开放问题

- **wall-clock 中途精确打断**：当前步边界检测为主（诚实边界）；独立 watchdog 线程到点调 `session.cancel()` 的中途打断为潜在增强，不在本期。
- **转人工完整机制**：与 spec 15 运行时干预收敛（挂起-回填原语）；当前只交付事件 + 终止语义 + HITL 通道对接。
- **绑定级 / 工具级 policy 四层消费**：同 spec 13 背压 M1 口径，待 policy 消费管线打通；本期按工具 `<glob>` 匹配是 yml 层通配。
- **重复检测状态原地踏步规则**：M2 给「连续同参数」规则；「连续同结果指纹」（结果未变化）留实现期 + 自建评测定案。
- **会话级计数手工重置 API**：默认随会话删除清除、不提供手工重置。
