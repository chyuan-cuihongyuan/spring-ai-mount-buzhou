# effort #816 — 记忆分层容量读数

- 会话：H 会话 800 系第 17 轮 ｜ spec [816](../../../docs/spec/816-memory-hierarchy-capacity.md) ｜ 票 [T1133](../tickets/T1133-memory-hierarchy-capacity.md)/[T1134](../tickets/T1134-memory-hierarchy-capacity-verify.md) ｜ impl569
- 借鉴：MemGPT/Letta 记忆分层（letta-ai/letta ≈18K star）——core/archival/recall 三层架构可视化

## 勘察（排重）

- FactStore/SummaryStore/MessageStore：三层存储存在——容量水位读数缺位。
- StateTtlCoverage（724）：状态域永生键——非记忆三层。
- grep -i `hierarchy|layer.*capacity`：无命中（memory 包 CompactionRatioStats 是压实率）。

## 决定

`MemoryHierarchyCapacity`（memory，纯函数）：Snapshot（调用方自 store 采集）+三 cap 参数→Report 三层固定序（core-summary/archival-facts/recall-window）：items/chars/fillRatio/level；分级 OK<80%≤WARN<100%≤FULL（cap≤0=不设限 ratio null 恒 OK）；脏快照负值归 0 不炸；core 层恒 1 条目（单活跃版本语义）；字符口径与 738/808 一致。

## 测试

三层序+份额+total/水位边界四点（80% WARN、100% FULL、79.9% OK、120% FULL ratio1.2）/混排设限与不设限（50% OK、85% WARN、null cap）/脏快照归零——4 例绿（首跑残留错行 50% 断 WARN——清理后绿）。

## 诚实边界

快照采集归调用方（store 零侵入）；字符口径非 token（一致性取舍）；items 口径：core=1（活跃版本）、facts=活跃条数、recall=消息条数——各自域内可比。
