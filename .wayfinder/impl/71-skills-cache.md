# 71 — skills 清单 TTL 缓存（T96 决策落地）

**What to build:** DefaultSkillRegistry DB 路径 TTL 缓存 + admin 变更自动失效 + catalog-cache-ttl 配置。

**Blocked by:** None.

**Status:** done

## Done

验证：`./mvnw -pl buzhou-skills clean test` 62/62 绿（deleteDbRestoresClasspath 按缓存契约更新：直改 store 手动失效）。
落地：resolve DB 路径 TTL 缓存（正/负缓存，DB 覆盖优先语义保持，classpath 直返）；`buzhou.skills.catalog-cache-ttl`（默认 30s，0=关闭）Builder+fromYml；SkillAdminApi 变更后自动失效（4 参构造 + 全变更路径）；SkillRegistry.invalidateCatalogCache default no-op。核实后不做：DB 预热/变更事件/句柄复用（无真实缺口，决议记录）。