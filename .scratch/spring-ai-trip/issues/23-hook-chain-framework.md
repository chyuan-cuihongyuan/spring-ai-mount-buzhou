# Hook 链框架设计

Type: grilling
Status: open
Blocked by: —

## Question

Harness 的 Hook（Callback 切面）基础设施怎么设计：暴露哪些切面（参照 DECO 的 beforeTool/afterTool/beforeModel/afterModel/beforeAgent/afterAgent/onRunEvent——映射到 Spring AI 2.x 的 Advisor 链 + ToolCallingManager 包装，各自对应什么扩展点）？Hook 的注册模型（SPI 自动发现 vs 显式配置）、同切面多 Hook 的编排顺序（order 约定）、Hook 的短路语义（返非空结果阻断后续，如 Guard 的 Maybe.just()）？Hook 与 Harness 内部机制的关系——Spill、微压缩、可观测采集是否都实现为内置 Hook（吃自己的狗粮）？业务自定义 Hook 的 API 表面长什么样？
