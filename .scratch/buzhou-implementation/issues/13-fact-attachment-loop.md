# 13 — Hook→state→Attachment 闭环

**What to build:** 通用事实模型（key/value/producer/createdTurn/ttl）+fact.*/auth.* 命名空间；FactCollector 三要素脚手架（判定器/渲染器/ttl）；注入视图构建时未过期事实渲染为 system-reminder 块插近期原文前（摘要块在前、事实块随后）；事实写入摘要 Current State 段；注入 token 计系统侧固定扣除。

**Blocked by:** 07

**Status:** resolved

- [x] 注册一个采集器后，工具调用触发的事实下一轮出现在注入视图（端到端）
- [x] ttl=1 一次性消费与 ttl>1 累积注入两种语义有测试
- [x] 压缩发生后事实仍经摘要 Current State 段保留
- [x] 判定器从入参判定语义（非硬匹配工具名）的示例可用

## Answer

Hook→state→Attachment 闭环落地（跨 buzhou-core/guard/memory 三模块）：

**事实模型**（buzhou-core SPI）：`Fact(key, value, producer, createdTurn, ttl)` record；`FactStore` 门面（建在 SessionStateStore 上）封装 `fact.{producer}.{name}` key 命名空间 + JSON 序列化 + ttl 轮次过滤（`currentTurn - createdTurn < ttl` 视为未过期）。`DefaultFactStore` 实现。

**FactCollector 三要素脚手架**（buzhou-guard）：`FactDefinition` 接口（name/judge/render/ttl）；`FactCollectorHook`（afterTool, order 200）遍历注册的采集器，对 judge 命中的事实 → FactStore.save（带 ttl）。判定器从**入参**判定语义（蓝本例：带 tableId 的 upsertTable 才是改表，新建表不触发）。

**Attachment 注入桥接**（buzhou-core SPI）：`AttachmentRenderer` 接口（`render(sessionId, currentTurn) → Optional<String>`）。guard 提供 `FactAttachmentRenderer` 实现（扫描 activeFacts → 逐条 render → 合并文本）；memory 的 InjectionViewProcessor 持有可选引用。

**注入视图渲染**（buzhou-memory）：`InjectionViewProcessor.setAttachmentRenderer`；`assembleWithSummary` 在摘要块之后、近期原文之前插入事实 `<system-reminder>` 块（metadata `facts=true`）；无摘要但有事实时也注入（injectSummaryOnly 放宽 null-summary 短路）。

**摘要 Current State 段写入**（buzhou-memory）：`NineSectionSummary.appendCurrentState(text)` 把未过期事实追加到 CURRENT_STATE 段（P0 死保，压缩不丢现场）；InjectionViewProcessor 在渲染摘要前 enrich。

**GuardModule 集成**：builder 增 `factDefinition(FactDefinition)` 注册采集器；自动装配 FactCollectorHook + FactAttachmentRenderer；`attachmentRenderer()` 暴露给 memory。`MemoryModule.configure` 增 AttachmentRenderer 重载。

**验收**：四项 checklist 覆盖——`FactInjectionTest`（memory，4 项：activeFact 注入 system-reminder 块、ttl=1 一次性消费、ttl>1 累积注入、appendCurrentState P0 保留）；`FactCollectorHookTest`（guard，2 项：判定器从入参判定语义 + ttl 存储）；`DefaultFactStoreTest`（core，6 项：save/activeFacts/ttl 过滤/命名空间/排序）。全量 mvn test 通过（core 63 / memory 27 / spill 59 / observability 23 / guard 21）。

**推演偏离（记入 Comments）**：
- 注入 token 精确计入预算（spec「系统侧固定扣除」）：本票简化为通过 AttachmentRenderer 文本承载，未接 BudgetReport 专项字段（systemPrompt 字段当前未用，后续接）。
- maxInjectChars 截断 + 指针：简化为不截断（长会话存储压力实测后决策）。
- 事实写入摘要的 LLM 提示词融合：简化为程序化 appendCurrentState（合并后追加，不重新调 LLM）。
