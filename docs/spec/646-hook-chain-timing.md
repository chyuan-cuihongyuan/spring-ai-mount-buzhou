# 646 — hook 链 per-hook 耗时观测

> 来源：F 会话第 47 轮 = effort #600 / [T942](../../.wayfinder/tickets/T942-hook-timing-shape.md) / [T943](../../.wayfinder/tickets/T943-hook-timing-verify.md) / impl 499。借鉴：Spring Boot Actuator `http.server.requests` per-endpoint 分布、OTel per-span 计时。

## 背景

HookChain 八个回调面（before/afterTurn、before/afterModel、onModelError、before/afterTool、fireEvent）同步内联在 Turn 主链路。零耗时观测：慢 hook（用户 hook 里一次重查询 / 远程调用）拖慢所有 Turn 时，TurnStallWatchdog 只见 Turn 级症状，无法定位是哪个 hook——「换掉哪个 hook 能救回延迟」无从答起。

## 目标

- HookChain 内嵌 per-hook 计时：每回调面包裹 `System.nanoTime()`，`ConcurrentHashMap<hookName, Timing>`（LongAdder `count`/`totalNanos` + volatile `maxNanos`）累计。
- `stats()`：不可变快照（hook 名 → count / totalNanos / maxNanos；调用方可换算均值与占比）。
- **慢 hook 告警**：单次超 `SLOW_HOOK_WARN_THRESHOLD_NANOS`（100ms 常量）输出 WARN（含 hook 名、回调面、耗时），**原子去重**——每 hook 只在「首次」慢时告警一次（此后只累计不刷屏；TransformingToolCallback fail-open 同款模式）。
- 默认开：纯观测零行为变化；计时开销纳秒级（nanoTime + LongAdder）。

## 非目标

不注入延迟（无超时截断——hook 护栏语义敏感，跳过即护栏失效；只观测）；不做 yml 阈值配置（100ms 量级常量足够，细调留后续扩散轮）。

## 测试

慢/快 hook 混合链计数（slow 的 count/total/max 非零、max ≥ sleep）；多次调用累计；block 路径计时不影响返回语义（既有用例零回归）；stats() 不可变。

## 兼容性

默认开纯增量；HookChain 既有构造与公共签名不变。
