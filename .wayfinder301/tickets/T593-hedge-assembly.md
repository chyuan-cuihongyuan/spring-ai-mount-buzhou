---
Type: task
Status: closed
---
## Question

hedge yml 属性组 + @Primary buzhouHedgedChatModel 装配（137 原语 → 装配面）。

## Resolution

done（2026-09-01）：impl-324；`ResilienceProperties.Hedge`（enabled/primary-model/
model/delay 默认 200ms，开启时空名/同名/非正延迟 fail-fast）+ 独立虚拟线程
执行器 bean + `@Primary` 装配 bean。`HedgeAssemblyTest` 四象限绿。
