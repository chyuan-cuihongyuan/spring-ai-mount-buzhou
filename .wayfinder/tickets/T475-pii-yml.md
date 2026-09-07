---
Type: task
Status: closed
---
## Question

PII 自定义规则 yml 声明式配置 + 输入侧叠加。

## Resolution

done（2026-08-30）：impl-277；PiiInputRedactionHook 补 (types, customRules) 构造器；
GuardModule.fromYml 解析 pii.custom-rules（List<{name,pattern}> / map 形态）→
CustomPiiRules，输出/输入两侧 hook 全四象限接线；非法规则装配期 fail-fast。
