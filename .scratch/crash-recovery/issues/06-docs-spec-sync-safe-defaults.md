# 06 — 机制文档 + docs/spec 同步 + safe-by-default 收口

**What to build:** 统一 `buzhou.recovery.*` 配置属性（各支柱票已带各自属性，本票做 **safe-by-default 审计 + AutoConfig 装配收口**：默认 async 档、VOID 恢复、去重开、一键关自动重驱动）；同步 docs/spec 持久化与会话机制文档（「改机制先改 Spec」）；写明 **M2 预留位**（绑定级 policy 消费、与 06 优雅停机 EXIT-flush 联动、与 09 工具结果缓存分工、崩溃循环兜底交接 03/04 熔断）。

**Blocked by:** 02, 05（两条并行支柱链收口后）

**Status:** done

- [ ] `buzhou.recovery.*` 配置属性 record（`enabled` / `resume-strategy` / `durability-tier` / `crashloop-hard-cap` / 幂等默认开关），boxed 类型、null=未配置，对齐 `SpillProperties` 模板
- [ ] safe-by-default 审计：默认 async 档、VOID 恢复、去重开；提供一键关自动重驱动的开关
- [ ] AutoConfig 装配收口（core AutoConfig），safe-by-default 项默认开
- [ ] docs/spec 持久化与会话机制文档同步（持久化强度分档、恢复语义分档、幂等三件套、租约心跳）
- [ ] M2 预留位说明：绑定级 policy 消费、`EXIT`-flush 与 06 优雅停机 drain 联动、去重记录与 09 工具结果缓存分工、自动重驱动崩溃循环兜底交接 03/04 熔断
- [ ] 公共 API 变更（`@BuzhouTool` 幂等声明扩展、键提取器、`SpawnOptions` 恢复策略）兼容性影响在 PR 描述说明（全 additive / default）
