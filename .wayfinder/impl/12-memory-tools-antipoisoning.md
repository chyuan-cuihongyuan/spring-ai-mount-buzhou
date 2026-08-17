# 12 — memory · memory-as-tools 自愈记忆 + 防投毒

**What to build:** 模型可用精确匹配工具修正自己的摘要段错误（自愈），写入带 provenance 与 taint 位、untrusted 内容未经脱敏不得进摘要正文、写操作全量审计——防投毒水位超越 Unit 42 公开建议。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：ReviseSummarySectionTool——精确匹配+EDIT_NOT_FOUND/EDIT_AMBIGUOUS 类型化错误+P0 只读锁+taint 门（spotlighting 包裹/交织标记/canary → REJECTED_UNTRUSTED）+全量审计台账（provenance/taint/裁决）；MemoryModule 默认注册+开关；ReviseSummarySectionToolTest 3 例；spec 01 配置表同步）

- [ ] `revise_summary_section(section_id, old_text, new_text)`：精确匹配 + 类型化错误 `EDIT_NOT_FOUND`/`EDIT_AMBIGUOUS`（多处命中拒绝）
- [ ] P0 段只读锁（物理拒绝写入）
- [ ] 每次写入带 provenance（来源 message id）+ taint 位；evidence 源自工具输出的内容标 untrusted，未经脱敏不得进摘要正文、只进 scope 受限 evidence 区
- [ ] 记忆写操作全量审计日志（为 AAT 链预留接入点）
- [ ] 端到端：①正常自愈流程 ②注入载荷试图写入摘要正文被 taint 拦截
- [ ] spec 01（记忆压缩）同步

> spec 12 §memory-10；[T38](../tickets/T38-memory-tools-antipoisoning.md)。源：letta 24,230★（唯一性检查防静默覆写）+ Unit 42 注记（摘要 LLM 调用投毒面）。
