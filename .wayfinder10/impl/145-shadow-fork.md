# 145 — shadow fork 探测

**Parent:** spec 49 §A / [T176](../tickets/T176-shadow-fork.md)

**Status:** done

- [x] ResilienceProperties.Shadow 配置组（enabled 默认关/models/max-concurrent 默认 2/daily-budget 默认 1000）
- [x] ShadowTrafficController：并发信号量 + UTC 日预算池护栏；提交即返回（用户路径零增延迟）；裸 ChatModel 调用（工具副作用红线）；shadow.compared 事件 + shadow.calls{outcome} 计数
- [x] Advisor 主路径成功后提交（金丝雀/流式不探测——诚实边界）；Spring 装配按名解析 shadow 模型（fail-fast）
- [x] 测试：对照事件/预算拦/并发拦/失败吞噬/默认关零变化（5 测试）
