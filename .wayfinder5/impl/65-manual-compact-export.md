# 65 — 手动 compact + 摘要导出（T90 决策落地）

**What to build:** memory `compact/ManualCompactor`（压缩管线抽取 + 导出）+ CompactNowTool 委托重构 + MemoryModule.manualCompactor() + auto-config bean + 测试。

**Blocked by:** None.

**Status:** done

- [ ] ManualCompactor：compact(sessionId) → CompactResult（folded/generation/estimatedTokens/skipped/错误）；exportSummary / exportSummaryMarkdown；幂等（alreadyCovered + summarizedIds）
- [ ] CompactNowTool 重构为委托 ManualCompactor（输出文本格式不变）
- [ ] MemoryModule.manualCompactor() + BuzhouMemoryAutoConfiguration bean
- [ ] 测试：宿主触发压缩（统计/幂等/无历史 skipped）、导出（类型化 + Markdown + 无摘要 empty）、CompactNowTool 回归不变

## Done

验证：`./mvnw -pl buzhou-memory clean test` 86/86 绿（新增 ManualCompactorTest 3 用例：宿主压缩统计+幂等/导出类型化+Markdown+empty/空历史 skipped+工厂装配；CompactNowToolTest 既有 3 用例回归绿——委托重构行为零变化）。
落地：`compact/ManualCompactor`（compact→CompactResult 统计对象、exportSummary/exportSummaryMarkdown、幂等水位复用、无锁并发安全）+ CompactNowTool 委托重构（输出文本格式保持）+ `MemoryModule.manualCompactor(stores, summaryModel, yml)`（无摘要模型=null）+ BuzhouMemoryAutoConfiguration bean（@Qualifier buzhouSummaryChatModel）。
