---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

buzhou-observe-dashboard 的安全与健壮性收口：默认 bind 127.0.0.1 + 可配 bind-address、鉴权形态（静态 bearer token vs 可插拔 filter）、写端点（Skill CRUD）是否额外门、500 响应不回显内部异常、请求体大小上限、分页 size clamp、executor 关闭、服务端日志、XSS 单引号转义、pathPrefix 校验、测试修掉跨模块 import core.internal 违例。安全默认值如何取舍（默认关？默认 localhost+token?）？

## Resolution

全部进本轮（采纳 T69 §1，对齐 Actuator 安全模型）：
1. **默认绑 127.0.0.1**；`bind-address` 可配。**绑定非 loopback 且未设 auth-token → 启动失败**（BuzhouConfigurationException：暴露面与鉴权强制配对，fail-fast）；绑定非 loopback 且已设 token → 启动 WARN 提示生产建议走 OTel。
2. **鉴权**：静态 bearer token（`auth-token` 配置，支持 ${ENV:} 占位）。设置后全部 API 要求 `Authorization: Bearer <token>`（401 JSON otherwise）；静态资源页也要求（浏览器场景文档给 curl/反代指引）。不做可插拔 Authenticator SPI（YAGNI，注记开放问题）。
3. 500 响应仅 `{"error":"internal_error"}`，异常细节进服务端 ERROR 日志。
4. 请求体上限 1MB（413）；分页 size clamp 到 [1,200]；cursor 长度上限。
5. executor 补 shutdown（stop() 时 close）；虚拟线程 executor 持有句柄。
6. esc() 补单引号转义（&#39;）；pathPrefix 校验（禁止 /api 前缀冲突、非法字符）。
7. 服务端日志基线：启动 INFO（端口/绑定/token 有无）、路由错误 WARN、500 ERROR。
8. 测试修掉 import core.internal（InMemoryObservabilityStore 改经 BuzhouStores 工厂或 test-jar public 入口）；补鉴权/绑定失败/413/clamp/转义用例。（可推翻）
