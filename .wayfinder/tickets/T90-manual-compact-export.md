---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

手动 compact 与摘要导出怎么做？现状：压缩全自动（预算触发），无手动触发 API；摘要无导出接口。借鉴：Claude Code /compact 手动压缩。决策点：API 形态（AgentRuntime.compact(sessionId) 或 session.compactNow()）、与在途 turn 的互斥、导出形态（summary Markdown/JSON 导出接口）、配置与幂等。产出 spec 20 增量 + impl 65。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **API 形态**：宿主侧 `memory` 模块公开 `ManualCompactor`（`compact(sessionId)` + `exportSummary(sessionId)` + `exportSummaryMarkdown(sessionId)`）——压缩机制归 memory 模块，core 无 surgery（session/runtime 不持 memory 组件，硬塞 core API 是伪抽象）。模型侧 CompactNowTool 与宿主侧 ManualCompactor **共用同一条压缩管线**（工具重构为委托 ManualCompactor，行为零变化）。
2. **与在途 turn 的互斥**：M1 不做锁——压缩落点是 SummaryStore（版本化追加）+ summarizedMessageIds 幂等集，与在途轮的 messageStore 追加天然并发安全；文档明示「建议在轮间隙调用，轮中调用安全但摘要可能少折最后一轮」。
3. **导出形态**：`exportSummary` 返回 `Optional<NineSectionSummary>`（类型化）；`exportSummaryMarkdown` 返回渲染文本（九段 Markdown）。无摘要 = empty。
4. **幂等**：与 compact_now 相同（alreadyCovered/summarizedIds 跳过已折入；无待折返回 skipped 结果对象 CompactResult）。
5. **装配**：MemoryModule.manualCompactor()（与 CompactNowTool 同条件：配置摘要模型才可用）+ auto-config bean。
6. **事件**：复用摘要管线既有事件；CompactResult 携带统计（折入数/代际/估算 token）供宿主回报。
