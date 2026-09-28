# Spec 7000 — U 系 U1 对账门落位（effort #7000，U1）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7201–U7202，impl 2252）。
> 对账门落位轮（U 会话 1/50：预防式台账先于内容）。

## Scope

- `USession7000LedgerAuditTest` 落位（TSession6000LedgerAuditTest 同款
  公式族第八应用）：spec N → shape 票 7201+2(N−7000) / verify=+1 /
  impl 2252+(N−7000)，范围自扩展（扫现有 spec 驱动）；
- U 会话地图 effort-7000.md 开图（50 轮号段/波次/借鉴静脉
  预告）；README 纵深行 U1 登记。

## Out of Scope

- 不新增任何公共类型（快照门零变动）。

## Testing Decisions

- 台账门四测：票对齐/impl 零缺位/README 覆盖/号严格递增
  从 7000 起。
