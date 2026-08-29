# Wayfinder Map — Buzhou 维护模式门（effort #221，B 会话第 44 轮）

> B 会话第 44 轮。单会话有排水（155）、runtime 有停机（drain）——但「计划内
> 维护窗口：全部新 Turn 温和拒绝」缺独立开关。借鉴 K8s maintenance/
> cordon（标记维护，新工作不调度，存量自然结束）。

## Destination

MaintenanceGate（core/session）：全局 volatile 开关 + beforeTurn 拦截
（维护中 block 可读理由「计划维护中，预计 X 后恢复」+ 计数）；与排水协调器
组合 = 完整维护序列（gate 拒新 → drain 排存量 → 维护）。挂 hook 即生效。

## Notes

- 号段：B=奇数 spec（本轮 205）；轮次 .wayfinder200+。
- order 10（比循环闸 20 更早——维护面最高优先级）。
- 开关纯内存（进程级）——多实例同步留档。

## Decisions so far

- 拒绝不抛异常（block 温和文案——用户看得到恢复时间）。

## Not yet specified

- 维护窗口自动调度；多实例共享开关。

## Out of scope

- 沿用各轮；存量会话强杀（归 drain+硬关既有语义）。

## Tickets

- [x] [T577 MaintenanceGate（全局维护开关+温和拦截）](tickets/T577-maintenance.md)（impl-316）
- [x] [T578 维护门回归（开拦/关放/文案/组合 drain）](tickets/T578-maintenance-tests.md)（impl-316）
