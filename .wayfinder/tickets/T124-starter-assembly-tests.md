---
Type: task
Status: closed
---
## Question

starter/装配测试扩展：effort #6 九能力的 Spring 装配面（auto-config 触发/属性绑定/条件降级）是否有 ApplicationContextRunner 断言？

## Resolution

AFK 自决：补。starter 或 core 既有装配测试风格增：①webhook url 配置→forwarder bean + stateStore 注入；②SessionIndexStore bean 存在→runtime 装配含索引接线（bean 不存在→无索引仍可 spawn）；③tools result-limit 属性→Holder 生效；④jdbc/redis type→index bean 类型。产 impl-99（并入 spec 21 质量面）。
