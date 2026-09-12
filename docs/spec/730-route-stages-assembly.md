# 730 — 路由阶段标签 yml 装配

> 来源：G 会话第 31 轮 = effort #730（D 会话装配轮模式）/ [T1011](../../.wayfinder/tickets/T1011-route-stages-assembly-shape.md) / [T1012](../../.wayfinder/tickets/T1012-route-stages-assembly-verify.md) / impl 533。

## 背景

RouteStages（spec 723）只有编程面——yml 声明式入口缺失。

## 目标

- `buzhouWeightedChatModel` bean 增 Environment 参数：`buzhou.routing.stages.<name>=STAGE`（map 绑定）+ `buzhou.routing.visible-stages`（set 绑定）双声明才生效——filter 后 <2 路抛 BuzhouConfigurationException（fail-fast 与路由面 ≥2 口径一致）。
- 缺省（两键均无）逐字节不变。

## 测试

装配直调（stages+visible → 剔除生效；缺省 → 原权重）；<2 路 fail-fast；既有用例零回归。

## 兼容性

缺省零变化。
