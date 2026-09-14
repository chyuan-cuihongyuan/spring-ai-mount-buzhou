# 1422 — Hook 顺序碰撞审计

> 来源：L 会话第 23 轮 = effort #1422（票 T2145 / T2146 / impl 1075）。借鉴：Spring ordered-bean 审计（同 order 的 bean 派发序靠名字兜底——重命名即变序的装配脆性显形）。

## Problem Statement

`ChainComposition` 同 order 钩子按名字字典序兜底排序——分发确定，但保序靠「名字恰好」是装配巧合：重命名一个 Hook 即改变 beforeTool/afterModel 等裁决先后。碰撞组无审计面（宿主看不见自己的脆性）。

## 目标

- `HookOrderAudit`（core/hook，纯函数静态面，private 构造）：
  - `analyze(List<BuzhouHook>)` → `record Report(hookCount, collisions, collisionCount)`；
  - `OrderGroup(order, hookNames)`：仅列 ≥2 钩子同序的碰撞组（唯一 order 不占报告），组内名字典序（现兜底序——脆性所在），组间 order 升序；
  - 空清单零报告哨兵；顺序无关容忍。
- 纯函数零状态：不触 HookChain 运行期，只读不裁决（修复 = 宿主分配显式错开的 order）。

## 兼容性

纯函数；无配置；不改链组装语义。

## Out of Scope

- 运行期 order 冲突阻断（装配脆性提示面，不做护栏裁决）。
- name 缺失钩子的合成命名规则（name() 为空串的钩子按空串参与典序——既有语义）。
- 阶段级（beforeTool vs afterModel）区分审计——order 是全链统一序（既有语义）。
