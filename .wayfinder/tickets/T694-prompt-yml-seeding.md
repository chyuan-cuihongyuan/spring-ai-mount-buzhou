---
Type: task
Status: closed
---
## Question

`buzhou.prompt.templates[{name,body,label}]` yml 播种（同 body 幂等）+
bean 恒在（空注册表零行为变化）+ `@EnableConfigurationProperties` 登记。

## Resolution

done（2026-09-08）：impl-374；yml 播种/幂等/label 指针/空配置 context
runner 用例绿，buzhou-core 全模块绿。
