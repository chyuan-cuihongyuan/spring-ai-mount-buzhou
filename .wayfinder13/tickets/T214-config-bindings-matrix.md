---
Type: task
Status: closed
---
## Question

全模块配置绑定完整性测试（starter 侧）：聚合各模块 additional-spring-configuration-metadata.json
的 properties 键 → 逐键 ApplicationContextRunner 绑定断言（真实装配路径，非仅 record 构造）；
T187 类「yml 键静默不生效」缺陷变成系统性防线。

## Resolution

impl-178 落地：93 键全矩阵（record 反射树强断言 + env 直读中断言 + 键宇宙自校准）。
**防线当场兑现**：抓获并修复 4 个存量缺陷——leage-age-threshold/catalog-cache-ttl 双格式
parse 炸（按文档配置启动失败）、runaway.max-wall-clock/session.* 与 index.closed-retention
键名与组件不符静默失效（键名修正对齐，破坏性改名入档）。矩阵 2 + hardening 4 + integration 1
全绿。T214 关闭。
