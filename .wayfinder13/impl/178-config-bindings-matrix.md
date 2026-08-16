# 178 — 配置绑定完整性矩阵

**Parent:** [T214](../tickets/T214-config-bindings-matrix.md)

**Status:** done

- [x] ConfigBindingsMatrixTest（starter）：全模块 metadata 键（93）→ 真实装配路径绑定断言；
  强断言（@ConfigurationProperties record 反射 accessor 树）+ 中断言（guard/memory/tools/
  leak/mcp 子集/jdbc/redis/skills/spill.enabled 等 env 直读键）；键宇宙覆盖自校准
  （新键无归属即失败；SKIPPED/ENV 登记腐化即失败）
- [x] **当场抓获并修复 4 个存量缺陷**：
  1. `buzhou.leak.lease-age-threshold`——metadata 默认 "5m" vs 代码 Duration.parse 只认
     "PT5M"（按文档配置启动炸）→ 改 DurationStyle 双格式；
  2. `buzhou.skills.catalog-cache-ttl`——同类裸 parse 炸 → 双格式；
  3. `buzhou.runaway.per-turn.max-wall-clock`/`runaway.session.*`——metadata 键名与组件
     （wallClock/perSession）不符，配置被 ignoreUnknownFields **静默吞掉** → 键名对齐组件
     （破坏性改名入档，原键本就无效）；
  4. `buzhou.index.closed-retention`——同上（组件路径 core.indexClosedRetention）→ 键名
     修正 `buzhou.core.index-closed-retention`
- [x] BuzhouStarterHardeningTest 兼容 -am 联编 classes 目录形态（jar 校验保留）
- [x] 矩阵 2 + hardening 4 + integration 1 全绿
