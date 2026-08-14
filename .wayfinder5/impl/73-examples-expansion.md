# 73 — examples 新能力 e2e 扩充（T98 决策落地）

**What to build:** NewCapabilitiesDemoTest（7 用例）+ examples resilience test 依赖。

**Blocked by:** 57、58、62、63、64（全部已 done）。

**Status:** done

## Done

验证：`./mvnw -pl examples clean test` 69/69 绿（新增 7 用例全绿；既有 62 无回归）。
落地：`demo/NewCapabilitiesDemoTest`——降级链（主 NETWORK 耗尽→备模型接管）、token 预算闸（累计触顶拦截+模型零调用）、会话 fork（分支继承历史+预算重置+源不动）、webhook（HttpServer 收件+HMAC 签名断言+session.forked 事件投递）、结构化输出（REASK 恢复+两败异常）、日配额（turns 超限拦截）。examples pom 增 buzhou-resilience test 依赖。
过程注记：排障中发现自身 pom 编辑曾把 groupId 写成下划线变体致 BOM 解析报错——已修正（教训：pom 编辑后立即 -pl 验证）。