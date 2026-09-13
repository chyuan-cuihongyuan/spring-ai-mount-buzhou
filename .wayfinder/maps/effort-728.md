# effort #728 — spill 配对健康面接线（707 扩散）

- 会话：G 会话 700 系第 29 轮 ｜ spec [728](../../../docs/spec/728-spill-pair-health.md) ｜ 票 [T1056](../tickets/T1056-spill-pair-health.md)/[T1057](../tickets/T1057-spill-pair-health-verify.md) ｜ impl628
- 借鉴：—（707 接线，548 同型）

## 勘察（排重）

- 707 audit 是原语；SpillHealth（写探针）不覆盖配对完整性；mechanism=spill-pair 零命中。

## 决定

`SpillPairHealth`（spill/config，implements BuzhouHealth）：禁用 UNKNOWN（BuzhouHealth 语义）/启用恒 UP；details 四项（dataFiles/metaFiles/dataBytes/dataWithoutMeta+metaWithoutData）；装配随 BuzhouSpillHealthAutoConfiguration（root-dir 与 SpillProperties 同源）。

## 测试

禁用 UNKNOWN+disabled 详情/启用 UP+孤 data 计数+字节精确。

## 诚实边界

details 全目录树扫描（健康端点低频可接受——超大目录宿主降频）；不自动清理。
