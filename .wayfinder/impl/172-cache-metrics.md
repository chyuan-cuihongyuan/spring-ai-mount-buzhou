# 172 — 计数可观测与配置装配

**Parent:** spec 53 §E / [T207](../tickets/T207-cache-metrics.md)

**Status:** done

- [x] ResilienceProperties 第 14 组件 ResponseCache（**单构造器 record**——@ConstructorBinding
  盲区预防；13 参兼容构造保留；非法值 fail-fast）
- [x] 装配：enabled=true 时进程级共享 store + 每会话 ResponseCacheAdvisor 进链（+450）；
  默认关 = null 零注入零开销（auto-config 测试钉住默认关）
- [x] hit/miss/evicted 纯计数器 API（宿主可读；Micrometer 注册进 observability 域不做重复面）
- [x] metadata 3 键入档（enabled/max-entries/ttl）+ 绑定验证（覆盖/默认/fail-fast）
- [x] resilience 全套 113 测试绿（+9）
