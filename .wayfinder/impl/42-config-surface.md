# 42 — 横切 · 配置全参数化 + 启动校验 + FailureAnalyzer + 默认值安全化

**What to build:** 调优不改代码、拼错启动即失败并给人类可读指引：全部硬编码项入 properties（并发/超时/TTL/事件分发/内存上限/保留族/spill 配额/sandbox/policy 刷新）；jakarta.validation 启动校验（store.type 封闭枚举 fail-fast）；FailureAnalyzer 翻译启动失败；不安全默认值修正（带迁移注记）。

**Blocked by:** 41（参数面收敛后再全量固化）

**Status:** ready-for-agent

- [ ] 参数清单盘点入各机制 properties（core: maxConcurrencyPerTurn/toolTimeout/leaseTtl+renew/loopTimeout/eventDispatch/in-memory；memory/spill/guard 各自 knob）
- [ ] jakarta.validation 依赖 + @Validated + @Min/@Max/@NotNull；store.type 枚举 fail-fast
- [ ] FailureAnalyzer（store 装配失败、配置约束冲突 → description + action）
- [ ] 默认值修正（迁移注记）：spill root-dir 独立临时目录、redis snapshot-ttl PT168H、hot-tail maxInlineChars 65536、jdbc dialect 缺省自动探测（DatabaseMetaData）
- [ ] 编程式 API 校验（负数 Duration/轮数）
- [ ] 测试：拼错 store.type 启动失败带指引；越界值被拒；默认值变更不破坏既有契约用例
