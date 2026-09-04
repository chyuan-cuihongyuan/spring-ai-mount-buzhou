---
Type: task
Status: closed
---
## Question

`RoutingWeightsHotReload`（refresh 事件重读 weights 逐路 setWeight、
面外名字 WARN 跳过、计数）+ 装配（路由器在场即挂）+ 收口。

## Resolution

done（2026-09-04）：impl-363；热重载三用例（MutableEnvironment 改值
生效/面外跳过/计数）+ 装配两用例（在场挂/未配无）绿；快照 regenerate
+1 型；README 纵深 IV 加行、覆盖门绿。
