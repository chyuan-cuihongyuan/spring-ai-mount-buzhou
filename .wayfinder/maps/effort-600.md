# Wayfinder Map — F 会话 600 系：借鉴高价值开源项目的 50 轮自迭代（effort #600 总图）

> **F 会话**（2026-09-12 启动）：继 C（300 系）/ D（400 系）/ E（500 系，并行中）之后的第四条自迭代线。
> 用户常设授权（2026-09-12）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。
> **编号裁决（撞号归一后）**：F 会话占用 spec **600–649**、票 **T851–T950**（每轮 2 张）、impl **453–502**（每轮 1 片）；与 E 会话（500 系 / T751+ / impl403+）保持 50 轮全距互不侵犯。
> 起点：origin/main @ 4c0ed7a8（D 会话收口后）。

## Destination

完成 **50 个完整自迭代 loop**：每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或 opt-in 的机制改进；50 轮全部 Conventional Commits 提交并推送 GitHub，全仓 `mvn verify` 保持绿。

## Notes

- 每轮固定四步产物：决策票（同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<6NN>-<slug>.md`（+README 生产级纵深 VI 表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级测试必须绿；周期性 + 收口轮跑全仓 `mvn -B -ntp clean verify`；每轮 commit 后 push origin main。
- 选题纪律：每轮开工先缺口核查（grep spec+代码），已实现则台账记 `ruled-out` 顺延；E 会话已落地的主题同样回避。
- 本机 Docker 就绪后 store 容器测试本地同口径；CI（GitHub ubuntu）直连 central。

## Decisions so far

- [MCP 工具注解以观测面暴露并纳入漂移口径](../tickets/T851-mcp-tool-hints.md) — 注解=buzhou 自有 record 观测快照+同名注解翻转独立事件 `mcp.tool-hints-drift`；不做护栏裁决（不信任 server 自报元数据的既有决策不变）。
- [离群驱逐恐慌阈值的形态与默认值](../tickets/T852-outlier-panic-shape.md) — 取 panic（过滤侧忽略驱逐）不取比例上限；默认 0=关零行为变化；ceil 取整、严格低于才触发。
- [fork 谱系的落点](../tickets/T854-fork-lineage-shape.md) — OTel span-links 思想落为 state+事件（`buzhou.fork.source` + copy 计数），不造伪 span 谱系；两 fork 入口都写。
- [GCRA 后端的算法映射](../tickets/T856-gcra-backend-shape.md) — TAT 映射 RateLimitBackend SPI；opt-in 默认 β=0 严格平滑；available = 此刻可连发数。

## 50 轮台账

| # | 主题 | 借鉴源 | 票 | impl | spec | 状态 |
|---|------|--------|----|------|------|------|
| 1 | MCP 工具注解观测面 + 注解漂移 | modelcontextprotocol/spec | T851 | 453 | 600 | ✅ |
| 2 | 模型离群驱逐恐慌阈值 | envoy/outlier_detection | T852–T853 | 454 | 601 | ✅ |
| 3 | fork 谱系（span-links 思想关联面） | open-telemetry spec links | T854–T855 | 455 | 602 | ✅ |
| 4 | GCRA 平滑限流后端 | redis-cell / envoy GCRA | T856–T857 | 456 | 603 | ✅ |
| 5 | 事实置信度衰减 | letta-ai/letta memory | T858–T859 | 457 | 604 | ✅ |
| 6 | skill 混合排序（BM25+RRF） | weaviate/qdrant hybrid | T860–T861 | 458 | 605 | ✅ |
| 7 | 取消原因枚举传播 | grpc status codes | T862–T863 | 459 | 606 | ✅ |
| 8 | 黄金轨迹 payload 归一化 | approvals/approvaltests | T864–T865 | 460 | 607 | ✅ |
| 9 | 工具幂等键上下文传播 | stripe X-Idempotency-Key | T866–T867 | 461 | 608 | ✅ |
| 10 | 评估项级超时预算 | pytest-dev/pytest-timeout | T868–T869 | 462 | 609 | ✅ |
| 11 | MCP 每连接并发上限 | modelcontextprotocol 生态 | T870–T871 | 463 | 610 | ✅ |
| 12 | 语义缓存维度漂移可见性 | 模型漂移监控惯例 | T872–T873 | 464 | 611 | ✅ |
| 13 | 微压缩影子干跑评估 | istio mirroring | T874–T875 | 465 | 612 | ✅ |
| 14 | gzip 导出压缩档位 | nginx gzip_comp_level | T876–T877 | 466 | 613 | ✅ |
| 15 | GCRA yml 装配扩散 | D 会话装配轮模式 | T878–T879 | 467 | 614 | ✅ |
| 16 | API 快照门硬化（regenerate 门控） | flaky-test 治理惯例 | T880–T881 | 468 | 615 | ✅ |
| 17 | 技能目录清单指纹 | sigstore/cosign 清单 | T882–T883 | 469 | 616 | ✅ |
| 18 | 技能目录漂移看门狗 + 616 diff 方向修正 + outbox lag 毫秒竞态修复 | spec 201 镜像 | T884–T885 | 470 | 617 | ✅ |
| 19 | MCP 注解聚入健康面 | spec 600 扩散 | T886–T887 | 471 | 618 | ✅ |
| 20 | spill 预览头尾语义 | ripgrep context | T888–T889 | 472 | 619 | ✅ |
| 21 | 熔断时间窗衰减 | resilience4j TIME-based | T890–T891 | 473 | 620 | ✅ |
| 22 | 导出打包落盘持久档 | sqlite WAL 同步档位 | T892–T893 | 474 | 621 | ✅ |

（9–50 轮主题自雾区顺延；每轮补行。另：eval 数据集 schema 校验主题经核查已被 spec 134 DatasetExpectations 覆盖——ruled-out 顺延。）

## Not yet specified

雾区主题队列（顺序即优先级；每轮缺口核查后具体化，重复/已实现者记 ruled-out 顺延）：

- GCRA 平滑限流后端（redis-cell / envoy GCRA，opt-in 替代令牌桶）
- 事实置信度衰减（时间+冲突双驱动，letta memory）
- 并行输入护栏（openai agents sdk guardrails：与模型调用并发、可取消）
- 熔断时间滑动窗（resilience4j TIME-based）
- 健康分组 liveness/readiness 视图（若 332 探针未覆盖聚合面）
- 取消原因枚举传播（gRPC status codes）
- skill 检索混合 BM25 关键词打分（weaviate/qdrant hybrid）
- 语义缓存负缓存 TTL（nginx negative ttl）
- eval 数据集 schema 校验（json-schema）
- 黄金轨迹归一化（UUID/时间戳，approval tests）
- 工具调用幂等键自动传播（stripe idempotency-key）
- 记忆压缩影子干跑评估（istio mirroring 思想）
- outbox 死信率健康维度（prometheus alert rules 思想）
- MCP 每连接并发上限（mcp spec 并发协商）
- skill 目录 SHA-256 清单指纹（cosign 思想）
- 观测 JSONL fsync 档位（sqlite WAL 同步类比）
- fallback 演练注入窗（chaos monkey 固定窗）
- lease 续租抖动上限（grpc keepalive backoff cap）
- 语义缓存维度漂移检测（model drift）
- 事件总线慢消费者分级（akka bounded mailbox）
- eval 项级超时预算（pytest-timeout）
- 记忆 fact 冲突策略可配（delta lake merge）
- 指标命名 _total 审计（prometheus naming）
- 熔断 OPEN 预热半开（envoy slow-start）
- 会话归档冷读限速（nginx limit_conn）
- starter 装配诊断报告（spring-boot diagnostics）
- 工具结果裁剪 dry-run 预览（jq dry-run）
- 前缀缓存命中率细粒度口径（vllm prefix cache）
- 会话索引 keyset 分页稳定性（postgres keyset）
- 错误签名聚合窗导出（es date_histogram）
- 预算池借用利率上限（k8s limit-ratio）
- redteam 门阈值 yml 化（gh actions inputs）
- run 恢复巡检抖动（alert for-range）
- skills 目录变更通知合并（fsnotify）
- gzip 导出压缩档位（nginx comp_level）

## Out of scope

- 不引入任何新的第三方运行时依赖（BOM/enforcer 收口优先）。
- 不做跨实例共享语义缓存的 Redis 向量存储大改（雾区单列 effort）。
- 不重构模块依赖星形拓扑、不改公共 API 破坏性签名（0.x 语义内也尽量避免）。
- 不与 E 会话（500 系）抢主题：每轮缺口核查含 E 分支已落地内容。
