# C 会话进度台账（rolling，收口轮据此归档）

号段：.wayfinder300-349 ｜ spec 300-349 ｜ 票 T591+ ｜ impl323+ ｜ README 段「生产级纵深 IV」。
验证纪律：受影响模块测试逐轮绿 + 双轮一批；本地 Windows 排除集
`!ClasspathSkillScannerTest,!RunCommandToolTest,!GuardAndHitlDemoTest,!TenantSandboxTest,!PropertyInvariantsTwoTest,!ErrorSignaturesTest`
（后四类为存量本地 flaky，CI/Linux 权威——#300 MAP 台账）；mvn install 被 Mimosa
钩子拦，跨模块测试一律 `-pl <mod> -am test` 带排除集。API 快照：新公共类型
（RetryBudgetHolder/CompensatingBatch/Step 等）收口轮统一 regenerate。

| 轮 | effort | spec | 票 | impl | 主题 | 提交 |
|----|--------|------|----|------|------|------|
| 1 | #300 | 300 | T591-592 | 323 | 批内合并接线 | 7cde0ca |
| 2 | #301 | 301 | T593-594 | 324 | 对冲装配面 | be31b31 |
| 3 | #302 | 302 | T595-596 | 325 | 重试预算接线 | d6c73cc |
| 4 | #303 | 303 | T597-598 | 326 | 围栏持久纪元 | 4251576 |
| 5 | #304 | 304 | T599-600 | 327 | 事务批补偿 saga | 5ef0998 |
| 6 | #305 | 305 | T601-602 | 328 | 工具健康探测装配（Consul） | c185d91 |
| 7 | #306 | 306 | T603-604 | 329 | 工具熔断 yml 面（resilience4j） | d0a2458 |
| 8 | #307 | 307 | T605-606 | 330 | 事件 schema yml 声明 | d45b48a |
| 9 | #308 | 308 | T607-608 | 331 | deadline 跨工具传播（gRPC） | 44281f6 |
| 10 | #309 | 309 | T609-610 | 332 | 影子对照明细 JSONL（W&B） | 9878542 |
| 11 | #310 | 310 | T611-612 | 333 | 空闲会话后台压缩（LSM） | 41bfaec |
| 12 | #311 | 311 | T613-614 | 334 | 时间旅行 fork（LangGraph） | 2737f3d |
| 13 | #312 | 312 | T615-616 | 335 | 健康告警规则（Grafana） | 96c73e3 |
| 14 | #313 | 313 | T617-618 | 336 | PII 命中分侧（Presidio） | 76571ce |
| 15 | #314 | 314 | T619-620 | 337 | 价目快照随单（复式记账） | 369d726 |
| 16 | #315 | 315 | T621-622 | 338 | 虚拟 key 配额 Redis 共享 | 3e5fdb2 |
| 17 | #316 | 316 | T623-624 | 339 | 泳道许可共享（fog 227 共享族收口） | 121e001 |
| 18 | #317 | 317 | T625-626 | 340 | 导出族合流打包（OCI artifact） | 4e0c35c+2786d77（测试修 2 补） |
| 19 | #318 | 318 | T627-628 | 341 | 会话扰乱预算（K8s PDB——维护三件套齐） | dd73cf5 |
| 20 | #319 | 319 | T629-630 | 342 | 舱压伸缩建议（K8s HPA） | e1b2d89 |
| 21 | #320 | 320 | T631-632 | 343 | 舱容量热调整（Spring Cloud rebind） | da6d8d3 |
| 22 | #321 | 321 | T633-634 | 344 | SLO 错误预算燃尽率（Google SRE） | 97bdfaf |
| 23 | #322 | 322 | T635-636 | 345 | 工具混沌注入（Chaos Monkey） | 4f9c249 |
| 24 | #323 | 323 | T637-638 | 346 | 干跑拦截/执行计划（Terraform plan） | ff976e1 |
| 25 | #324 | 324 | T639-T640 | 347 | 工具金丝雀发布（Istio/Flagger） | c2f53be |
| 26 | #325 | 325 | T641-T642 | 348 | 工具紧急停用开关（kill switch） | 1ca5002 |
| 27 | #326 | 326 | T643-T644 | 349 | 轮次重复检测（context rot） | ba6db61 |
| 28 | #327 | 327 | T645-T646 | 350 | 工具循环断路器（调用形态断路） | 1fe3dce |
| 29 | #328 | 328 | T647-T648 | 351 | 干跑计划 JSONL 导出（导出族） | 54f95be |
| 30 | #329 | 329 | T649-T650 | 352 | API 快照收口（半程防线+跨平台修） | 01e97a8 |
| 31 | #330 | 330 | T651-T652 | 353 | 告警静默窗与抑制规则（Alertmanager） | cae3a94 |
| 32 | #331 | 331 | T653-T654 | 354 | 后台任务选主（K8s leader election） | 6b782ee |
| 33 | #332 | 332 | T655-T656 | 355 | 健康三探针分层（K8s probes） | 72a73da |
| 34 | #333 | 333 | T657-T658 | 356 | 消息静态信封加密（Vault/KMS envelope） | 1fa3b78 |
| 35 | #334 | 334 | T659-T660 | 357 | 成本归因台账（Kubecost 标签归因） | 902e2ed |
| 36 | #335 | 335 | T661-T662 | 358 | 错误预算政策·烧穿自动降级（Google SRE） | 07bf137 |
| 37 | #336 | 336 | T663-T664 | 359 | 摘要槽信封加密（333 扩散轮） | 0062bcc |
| 38 | #337 | 337 | T665-T666 | 360 | 工具上下文行李（W3C Baggage/OTel） | ae20e1d |
| 39 | #338 | 338 | T667-T668 | 361 | 预算软预警线（AWS Budgets） | 9630cc5 |
| 40 | #339 | 339 | T669-T670 | 362 | 多模型加权路由（LiteLLM Router） | 6cb4719 |
| 41 | #340 | 340 | T671-T672 | 363 | 路由权重热调整（320 rebind 同模式） | ac49b8f |
| 42 | #341 | 341 | T673-T674 | 364 | 选主扩散：归档清理与空闲压缩 | b84c48f |
| 43 | #342 | 342 | T675-T676 | 365 | 维护窗口 cordon（K8s cordon+地板多源） | 3e8f208 |
| 44 | #343 | 343 | T677-T678 | 366 | 生效配置自描述端点（configprops+掩码） | d1044a5 |

## 纪律修正（R18 教训）
- **验证命令一律 `mvn ... > /tmp/rN.log 2>&1; echo "MVN_EXIT=$?"` 后看日志**——
  管道 grep 匹配即退 0 会掩盖 BUILD FAILURE（R18 曾两次带红提交，教训入账）。
- **R36 修正**：R35 曾误建 `.wayfinder335/`（应为 `.wayfinder334`——目录号=
  effort 号），已 `git mv` 归位并随 R36 提交；后续轮开工前先对表号段。

## 备忘（更新）
- jedismock 实测支持 EVAL——Redis Lua 后端可本地全测（R16 发现）。
- README 纵深 IV 已有行至 spec 329。
- 验证命令排除集新增 `!UnsubscribedStreamTest`（R24 全跑偶发 R25 起常驻
  排除——单跑绿佐证存量时序 flaky，CI/Linux 权威）。R26 又见
  TurnStallWatchdogTest 全跑时序 flaky（单跑绿，与改动无共享状态）——
  下轮起一并入排除集。
- R30 收口：快照已 regenerate +38 型全量入档 api-surface.md；**快照机已
  跨平台**——后续轮新公共类型**随轮 regenerate**（Windows 本机可跑：
  `mvn -pl buzhou-spring-boot-starter -am test -Dtest='ApiSurfaceSnapshotTest#regenerateSnapshot'`
  后比对测试须 Skipped: 0 且绿）。原「待快照追加」清单就此清账作废。
- 宽反应堆 Windows 观察：buzhou-memory SleepTimeConsolidationTest 异步
  时序 flaky（与 UnsubscribedStream/TurnStallWatchdog 同类——CI/Linux
  权威）；SpillHealthTest /dev/null 假红已在 R30 修（文件下建子路径
  双平台 ENOTDIR）。
- R20 新坑：@Bean 方法 return null（NullBean）的**定义类型仍参与类型匹配**——
  doesNotHaveBean(Class) 不认账（307 先例能过纯因断言的是子类型）；复合
  条件装配用 Binder 预绑 Condition（312 同法），别走 NullBean。

## 备忘
- 泳道公平模式（原 R7 计划）勘察发现 spec 173 已用公平 Semaphore——弃。
- README 纵深 IV 表当前行：300/301/302/303/304/305+306。
- 新公共类型待快照：RetryBudgetHolder、CompensatingBatch(+Step)、ToolHealth、
  SequenceFence.Verdict.STALE（枚举成员不增类型）、Hedge/RetryBudgetParams/
  Health/Circuit（嵌套 record 属性类）、BulkheadScalingAdvisor(+Advice)。收口轮 regenerate。
