# 907 — SnapshotMessage 补测与跨模块复核（R5）

**What to build:** SnapshotMessageTest（compact 构造 null 防御 + 防御拷贝）；六小模块 miss≥1 口径隔离复扫清单。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] core：SnapshotMessageTest（2 用例：null→空 Map / 防御拷贝 + 字段透传）
- [x] spec 1204 + README 行
- [x] 六小模块隔离复扫：tools/observability/observe-otel/observe-dashboard/spill/resilience 双口径（zero miss≥1 + low）**零浮出**——优于预期「清单化归 R6+」，R6 无遗留

## Done

验证：SnapshotMessage 清零；core miss≥1 口径正式收官（残留仅 R1 豁免的 SmartLifecycle 匿名类）；合计 3391 用例 0 失败。commit 见本轮 `test(core)` 提交。
