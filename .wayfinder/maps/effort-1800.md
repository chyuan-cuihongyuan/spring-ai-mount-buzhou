# Effort #1800 总图 — O 会话 1800 系 150 轮自迭代（能力自补充与自进化第五弹）

> 会话：O（A–N 字母已占用：J=1000/K=1200/L=1400+1700/M=1500/N=1600）；本轮**直推 main 逐轮 push**（K 会话先例，用户常设授权 GitHub 自动提交）；启动 2026-09-16。
> 号段（fetch+双查 origin/main@bcf1322e 后占用，号段声明先行）：**efforts #1800–#1999、specs 1800–1949（150 轮=spec 号，1950+ 缓冲）、票 T2801–T3100（每轮 shape+verify 一对，T3101+ 缓冲）、impl 1401–1550（impl 1401+(N−1800)）**。
> 纪律：每轮四步（map→spec→tickets→implement）+ 双门（模块测试 + 全局三门：覆盖门/快照门/对账门）+ 一轮一 commit 一 push；**每 6 轮一対账轮**（R6k：全仓 mvn verify + 台账核账 + 合并 origin/main 吸收并行会话——J/K/L/M 均活跃）；README 纵深行逐轮即时登记。
> 对账门：R1 即落 `OSession1800LedgerAuditTest`（starter，LSession1700LedgerAuditTest 同款公式族）：spec N → shape 票 T2801+2(N−1800) / verify=+1 / impl 1401+(N−1800)，spec 起点断言 1800 严格递增——预防式对账而非事后补救。

## Destination

150 轮连续 effort（#1800–#1949）全部四步闭环：从高价值开源项目借鉴思想，在十大机制上落 ~140 个小纵切（读面/护栏/调度/一致性语义族，避开 L-1700 读面族与 M-1542 在途工作）+ 25 个对账轮（R6k），全仓三门绿、README/api-surface/MAP/台账四面一致、逐轮 push GitHub。

## 选题原则与借鉴定源（GitHub >10K★ 高价值项目思想）

避开已 mined 带（RetryBudget/WebhookOutboxLag/OutlierEjection/HedgedChatModel/AdaptiveTimeout/SleepTimeConsolidator 等已存在；L-1700 排程的 fork 形态/年龄分桶/续租抖动/批规模/合并节省/开关台账族）。本轮候选静脉（每轮落轮前 grep 复核前沿）：
Linux PSI（压力失速 some/full）、gRPC/Temporal（deadline 传播/活动心跳/重试策略分型）、JVM 分代（记忆层代晋升审计）、TCP BBR/BBRv2（pacing/带宽估计）、SQLite WAL（checkpoint 触发阈值/脏页水位）、Raft（领导租约 fencing 已有→选举期只读降级）、Postgres buffer cache（命中率/驱逐脏写）、RocksDB level compaction（层级放大因子）、Nginx/OpenResty（限流 leaky/freq 区间合并）、Consul（健康检查 TTL 状态机）、Nomad/K8s（驱逐信号驱逐阈值、descheduler 反热点）、RabbitMQ（prefetch/QoS 流控）、RocketMQ（事务半消息）、Letta/MemGPT（记忆块编辑冲突/self-editing 追踪）、mem0（衰减三分量 recency/frequency/importance）、LangGraph（checkpoint channel diff）、vLLM/SGLang（前缀块命中/radix LRU）、LiteLLM（路由粘性/冷却矩阵/context-window 预检）、Cedar/OPA（决策缓存 TTL/部分求值）、OpenFeature（flag 求值错误语义）、GitHub Actions（超时分级/重试上限）、Jenkins（构建稳定性 Readout）、Blazemeter（斜坡加压）、Chaos Toolkit（稳态假设断言）、Pinpoint/SkyWalking（拓扑边采样）、Zipkin（依赖聚合延迟百分位）。

## 已裁决（Decisions so far）

- 号段占用 1800–1999/T2801+/impl 1401+（origin 双查后空闲；见上）。
- 工作树 M 会话遗留（EvalRunner dry-run，引用未落盘的 spec 1542/T2335）**不动不提交**——其文书链未落，提交将制造台账断链；本会话提交一律定向 `git add` 自有文件（R67/R118 覆盖丢失教训的镜像处置）。
- 直推 main 而非分支（K 会话先例 + 共享检出唯一可行解；并行会话同在此检出工作）。
- 本地积压 2 提交（K R18 开工 + README 补登）已于开工前 push（f24dfefc..bcf1322e）。

## 排程表（滚动追加——Wave 1 先行，后续波次前沿推进后毕业）

| R | effort | 主题 | 票 | impl | 状态 |
|---|--------|------|----|------|------|
| R1 | #1800 | 1800 系对账门落位（OSession1800LedgerAuditTest 四面互证） | T2801–T2802 | 1401 | ✅ |
| R2 | #1801 | Spill 压力失速读面（Linux PSI some/full 语义） | T2803–T2804 | 1402 | ✅ |
| R3 | #1802 | 工具墙钟 deadline 传播预算（gRPC/Temporal deadline propagation） | T2805–T2806 | 1403 | ✅ |
| R4 | #1803 | 记忆层代晋升审计（JVM 分代 GC promotion 思想） | T2807–T2808 | 1404 | ✅ |
| R5 | #1804 | 语义缓存前缀块命中读面（vLLM block-level prefix cache） | T2809–T2810 | 1405 | ✅ |
| R6 | #1805 | 对账轮（全仓 mvn verify + 台账核账 + 合并 origin/main） | T2811–T2812 | 1406 | ✅ |
| R7 | #1806 | 检查点滞后读面（Kafka consumer lag / SQLite WAL checkpoint 思想） | T2813–T2814 | 1407 | ✅ |
| R8 | #1807 | 追限区间合并（Prometheus 告警分组/相邻窗合并思想） | T2815–T2816 | 1408 | ✅ |
| R9 | #1808 | 驱逐信号阈值门（K8s soft 宽限/hard 即逐思想） | T2817–T2818 | 1409 | ✅ |
| R10 | #1809 | 扇出 pacing 计划（TCP BBR pacing/惊群避让思想） | T2819–T2820 | 1410 | ✅ |
| R11 | #1810 | Prefetch 信用窗口（RabbitMQ basic.qos/AMQP credit 思想） | T2821–T2822 | 1411 | ✅ |
| R12 | #1811 | 对账轮（全仓 mvn verify + 台账核账 + 合并 origin/main） | T2823–T2824 | 1412 | ✅ |
| R13 | #1812 | 半消息审计（RocketMQ 事务半消息/回查思想） | T2825–T2826 | 1413 | ✅ |
| R14 | #1813 | TTL 探针状态机（Consul health check TTL 思想） | T2827–T2828 | 1414 | ✅ |
| R15 | #1814 | 反热点重平衡建议（K8s descheduler 思想） | T2829–T2830 | 1415 | ✅ |
| R16 | #1815 | 回填计划（缺口补填 catch-up 思想） | T2831–T2832 | 1416 | ✅ |
| R17 | #1816 | 负载脱落阶梯（Envoy overload manager 思想） | T2833–T2834 | 1417 | ✅ |
| R18 | #1817 | 对账轮（全仓 mvn verify + 台账核账 + 快照补登前置） | T2835–T2836 | 1418 | ✅ |
| R19 | #1818 | 顺序读预读顾问（Linux readahead 思想） | T2837–T2838 | 1419 | ✅ |
| R20 | #1819 | 预算花费匀速曲线（广告 spend pacing 思想） | T2839–T2840 | 1420 | ✅ |
| R21 | #1820 | 重启错峰计划（memberlist/consul+AWS jitter 思想） | T2841–T2842 | 1421 | ✅ |
| R22 | #1821 | 优先级反转暴露读面（Mars Pathfinder 教训） | T2843–T2844 | 1422 | ✅ |
| R23 | #1822 | 混沌预算门（Chaos Monkey/Chaos Toolkit 思想） | T2845–T2846 | 1423 | ✅ |
| R24 | #1823 | 对账轮（全仓 mvn verify + 台账核账 + 快照补登前置） | T2847–T2848 | 1424 | ✅ |
| R25 | #1824 | SWR 策略（HTTP Cache-Control/CDN 思想） | T2849–T2850 | 1425 | ✅ |
| R26 | #1825 | 会话休眠分级（k8s scale-to-zero 思想） | T2851–T2852 | 1426 | ✅ |
| R27 | #1826 | 会话布隆粗筛（Bloom filter 思想） | T2853–T2854 | 1427 | ✅ |
| R28 | #1827 | 优雅停机排空预测（k8s drain 思想） | T2855–T2856 | 1428 | ✅ |
| R29 | #1828 | 平滑加权轮询序列（NGINX smooth WRR 思想） | T2857–T2858 | 1429 | ✅ |
| R30 | #1829 | 对账轮（全仓 mvn verify + 台账核账 + 快照补登前置） | T2859–T2860 | 1430 | ✅ |
| R31 | #1830 | 多级缓存命中读面（Caffeine multi-level 思想） | T2861–T2862 | 1431 | ✅ |
| R32 | #1831 | 对冲延迟策略（Google Tail at Scale 思想） | T2863–T2864 | 1432 | ✅ |
| R33 | #1832 | 覆写环形缓冲（LMAX Disruptor 思想） | T2865–T2866 | 1433 | ✅ |
| R34 | #1833 | 续读令牌编解码裁决（continuation token/ETag 思想） | T2867–T2868 | 1434 | ✅ |
| R35 | #1834 | 完成度 ETA 投影（CI 进度条/带宽估计思想） | T2869–T2870 | 1435 | ✅ |
| R36 | #1835 | 对账轮（全仓 mvn verify + 台账核账 + 快照补登前置） | T2871–T2872 | 1436 | ✅ |
| R37 | #1836 | 库存周转读面（供应链 inventory turnover 思想） | T2873–T2874 | 1437 | ✅ |
| R38 | #1837 | 失败域配额（k8s failure-domain/分域备货思想） | T2875–T2876 | 1438 | ✅ |
| R39 | #1838 | 密钥轮换重叠窗（TLS 证书轮换/Vault grace 思想） | T2877–T2878 | 1439 | ✅ |
| R40 | #1839 | 反熵分歧账（Cassandra/Dynamo anti-entropy 思想） | T2879–T2880 | 1440 | ✅ |
| R41 | #1840 | 向量时钟偏序比较（Dynamo/Lamport 思想） | T2881–T2882 | 1441 | ✅ |
| R42 | #1841 | 对账轮（全仓 mvn verify + 台账核账 + 快照补登前置） | T2883–T2884 | 1442 | ✅ |
| R43 | #1842 | 隔离区普查（邮件隔离区/恶意样本沙箱思想） | T2885–T2886 | 1443 | ✅ |
| R44 | #1843 | 技能依赖图审计（npm/pip 依赖解析思想） | T2887–T2888 | 1444 | ✅ |
| R45 | #1844 | Wave 8 弹性位 C | T2889–T2890 | 1445 | ⬜ |
| R46 | #1845 | Wave 8 弹性位 D | T2891–T2892 | 1446 | ⬜ |
| R47 | #1846 | Wave 8 弹性位 E | T2893–T2894 | 1447 | ⬜ |
| R48 | #1847 | 对账轮（全仓 mvn verify + 台账核账 + 快照补登前置） | T2895–T2896 | 1448 | ⬜ |

## Not yet specified（雾区——候选静脉见「借鉴定源」，前沿推进后逐波毕业成排程行）

- Wave 2+（R7 起）：PSI 之后的调度/一致性静脉（BBR pacing / WAL checkpoint 水位 / buffer cache 命中 / level compaction 放大 / 限流区间合并 / 健康检查 TTL 状态机 / 驱逐信号阈值 / prefetch 流控 …）——每轮落轮前 grep 复核是否已被并行会话占坑，占坑即换静脉下一候选。

## Out of scope

- L-1700 已排程主题（fork 形态/续租抖动/合并节省/开关台账等 R7–R50 清单）不重复实现。
- M-1542 在途 dry-run 族（观察者通知隔离等 1500 系排程）不碰。
- 修改持久化 SPI 公共签名或跨模块依赖边界（星形白名单硬约束）。
