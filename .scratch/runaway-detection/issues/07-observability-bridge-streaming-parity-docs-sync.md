# 07 — 事件落 ObservabilityStore/metering 桥接 + 流式对等 + 文档/spec 同步（收口）

**What to build:** 收口三件事。① **事件落库路径定案**：`runaway.*` 全族达 dashboard（ObservabilityStore）+ Micrometer 计数——二选一（扩展 `HookContext` 暴露 span 走 `SpanRecorder.emit` 直发得免费 metering / 新增 `SessionEventListener`→`ObservabilityStore` 桥接），与既有 `backpressure.*`/`guard.*` 保持一致并注释定案理由。② **流式对等**：`session.stream` 下全部行为（步数/工具调用/wall-clock/会话级/重复检测）同样生效，流式 Block 的回复序列与同步一致。③ **文档/spec 同步**：写 `docs/spec/14-runaway-detection.md` 八节档（对齐既有 spec 模板：设计目标/术语/API/配置项/事件清单/时序/推演标注/开放问题）+ 配置项全表 + `runaway.*` 事件类型全表 + 软退出提醒文案模板（「改机制先改 Spec」）。

**Blocked by:** 01、02、03、04、05、06 — 殿后收口

**Status:** ready-for-agent

- [ ] `runaway.*` 全族事件在 dashboard（`ObservabilityStore.eventsOfSession`）可查 + Micrometer 计数覆盖
- [ ] 落库路径定案有注释说明（SpanRecorder.emit 直发 or SessionEventListener 桥接），与既有事件族口径一致
- [ ] 流式对等：`session.stream` 下步数/工具调用/wall-clock/会话级/重复检测硬顶同样触发（参照 `GracefulShutdownEndToEndTest.drainWaitsForInFlightStreamTurn` 形态），流式 Block 回复序列与同步一致
- [ ] `docs/spec/14-runaway-detection.md` 八节档落地
- [ ] 配置项全表 + `runaway.*` 事件类型全表 + 软退出提醒文案模板同步落 spec
- [ ] 「与花费失控的分工」（行为失控 vs 花费失控，正交）、「wall-clock 步边界诚实边界」、「与幂等去重的协同」写入机制文档
