# 1002 — Hook 链解析顺序快照读面

> 来源：J 会话第 3 轮 = effort #1002（[T1455](../../.wayfinder/tickets/T1455-hook-composition-shape.md) / [T1456](../../.wayfinder/tickets/T1456-hook-composition-verify.md) / impl 755）。借鉴：Kong Gateway [plugin priority](https://docs.konghq.com/gateway/latest/plugin-development/custom-plugin-guide/)（显式优先级 + 稳定决胜序的配置可见性）。

## Problem Statement

HookChain 构造期按 `order` 升序、同序按名稳定排序并过滤 disabled 名单——语义正确但**不可见**：①`disabled` 配置里拼错的 hook 名被静默丢弃（幽灵禁用零信号，关不掉想关的护栏而不自知）；②实际派发序与各 hook 的 order 值要翻源码才能确认。spec 646/647 给了耗时面，没给「序与名单」面。

## 目标

- 新公共 record `ChainComposition`（core.hook，api 面）：`resolvedHookNames`（解析后派发序，不可变）+ `ghostDisabledNames`（disabled 配置中未命中任何 hook 的名字集，不可变）。
- `HookChain.composition()`：只读快照（构造期一次计算，字段 final；排序/过滤语义逐位不变）。

## 兼容性

纯增量读面：排序、过滤、派发行为零变化；无新配置项。

## Out of Scope

- 幽灵禁用升级为启动失败（语义变化——保守起见只显形不拦截，宿主可据快照自警）。
- order 值进快照（记录对象是名字序；order 值属 hook 自身元数据，避免快照与实现漂移）。
