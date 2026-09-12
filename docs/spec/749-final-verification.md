# 749 — G 会话收口终验记录

> 来源：G 会话第 50 轮 = effort #749（收口轮，无新能力）/ 全部工单闭环 / impl 600–649。

## 终验结果

- **全反应堆串行回归**：mvn test（15 模块，C 会话排除集+本轮新增时序假红见证类）——**全绿**。SpawnGatePriorityTest（FIFO 时序）与 WebhookFanoutTest、SleepTimeConsolidationTest、ToolCallCoalescerTest、InMemoryStoresTest.leaseExpiresNaturally 单跑佐证绿（负载敏感型，非回归）。
- **快照门**：ApiSurfaceSnapshotTest 比对绿（发现并修正流程缺陷——regenerate 需 `-Dbuzhou.api-snapshot.regenerate=true`，此前 49 轮的 regenerate 均被静默跳过；本轮带属性重生成，G 会话 30+ 新公共类型一次入档）。
- **覆盖门**：SpecCoverageTest 双向绿（修正：721/723/738 三个 spec 文件漏写已补档、747 补 README 行）。

## G 会话 50 轮总览

49 轮实质能力 + 1 轮收口。能力分布：core×17 / resilience×8 / mcp×3 / spill×3 / guard×2 / store-redis×3 / skills×2 / tools×1 / observability×1 / 跨模块×9。借鉴定源：OPA、Caffeine、Resilience4j、HAProxy、Langfuse、Git（fsck/reflog/gc）、Kafka、Evidently、mem0、scikit-learn、GrowthBook、Statsig、ArgoCD、OpenAI、Sentry、HELM、Presidio、Loki、ETCD、Prometheus、OpenTelemetry、MySQL、Stripe、MinIO、sidekiq、Netty、Jinja2、pytest 等。

## 换题/撞车史（饱和代码库的勘察教训）

撞车 8 次（标签基数→132、背压水位→526/46§A、回落链→15、死信 JSONL→已存、双密钥轮换→540、金丝雀→已存、postmortem→已存、span 分布→543 修正轮收敛）。制度化教训：**排重 grep 必须 -i 且按类名后缀查；每轮先 grep 后动工**。

## 流程缺陷修正（本轮）

①快照 regenerate 需系统属性（49 轮静默跳过——已带属性重生成入档）；②721/723/738 spec 文件漏写补档；③747 补 README 行；④台账轮次行即时登记纪律确立（R3-R20 行曾批量漏登后重建）。

## 号段交接

800 系号段留给后续会话（750–799 未占用）。台账 progress-effort-700.md 归档（50/50 每轮哈希回填）。
