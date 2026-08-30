---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

buzhou-observe-otel 的生产级收口范围：openSpans/sessionTrace 无界增长的治理（上限+驱逐 vs 接入 core LeakDetector）、三处静默 catch 的日志化、exporter-mode=tracer 无 Tracer bean 时的 fail-fast（替代静默回退）、exporterMode 枚举校验、OTLP header/timeout 透出、javadoc 前缀漂移修复、pom 冗余 test 依赖清理。

## Resolution

全部进本轮（采纳 T69 §2）：
1. openSpans 有界化：上限默认 10_000，超限驱逐最旧未终态 span（记 UNSET end + buzhou.evicted 属性）+ 驱逐计数暴露；sessionTrace 改为会话结束清理（onSessionEnd 钩子已有路径）+ 上限同治。
2. 三处 `catch (RuntimeException ignored)` 日志化（WARN 限频，含 exporter 类型）。
3. exporter-mode=tracer 且无 Tracer bean：从静默回退改为启动失败（BuzhouConfigurationException + FailureAnalyzer 文案），显式意图不容忽略；exporterMode 加枚举校验（非法值启动失败）。
4. OtlpBridge.otlp 补 headers/timeout 可配（buzhou.observe.otel.headers.* 平铺键，值支持 env 占位 ${ENV:}）。
5. javadoc 前缀漂移修复（buzhou.observe.otel）；pom 冗余 junit/assertj test 声明删除（根 pom 已注入）。
6. 补测试：驱逐路径、tracer 模式缺 bean 启动失败、枚举校验、headers 透传。（可推翻）
