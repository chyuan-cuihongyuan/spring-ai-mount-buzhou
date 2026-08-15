# Spec 35 — 韧性/技能目录/媒体摄取增补

> effort #7（T118–T120 / impl-93–95）。

## §A 熔断半开多探测（T118 / impl-93）

- `Circuit.halfOpenSuccessThreshold`（默认 1 = 单探测即恢复的既有行为零变化）：>1 时
  半开需**连续 N 次探测成功**才回 CLOSED——抖动 provider（成功率 ~50%）不再
  「单探测成功→CLOSED→立刻再跳」循环。
- **探测槽位不变量**：`在飞探测数 + 已成功数 ≥ 阈值` 即占位满员拒绝（每成功永久占一槽）；
  任一探测失败立即回 OPEN（trips 递增、退避生效）；逃生窗口（2×生效冷却）外重置槽位。
- 7/6 参便捷构造保留（源/二进制兼容）；配置键
  `buzhou.resilience.circuit.half-open-success-threshold`（fail-fast ≥1）。

## §B skills 目录注入预算（T119 / impl-94）

- **注入上限已有**（registry 层 catalog-max-entries，默认 64）——缺的是**模型可感知**：
  截断后模型不知道还有未列出技能。
- `SkillRegistry.listForPage`（默认 = listFor 全量无溢出，截断实现覆写）返回
  `CatalogPage(entries, total)`；`DefaultSkillRegistry` 覆写为截断 + candidates 全量计数。
- 渲染器溢出提示：「（另有 N 个技能因目录注入上限未列出——如需加载其正文，请运维调整
  绑定关系或提高 buzhou.skills.catalog-max-entries）」。
- **不做评分/skill_search**：评分需查询语义（模型自会按 description 挑）；检索工具属
  新能力面（fog）。

## §C MediaIntake 字节摄取（T120 / impl-95）

- spill 模块 `MediaIntake`（SpillStore 之上）：`intake(bytes, mimeType, agentName, sessionId)`
  → 落 spill（配额/原子写/sha256 meta/随会话级联删全沿用）→ 返回 `MediaRef`（spill:// URI，
  可直接传入 `chat(input, media)`）。
- **二进制保真**：Latin-1 逐字节映射（0-255 双向无损）；`readBack(ref)` 还原原字节；
  `intakeText/readBackText` UTF-8 便捷通道。
- 生命周期：属主 = 会话（级联删除锚）；fork 引用计数语义兼容（spec 26）。
