# 02 — 配置体系四层覆盖

**What to build:** PolicyConfigProvider SPI + properties 内置实现；四层覆盖（默认<yml<绑定级<工具级）合并语义（标量覆盖/映射深合并/列表替换）与通配消歧（精确>最长前缀>*）生效；绑定级配置可持久化；安全项默认全开、依赖项优雅降级。

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] 四层覆盖合并与通配消歧有行为测试
- [ ] 工具策略=工具声明默认+配置通配覆盖可用
- [ ] 绑定级配置写入持久层并在重启后生效
- [ ] 非法配置优雅降级并记告警，不炸启动
