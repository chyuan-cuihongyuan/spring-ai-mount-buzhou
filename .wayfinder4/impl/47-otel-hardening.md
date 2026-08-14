# 47 — otel 桥有界化与 fail-fast

**What to build:** 长跑进程 otel 桥内存稳定（openSpans 上限驱逐+计数、sessionTrace 会话清理）；桥接层异常可见（WARN 限频）；exporter-mode 配错启动失败而非静默回退；OTLP headers/timeout 可配。

**Blocked by:** 46-observability-hardening

**Status:** ready-for-agent

- [ ] openSpans 上限（默认 10_000）最旧驱逐（end=UNSET+buzhou.evicted）+ 驱逐计数；sessionTrace onSessionEnd 清理
- [ ] 三处静默 catch WARN 限频日志化
- [ ] tracer 模式缺 Tracer bean → BuzhouConfigurationException；exporterMode 枚举校验
- [ ] OTLP headers（${ENV:} 占位）/timeout 可配 + 测试
- [ ] javadoc 前缀修正、pom 冗余 test 依赖删除
- [ ] 测试：驱逐路径、缺 bean 启动失败（ApplicationContextRunner）、枚举非法值、headers 透传
