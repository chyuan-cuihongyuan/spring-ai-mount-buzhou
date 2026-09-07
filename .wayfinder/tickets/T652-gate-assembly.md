---
Type: task
Status: closed
---
## Question

引擎 notify 路径接线（未配置门零行为变化）+ yml 装配面
（`buzhou.alert.silences[]` / `buzhou.alert.inhibit-rules[]`，与 312
共前缀）+ README 纵深 IV 加行 + API 快照随轮 regenerate。

## Resolution

done（2026-09-04）：impl-353；引擎构造器增设可选 gate 重载（旧构造器
委托——二进制兼容），notify 先 observe 再判定；属性类扩 silences/
inhibitRules 两键（声明才建门，机制名启动校验复用 validateMechanisms
口径）；装配三用例 + 引擎接线两用例绿；快照 regenerate +5 型；
README/spec↔README 覆盖门绿。
