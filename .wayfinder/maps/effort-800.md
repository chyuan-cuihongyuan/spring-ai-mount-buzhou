# Wayfinder Map — H 会话 800 系：借鉴高价值开源项目的 100 轮自迭代（effort #800 总图）

> **H 会话**（2026-09-13 启动）：继 C（300 系）/ D（400 系）/ E（500 系）/ F（600 系）/ G（700 系）之后的第六条自迭代线。
> 用户常设授权（沿 F/G 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。
> **编号裁决**：H 会话占用 spec **800–899**、票 **T1051–T1250**（每轮 2 张：shape + verify）、impl **553–652**（每轮 1 片）；与已收口的 G 会话（700 系 / T951–T1050 / impl 503–552）保持全距互不侵犯。
> 起点：origin/main @ e3dcd274（G 会话收口后，全仓 verify 绿）。

## Destination

**100 个完整自迭代 loop 全部完成**——每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或 opt-in 的机制改进；100 轮全部 Conventional Commits 提交并推送 GitHub，终验全仓 `mvn -B -ntp clean verify` 绿（16 模块 + JaCoCo ≥70% + enforcer + 快照门 + SpecCoverage）。

## Notes

- 每轮固定四步产物：决策票（同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<8NN>-<slug>.md`（+README 生产级纵深表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级测试必须绿；周期性（每 10 轮）+ 收口轮跑全仓 `mvn -B -ntp clean verify`；每轮 commit 后 push origin main。
- 选题纪律：每轮开工先缺口核查（grep spec+代码），已实现则台账记 `ruled-out` 顺延；D/E/F/G 会话已落地的主题同样回避。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J 占位符、record/sealed 优先；新公共类入 `api` 包需 Javadoc + API 快照随轮再生。
- 模块依赖边界不变：feature 模块互不直接依赖，跨机制协作走 core 事件总线或 core SPI。

## Decisions so far

- [事件丢弃按原因分类读面](../tickets/T1051-event-drop-breakdown-shape.md) — EventDropBreakdown（drop-oldest/block-timeout/closed-undelivered 等分桶）+ eventDropBreakdown() 读面（SYNC empty 与 eventBusStats 同构）；ΣbyReason 守恒 == dropped；EventBusStats 原样不动（Sentry discarded events）。

## 100 轮台账

| # | 主题 | 借鉴源 | 票 | impl | spec | 状态 |
|---|------|--------|----|------|------|------|
| 1 | 事件丢弃按原因分类读面（原列「outbox 积压深度健康面」ruled-out——spec 135 已覆盖） | Sentry discarded events | T1051–T1052 | 553 | 800 | ✅ |
| 2 | （原列「健康段属性截断上限」ruled-out——BuzhouHealth 有界详情纪律+逐点防御式截断已到位，无实际缺陷；顺延）定时任务 jitter 防同步雪崩？（开工时按缺口核查选题） | 候选池 | T1053–T1054 | 554 | 801 |  |
| 3 | 健康段属性截断上限 | OpenTelemetry attribute limits | T1055–T1056 | 555 | 802 |  |
| 4 | logfmt 单行日志模式 | logfmt（Heroku/pborman/logfmt） | T1057–T1058 | 556 | 803 |  |
| 5 | 响应缓存水位读面 | Caffeine recordStats | T1059–T1060 | 557 | 804 |  |
| 6 | 失败分类计数分离 | Envoy outlier 4xx/5xx 分离 | T1061–T1062 | 558 | 805 |  |
| 7 | 弹射比例上限 | Envoy max_ejection_percent | T1063–T1064 | 559 | 806 |  |
| 8 | 重试预算 | Finagle/Envoy retry budget | T1065–T1066 | 560 | 807 |  |
| 9 | 长工具心跳时长观测 | Temporal activity heartbeat | T1067–T1068 | 561 | 808 |  |
| 10 | 评估 SLA miss 读面 | Airflow sla_miss | T1069–T1070 | 562 | 809 |  |
| 11 | 特性开关评估读面 | OpenFeature / flagd | T1071–T1072 | 563 | 810 |  |
| 12 | 配置归属溯源读面 | git blame / 配置覆盖链 | T1073–T1074 | 564 | 811 |  |
| 13 | webhook HMAC 签名 | GitHub X-Hub-Signature-256 | T1075–T1076 | 565 | 812 |  |
| 14 | webhook 幂等键读面 | Restate idempotency keys | T1077–T1078 | 566 | 813 |  |
| 15 | 重复违规累进封禁 | fail2ban 递增 ban | T1079–T1080 | 567 | 814 |  |
| 16 | 技能目录指纹变更观测 | LSP didChangeWatchedFiles | T1081–T1082 | 568 | 815 |  |
| 17 | MCP 工具清单版本漂移事件 | MCP notifications/list_changed | T1083–T1084 | 569 | 816 |  |
| 18 | 评估中途剪枝 | Optuna pruner | T1085–T1086 | 570 | 817 |  |
| 19 | pass@k 无偏指标 | HumanEval/Codex pass@k | T1087–T1088 | 571 | 818 |  |
| 20 | 评估 bootstrap 置信区间 | Efron bootstrap / statmodels | T1089–T1090 | 572 | 819 |  |
| 21 | 评估分组汇总读面 | lm-eval-harness task groups | T1091–T1092 | 573 | 820 |  |
| 22 | 会话内存占用估算读面 | Redis MEMORY USAGE | T1093–T1094 | 574 | 821 |  |
| 23 | 压缩驱逐 LRU-K 候选序 | PostgreSQL buffer LRU-K | T1095–T1096 | 575 | 822 |  |
| 24 | 租户窗口用量读面 | Stripe usage records | T1097–T1098 | 576 | 823 |  |
| 25 | prompt 前缀缓存命中观测 | Anthropic prompt caching | T1099–T1100 | 577 | 824 |  |
| 26 | 模型抢占语义观测 | vLLM preemption | T1101–T1102 | 578 | 825 |  |
| 27 | 校验失败重问上限 | guardrails-ai reask | T1103–T1104 | 579 | 826 |  |
| 28 | 断言反馈回路 | DSPy assertions | T1105–T1106 | 580 | 827 |  |
| 29 | 工具结果过期引用读面 | HTTP Cache-Control max-age | T1107–T1108 | 581 | 828 |  |
| 30 | 会话导出滚动校验和增量 | rsync rolling checksum | T1109–T1110 | 582 | 829 |  |
| 31 | store 扫描游标稳定性 | Redis SCAN cursor 语义 | T1111–T1112 | 583 | 830 |  |
| 32 | outbox 批量 AIMD 自适应 | TCP 拥塞控制 AIMD | T1113–T1114 | 584 | 831 |  |
| 33 | 审计链 Merkle 根读面 | Certificate Transparency | T1115–T1116 | 585 | 832 |  |
| 34 | 事件重放序号缺口检测 | Kafka log gap / consumer offsets | T1117–T1118 | 586 | 833 |  |
| 35 | 健康加权评分读面 | K8s probe aggregate / HTTP health score | T1119–T1120 | 587 | 834 |  |
| 36 | 影子分叉对比报告面 | shadow testing report | T1121–T1122 | 588 | 835 |  |
| 37 | 租约续约活性观测 | k8s client-go leaderelection | T1123–T1124 | 589 | 836 |  |
| 38 | 配额预测读面 | Prometheus predict_linear | T1125–T1126 | 590 | 837 |  |
| 39 | 压缩策略建议读面 | pg advisor / pg_stat_statements 建议 | T1127–T1128 | 591 | 838 |  |
| 40 | 技能使用统计读面 | npm downloads 面 | T1129–T1130 | 592 | 839 |  |
| 41 | 拒绝趋势分桶读面 | fail2ban / 拒绝日志时序化 | T1131–T1132 | 593 | 840 |  |
| 42 | 导入严格校验模式 | pg_restore / protobuf unknown fields | T1133–T1134 | 594 | 841 |  |
| 43 | store fsck 修复建议面 | git fsck / postgres amcheck | T1135–T1136 | 595 | 842 |  |
| 44 | 事件水位线读面 | Flink watermark | T1137–T1138 | 596 | 843 |  |
| 45 | 软截止分层提醒 | K8s terminationGracePeriod / SIGTERM→SIGKILL | T1139–T1140 | 597 | 844 |  |
| 46 | 归档验证回读面 | backup verify-restore 惯例 | T1141–T1142 | 598 | 845 |  |
| 47 | 路由健康 EWMA 平滑 | Envoy outlier EWMA | T1143–T1144 | 599 | 846 |  |
| 48 | 指标标签值集守卫 | prometheus label cardinality 约束 | T1145–T1146 | 600 | 847 |  |
| 49 | 50–54 | T1147–T1156 | 601–605 | 848–852 | （按缺口核查顺延装配） |
| 55–64 | yml 装配轮 ×10（H 前段 opt-in 机制装配） | D/G 会话装配轮模式 | T1157–T1176 | 606–615 | 853–862 |  |
| 65–80 | 联动补验 e2e ×16 | D/E/F/G 补验模式 | T1177–T1208 | 616–631 | 863–878 |  |
| 81–88 | 契约套件同构扩散 ×8 | spec 705/743 同构扩散 | T1209–T1224 | 632–639 | 879–886 |  |
| 89–93 | 压测/稳定深验 ×5 | G 会话压测模式 | T1225–T1234 | 640–644 | 887–891 |  |
| 94–96 | 中点快照再生 / 台账预检 / 深验 | spec 748/745 惯例 | T1235–T1240 | 645–647 | 892–894 |  |
| 97–99 | 收口预检 ×2 + 地图雾区清理 | D/E/F/G 收口预检模式 | T1241–T1246 | 648–650 | 895–897 |  |
| 100 | 收口：全仓终验+台账核查+地图关闭 | D/E/F/G 会话收口模式 | T1247–T1250 | 651–652 | 898–899 |  |

（每轮完成后置 ✅ 并在 Decisions so far 补行；轮次主题如遇缺口核查命中已实现，记 ruled-out 顺延下一主题，台账行内注明。49–54 号段为机动机制轮，开工时按缺口核查从候选池选取。）

## Not yet specified

（开工时为空；收口时清理。）

## Out of scope

- 不引入任何新的第三方运行时依赖（BOM/enforcer 收口优先；测试域新增依赖亦回避）。
- 不重构模块依赖星形拓扑、不改公共 API 破坏性签名（0.x 语义内也尽量避免）。
- 不与已收口会话主题撞车：每轮缺口核查含 D/E/F/G 已落地内容。
- 不做 FPE/FF1 等需密码学依赖的主题（G 会话已判定 out of scope，维持）。
