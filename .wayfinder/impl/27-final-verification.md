# 27 — 收口 · 全量验证 + 落地记录回写

**What to build:** 全部切片落地后的收口验证与文档回写：全模块 clean verify、spec 12 落地记录、机制 Spec/边界文档一致性、wayfinder2 图收口。

**Blocked by:** 01–26（全部实现切片）

**Status:** done（2026-08-14：`mvn -B -ntp clean verify` 16 模块 BUILD SUCCESS（576 tests / 0 fail / 0 err / 30 skip 门控）；spec 12 落地记录回写；机制 Spec 01/02/05/07 同步；impl README 全量 done；OnFailReask 测试对齐 impl-04 契约（交互缺陷修复））

- [ ] `mvn -B -ntp clean verify` 全模块绿（记录模块数与命令）
- [ ] spec 12 追加「落地记录」（各切片落点 + 交互缺陷修复 + 验证证据，仿 spec 11 体例）
- [ ] docs/spec/10（Spring AI 边界）能力表按新增能力刷新（ADDS 行）
- [ ] README 能力段落与 alpha 状态一致性检查（不夸大措辞）
- [ ] `.wayfinder/impl/README.md` 状态更新、MAP 收口行追加
- [ ] 双轴 code-review（Standards + Spec）过一遍跨切片交互点（仿 spec 11 的三缺陷复盘）

> spec 12 Further Notes「Spec 同步义务」；[T54](../tickets/T54-scope-cut-and-priority.md) 收口。
