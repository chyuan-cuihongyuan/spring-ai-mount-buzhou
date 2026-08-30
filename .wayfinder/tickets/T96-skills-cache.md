---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

skills 清单缓存与预热及深化小项？现状：effort#4 已做资源上限/缓存/正文上限+frontmatter 多行；后续项剩「清单缓存」。核实后若已覆盖则转为其他深化项：DB SkillStore 启动预热（首次清单读取延迟消除）、清单变更事件、tools read_range 与 spill 的只读句柄复用等小缺口核实与补齐。产出 spec 22 增量 + impl 71。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **核实结论**：清单缓存**确实缺失**——`DefaultSkillRegistry.resolve` 每次都查 DB（`listFor` 每轮 N 技能 = N 次 DB 往返，注入视图每轮都走）。这是真实热路径缺口，直接补。
2. **实现**：resolve 的 **DB 路径 TTL 缓存**（DB 覆盖结果 + 未命中负缓存；classpath 命中本就是 map 查找不缓存）；TTL 默认 30s、`buzhou.skills.catalog-cache-ttl` 可配（ISO-8601/秒数，0=关闭直查）。
3. **失效通道**：`SkillAdminApi` 全部变更路径（update/publish/disable/delete/transition）后自动失效（写立即可见不等 TTL）；`SkillRegistry.invalidateCatalogCache()` default no-op（接口二进制兼容）。**契约文档化**：直改 SkillStore 绕过 admin = 需手动失效（测试同步注记）。
4. **不做**（核实后无真实缺口）：DB 启动预热（TTL 缓存已消除稳态开销，首查延迟一条 SQL 量级无感）；清单变更事件（admin 失效已是同步语义，事件属 webhook 通道可组合）；read_range 句柄复用（spill 已独立完备，复用无净收益）。
5. 语义保持：DB 覆盖优先于 classpath 不变（缓存命中含负缓存也按此定论）。
