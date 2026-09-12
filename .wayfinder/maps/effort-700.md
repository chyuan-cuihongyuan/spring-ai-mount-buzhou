# Wayfinder Map — G 会话 700 系：借鉴高价值开源项目的 50 轮自迭代（effort #700 总图）

> **G 会话**（2026-09-13 启动）：继 C（300 系）/ D（400 系）/ E（500 系）/ F（600 系）之后的第五条自迭代线。
> 用户常设授权（沿 F 会话 2026-09-12）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。
> **编号裁决**：G 会话占用 spec **700–749**、票 **T951–T1050**（每轮 2 张：shape + verify）、impl **503–552**（每轮 1 片）；与已收口的 F 会话（600 系 / T851–T950 / impl453–502）保持 50 轮全距互不侵犯。
> 起点：origin/main @ 61b48668（F 会话收口后，全仓 verify 绿）。

## Destination

**50 个完整自迭代 loop 全部完成**：每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或 opt-in 的机制改进；50 轮全部 Conventional Commits 提交并推送 GitHub；收口轮全仓 `mvn -B -ntp clean verify` 终验绿（16 模块 + JaCoCo ≥70% + enforcer + 快照门 + SpecCoverage）。

## Notes

- 每轮固定四步产物：决策票（同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<7NN>-<slug>.md`（+README 生产级纵深表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级测试必须绿；周期性 + 收口轮跑全仓 `mvn -B -ntp clean verify`；每轮 commit 后 push origin main。
- 选题纪律：每轮开工先缺口核查（grep spec+代码），已实现则台账记 `ruled-out` 顺延；E/F 会话已落地的主题同样回避。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J 占位符、record/sealed 优先；新公共类入 `api` 包需 Javadoc + API 快照随轮再生。

## Decisions so far

- [工具执行 per-tool 耗时聚合读面](../tickets/T951-tool-timing-aggregate-shape.md) — ToolTimingAggregator（Holder 模式）+ tool-timing 健康段按总耗时降序 top-20（pg_stat_statements 本义）；per-tool 不进 micrometer（基数守卫）。
- [缓存 stale-while-revalidate](../tickets/T953-cache-swr-shape.md) — 落点 TtlCachingToolCallback（刷新=重放工具调用，advisor 重放请求不值）；swrGrace opt-in、后台单飞、失败保旧值。
- [模型端点慢启动权重爬坡](../tickets/T955-routing-slow-start-shape.md) — RoutingSlowStart 分 4 步线性爬坡（tick 包内可见测试手动推进）；热重载上调走 ramp、降权瞬时。
- [MCP keepalive 空闲探活](../tickets/T957-mcp-keepalive-shape.md) — 注册表周期 listToolNames 探活（漂移基线同源）；失败走 spec-changed 同口径重建，refreshLock 内防竞态。
- [最小可用水位闸（归档 PDB）](../tickets/T959-session-pdb-shape.md) — SessionAvailabilityFloor 挂 archive()；未知计数 fail-open（保底闸失明不误伤）；restore/purge 不受闸。
- [store SPI 契约校验套件](../tickets/T961-store-contract-shape.md) — SessionStateStoreContract.verify 九项语义检查（主源码零 JUnit；逐项收集不抛）；`__contract__` 会话自清理。

## 50 轮台账

| # | 主题 | 借鉴源 | 票 | impl | spec | 状态 |
|---|------|--------|----|------|------|------|
| 1 | 工具执行 per-tool 耗时聚合读面 | pg_stat_statements / ClickHouse query log | T951–T952 | 503 | 700 | ✅ |
| 2 | 缓存 SWR（原列「响应缓存容量上限」ruled-out——spec 53 §D 已覆盖） | nginx proxy_cache_use_stale / guava refreshAfterWrite | T953–T954 | 504 | 701 | ✅ |
| 3 | 模型端点慢启动权重爬坡 | nginx upstream slow_start | T955–T956 | 505 | 702 | ✅ |
| 4 | MCP keepalive 空闲探活（原列「Retry-After」ruled-out——spec 10 已覆盖） | grpc keepalive pings | T957–T958 | 506 | 703 | ✅ |
| 5 | 最小可用水位闸·归档 PDB（原列「租约泄漏检测」ruled-out——impl-41 已覆盖；「排水水位闸」并入本主题：archive 即自愿驱逐入口） | k8s PodDisruptionBudget | T959–T960 | 507 | 704 | ✅ |
| 6 | store SPI 契约校验套件（原列「压力归档建议」与 spec 179 空闲清单重叠顺延） | Pact consumer contract testing | T961–T962 | 508 | 705 | ✅ |
| 7 | API 快照 diff 破坏性分级 | oasdiff / OpenAPI diff | T963–T964 | 509 | 706 | ✅ |
| 8 | 模块边界守卫 + internal 存量清零（8 处违规、6 类迁出、快照再生 11 行） | ArchUnit / eslint no-restricted-imports | T965–T966 | 510 | 707 | ✅ |
| 9 | HookTiming 滚动 max 读面 | micrometer Timer max decay | T967–T968 | 511 | 708 | ✅ |
| 10 | 角色权限拒绝有界日志 | Redis ACL LOG | T969–T970 | 512 | 709 | ✅ |
| 11 | 会话导出 unchanged 协商 | HTTP ETag / RFC 7232 | T971–T972 | 513 | 710 | ✅ |
| 12 | fork 谱系游走环防护（原列「命中率 getter」ruled-out——spec 90 已覆盖） | call-graph 环检测 | T973–T974 | 514 | 711 | ✅ |
| 13 | JSONL 轮转旧档 gzip 压缩 | logrotate compress+delaycompress | T975–T976 | 515 | 712 | ✅ |
| 14 | PII 格式保持假名化（原列「Kappa」ruled-out——spec 541 已覆盖） | presidio surrogate | T977–T978 | 516 | 713 | ✅ |
| 15 | 秘密扫描熵阈值过滤 | trufflesecurity/trufflehog | T979–T980 | 517 | 714 | ✅ |
| 16 | 指标命名规范守卫测试（+ RoutingScheduleAdjuster 动态拼接名清零） | prometheus/client_java naming | T981–T982 | 518 | 715 | ✅ |
| 17 | 告警规则 dry-run | k8s admission dryRun / argo --dry-run | T983–T984 | 519 | 716 | ✅ |
| 18 | 工具调用图谱环检测 | call-graph cycle detection | T985–T986 | 520 | 717 | ✅ |
| 19 | webhook 投递限速 | envoy local rate limit | T987–T988 | 521 | 718 | ✅ |
| 20 | 生效配置 diff 读面 | kubectl diff | T989–T990 | 522 | 719 | ✅ |
| 21 | 错误签名静默标记 | getsentry/sentry muted issues | T991–T992 | 523 | 720 | ✅ |
| 22 | 死信重放审计事件 | 审计完整性惯例 | T993–T994 | 524 | 721 | ✅ |
| 23 | 工具泳道排队时延观测 | grpc server queue 时延 | T995–T996 | 525 | 722 | ✅ |
| 24 | 路由金丝雀阶段标签 | MLflow stages / argo-rollouts | T997–T998 | 526 | 723 | ✅ |
| 25 | MCP keepalive yml 装配 | D 会话装配轮模式 | T999–T1000 | 527 | 724 | ✅ |
| 26 | 路由慢启动 yml 装配 | D 会话装配轮模式 | T1001–T1002 | 528 | 725 | ✅ |
| 27 | 归档 PDB yml 装配（capped probe 计数） | D 会话装配轮模式 | T1003–T1004 | 529 | 726 | ✅ |
| 28 | 秘密熵过滤 Builder 装配 | D 会话装配轮模式 | T1005–T1006 | 530 | 727 | ✅ |
| 29 | webhook 限速 yml 装配 | D 会话装配轮模式 | T1007–T1008 | 531 | 728 | ✅ |
| 30 | 健康时间线 JSONL 压缩线装配 | D 会话装配轮模式 | T1009–T1010 | 532 | 729 | ✅ |
| 31 | 路由阶段标签 yml 装配 | D 会话装配轮模式 | T1011–T1012 | 533 | 730 | ✅ |
| 32 | PII 假名化模式装配 | D 会话装配轮模式 | T1013–T1014 | 534 | 731 | ✅ |
| 33 | 契约套件接入示例（H2 实存储） | spec 705 复用面 | T1015–T1016 | 535 | 732 | ✅ |
| 34 | 导出协商联动补验 | spec 710 补验 | T1017–T1018 | 536 | 733 | ✅ |
| 35 | PDB×空闲压缩联动补验 | spec 704/179 补验 | T1019–T1020 | 537 | 734 | ✅ |
| 36 | 金丝雀×慢启动×热重载联动补验 | spec 723/702/340 补验 | T1021–T1022 | 538 | 735 | ✅ |
| 37 | ConfigDiff×快照端点同源补验 | spec 719 补验 | T1023–T1024 | 539 | 736 | ✅ |
| 38 | 工具侧滚动 max 同构扩散 | spec 708 同构扩散 | T1025–T1026 | 540 | 738 | ✅ |
| 39 | 拒绝日志排序稳定性补验（并列 tie-break 实现缺陷补齐） | spec 709 补验 | T1027–T1028 | 541 | 737 | ✅ |
| 40 | 限速×死信路径隔离补验 | spec 718/24 补验 | T1029–T1030 | 542 | 739 | ✅ |
| 41 | 谱系游走导入场景深链补验 | spec 711 补验 | T1031–T1032 | 543 | 740 | ✅ |
| 42 | 静默标记×健康段联动补验 | spec 720/85 补验 | T1033–T1034 | 544 | 742 | ✅ |
| 43 | 假名化×幂等占位符互操作补验 | spec 713/731 补验 | T1035–T1036 | 545 | 741 | ✅ |
| 44 | MessageStore SPI 契约校验套件 | spec 705 同构扩散 | T1037–T1038 | 546 | 743 | ✅ |
| 45 | MessageStore 契约接入 H2（抓出探针会话主键冲突设计缺陷并重构） | spec 743 复用面 | T1039–T1040 | 547 | 744 | ✅ |
| 46 | dryRun×AlertGate 语义确认 | spec 716 补验 | T1041–T1042 | 548 | 746 | ✅ |
| 7 | 会话租约泄漏检测 | brettwooldridge/HikariCP leakDetectionThreshold | T963–T964 | 509 | 706 | ❌ |
| 8 | 排水最小可用水位闸 | k8s PodDisruptionBudget | T965–T966 | 510 | 707 | ❌ |
| 9 | 会话压力归档建议排序 | k8s Eviction API 排序思想 | T967–T968 | 511 | 708 | ❌ |
| 10 | store SPI 契约测试基类 | Pact consumer contract testing | T969–T970 | 512 | 709 | ❌ |
| 11 | API 快照 diff 破坏性分级 | oasdiff / OpenAPI diff | T971–T972 | 513 | 710 | ❌ |
| 12 | 模块边界守卫测试 | ArchUnit / eslint no-restricted-imports | T973–T974 | 514 | 711 | ❌ |
| 13 | HookTiming 滚动 max 读面 | micrometer max decay window | T975–T976 | 515 | 712 | ❌ |
| 14 | 角色权限拒绝统计 | Redis ACL log | T977–T978 | 516 | 713 | ❌ |
| 15 | 会话导出 unchanged 协商跳过 | HTTP ETag / If-None-Match | T979–T980 | 517 | 714 | ❌ |
| 16 | 模型端点金丝雀标签路由 | argo-rollouts canary / MLflow stages | T981–T982 | 518 | 715 | ❌ |
| 17 | 提示前缀缓存命中率 getter | spec 90 补全（缓存命中率便利面） | T983–T984 | 519 | 716 | ❌ |
| 18 | fork 谱系环检测 | call-graph cycle detection | T985–T986 | 520 | 717 | ❌ |
| 19 | JSONL 轮转旧档 gzip 压缩 | logrotate compress | T987–T988 | 521 | 718 | ❌ |
| 20 | 双 judge Kappa 一致系数 | spec 541 扩散（Cohen's kappa） | T989–T990 | 522 | 719 | ❌ |
| 21 | PII 格式保持假名化 | microsoft/presidio FPE 思想 | T991–T992 | 523 | 720 | ❌ |
| 22 | 秘密扫描熵阈值过滤 | trufflesecurity/trufflehog entropy | T993–T994 | 524 | 721 | ❌ |
| 23 | 告警规则 dry-run 模式 | k8s admission dry-run / argo sync --dry-run | T995–T996 | 525 | 722 | ❌ |
| 24 | 工具调用图谱环检测 | spec 519 扩散（静态分析环检测） | T997–T998 | 526 | 723 | ❌ |
| 25 | 指标命名规范守卫测试 | prometheus/client_java naming conventions | T999–T1000 | 527 | 724 | ❌ |
| 26 | 会话导出滚动分片大档 | spec 642 RollingJsonlWriter 复用 | T1001–T1002 | 528 | 725 | ❌ |
| 27 | 评估并行 per-run 上限 | spec 28 补全（并发隔离） | T1003–T1004 | 529 | 726 | ❌ |
| 28 | 死信重放审计事件 | 审计完整性惯例（动作入链） | T1005–T1006 | 530 | 727 | ❌ |
| 29 | 工具泳道排队时延观测 | 排队论队列时延惯例 / grpc server queue | T1007–T1008 | 531 | 728 | ❌ |
| 30 | webhook 订阅投递限速 | envoy local rate limit per upstream | T1009–T1010 | 532 | 729 | ❌ |
| 31 | 生效配置 diff 读面 | kubectl diff / git diff 思想 | T1011–T1012 | 533 | 730 | ❌ |
| 32 | 错误签名已知问题静默标记 | getsentry/sentry resolved/muted issues | T1013–T1014 | 534 | 731 | ❌ |
| 33 | per-tool 耗时聚合 yml 装配 | spec 700 扩散（D 会话装配轮模式） | T1015–T1016 | 535 | 732 | ❌ |
| 34 | 响应缓存容量上限 yml 装配 | spec 701 扩散 | T1017–T1018 | 536 | 733 | ❌ |
| 35 | 慢启动 yml 装配 | spec 703 扩散 | T1019–T1020 | 537 | 734 | ❌ |
| 36 | Retry-After yml 装配 | spec 704 扩散 | T1021–T1022 | 538 | 735 | ❌ |
| 37 | MCP keepalive yml 装配 | spec 705 扩散 | T1023–T1024 | 539 | 736 | ❌ |
| 38 | 租约泄漏 yml 装配 | spec 706 扩散 | T1025–T1026 | 540 | 737 | ❌ |
| 39 | 水位闸 yml 装配 | spec 707 扩散 | T1027–T1028 | 541 | 738 | ❌ |
| 40 | 金丝雀标签 yml 装配 | spec 715 扩散 | T1029–T1030 | 542 | 739 | ❌ |
| 41 | 轮转 gzip yml 装配 | spec 718 扩散 | T1031–T1032 | 543 | 740 | ❌ |
| 42 | 秘密熵过滤 yml 装配 | spec 721 扩散 | T1033–T1034 | 544 | 741 | ❌ |
| 43 | 契约测试基类 store-jdbc 接入示例 | spec 709 复用面 | T1035–T1036 | 545 | 742 | ❌ |
| 44 | 边界守卫全仓接线确认 | spec 711 CI 面 | T1037–T1038 | 546 | 743 | ❌ |
| 45 | unchanged 协商补验 | spec 714 补验 | T1039–T1040 | 547 | 744 | ❌ |
| 46 | 熵过滤误报基线补强 | spec 721 补验 | T1041–T1042 | 548 | 745 | ❌ |
| 47 | 水位闸排水 E2E 补验 | spec 707 补验 | T1043–T1044 | 549 | 746 | ❌ |
| 48 | 金丝雀路由生效读面 | spec 715 装配后读面 | T1045–T1046 | 550 | 747 | ❌ |
| 49 | 健康段指标命名守卫扩展 | spec 724 扩散 | T1047–T1048 | 551 | 748 | ❌ |
| 50 | 收口：全仓终验+台账核查+地图关闭 | D/E/F 会话收口模式 | T1049–T1050 | 552 | 749 | ❌ |

（每轮完成后置 ✅ 并在 Decisions so far 补行；轮次主题如遇缺口核查命中已实现，记 ruled-out 顺延下一主题，台账行内注明。）

## Not yet specified

- 泳道公平共享加权（WFQ）——与既有泳道容量语义叠加点未决，本轮不毕业
- 会话存活 TTL 只读化——维护模式门（spec 221）已覆盖主诉求，暂不毕业
- 语义缓存负缓存 TTL——F 会话已判定不毕业，维持

## Out of scope

- 不引入任何新的第三方运行时依赖（BOM/enforcer 收口优先；测试域新增依赖亦回避——边界守卫自写不引 ArchUnit）。
- 不做跨实例共享语义缓存的 Redis 向量存储大改（雾区单列 effort）。
- 不重构模块依赖星形拓扑、不改公共 API 破坏性签名（0.x 语义内也尽量避免）。
- 不与已收口会话主题撞车：每轮缺口核查含 D/E/F 已落地内容。
