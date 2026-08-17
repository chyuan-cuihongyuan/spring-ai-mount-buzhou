---
Type: task
Status: closed
---
## Question

配置元数据第三批：effort#9 新键（加密/单飞/读降级/排空预算等）入档 additional-spring-configuration-metadata + 绑定验证。

## Resolution

AFK 自决：五键入档——core（store.read-degrade / webhook.close-drain-timeout）、spill（encryption-key）、
guard（audit.signing.key-dir）、tools 新建元数据文件（run-command.max-output-bytes；effort #7 skills
先例——map 解析键同样入档 IDE 提示）。绑定验证：read-degrade=empty 经 kebab 绑定 + Holder 下发 +
非法值启动失败（BuzhouCoreAutoConfigurationTest 新用例）；其余键构造期 fail-fast 已有对应单测。
产 impl-138。
