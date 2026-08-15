# impl-94 — skills 目录注入预算

**What to build:** 目录截断的溢出提示（模型可感知未列出技能数与调大途径）。

**Blocked by:** None

**Status:** done

- [x] SkillRegistry.listForPage（CatalogPage entries+total；默认全量无溢出，截断实现覆写）
- [x] DefaultSkillRegistry：先判后加截断 + candidates 全量计数
- [x] 渲染器溢出提示（另有 N 个未列出 + catalog-max-entries 指引）
- [x] 测试：溢出提示 + 上限内条目数 + page.total > entries——skills 63/63 绿；spec 35 §B

## Done

commit：见 git log（impl-94）。
