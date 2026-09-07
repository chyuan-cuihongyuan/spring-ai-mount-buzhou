---
Type: task
Status: closed
---
## Question

schema 属性组（required-keys Map + fail-open）+ checker NullBean 装配 +
全局挂点去重（effectiveGlobalListeners）。

## Resolution

done（2026-09-01）：impl-330；BuzhouWebhookProperties.Schema + autoconfig
checker bean（空声明 null）+ EventSchemaChecker.delegate() 暴露 + 挂点去重。
