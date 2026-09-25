# Effort #6000 总图 — T 会话 6000 系 50 轮自迭代（能力自补充与自进化第十弹）

> 会话：T（A–S 字母已占用：S=5000 已收口）；本轮直推 main 逐轮 commit（GitHub 离线时本地逐轮 commit、对账轮补 push）。
> 号段（本地全档实查空闲：specs 6000+ 全空、tickets T6201+ 全空、impl 2201+ 全空；S 系 5000 段已收口封卷）：**efforts #6000–#6049（50 轮）、specs 6000–6049、票 T6201–T6300（每轮 shape+verify 一对，shape=6201+2(N−6000)）、impl 2201–2250（impl = 2201+(N−6000)）**。
> 纪律：每轮四步（map→spec→tickets→implement）+ 双门（模块测试 + 全局三门：覆盖门/快照门/对账门）+ 一轮一 commit；**每 6 轮一对账轮**（T6k：快照批补登 + 全仓离线 verify + 台账核账）；README 纵深行逐轮即时登记（SpecCoverageTest 双向门）。
> 对账门：T1 即落 `TSession6000LedgerAuditTest`（SSession5000LedgerAuditTest 同款公式族第七应用）：spec N → shape 票 6201+2(N−6000) / verify=+1 / impl 2201+(N−6000)，spec 起点断言 6000 严格递增。

## Destination

T 会话四步闭环持续推进：从高价值开源项目（>10K stars）借鉴思想，落小纵切组件 + 周期对账轮，全仓三门绿、README/api-surface/MAP/台账四面一致、逐轮 commit。

## 选题原则与借鉴定源（GitHub 高价值项目思想）

已实现带避让：每轮落轮前 grep 快照/README 复核（S-5000 全 50 轮、R-4000/Q-3000/P-2000 全量及更早 A–R 系）；S/R 雾区候选不抢。本轮候选静脉（占坑即换下一候选）：

- **结构与查询族**：伸展树（Sleator-Tarjan 自调整 BST）、树堆（Seidel-Aragon 随机化 BST）、稀疏表（Bender-Farach 静态 RMQ O(1) 查询）、单调队列（滑动窗口最值摊还 O(1)）
- **文本与检索族**：Aho-Corasick 多模式匹配（ripgrep/安全扫描器同源）、Myers O(ND) diff（git diff 思想）、BK 树（编辑距离邻域树）、后缀数组（Manber-Myer 倍增 + 子串二分）、Piece Table 文本缓冲（VSCode/Word 思想）
- **列式编码族**：Elias-Fano 单调序列编码（Lucene）、Gorilla XOR 浮点压缩（Facebook Gorilla/Prometheus TSDB）、Simple8b 位打包（InfluxDB）、固定位宽打包（Parquet RLE/bit-packing 面）、字典编码（Parquet/ORC 列存）
- **空间检索族**：KD 树（scikit-learn 邻域面）、四叉树（地理围栏点域）、Geohash（Redis GEO 思想）、Hilbert 曲线（空间填充局部性——ZOrderCurve 同族不同面）
- **并发调度族**：MPSC 有界队列（JCTools 思想）、条带锁（Guava Striped 键分桶）、索引堆（位置映射 + decrease-key）、步幅调度（MIT Stride 确定性比例份额）、多级反馈队列（OS 调度经典 MLFQ）
- **内存存储族**：伙伴分配器（Linux buddy allocator）、外归并排序（Spark/数据库外部排序）、可扩目录哈希（Fagin extendible hashing）、电梯扫掠（磁盘调度 SCAN/LOOK）、跳房子哈希（Herlihy hopscotch 邻域探测）
- **估计采样族**：Greenwald-Khanna ε 确定性分位摘要、KLL 随机化压实分位草图（Apache DataSketches/Druid）、稳定 Bloom 流式衰减过滤（Stable Bloom）、二择一负载均衡（Mitzenmacher power of two choices）、A-Chao 加权蓄水池采样
- **公平治理族**：等待图死锁检测（DB2/SQL Server 锁表面）、AIMD 窗口（TCP 拥塞避免思想）、MCS 队列锁（Linux 内核本地自旋）、彩票调度（Waldspurger lottery 比例份额）、Van Emde Boas 有界宇宙树
- **Wave 9 独件**：双堆中位数流（streaming median 两堆经典）

## 已裁决（Decisions so far）

- 号段占用 6000–6049/T6201–T6300/impl 2201–2250（本地全档实查 + S 系收口交接声明）。
- 直推 main 逐轮 commit；每轮定向 `git add` 自有文件；对账轮尝试 push（离线则记「离线」）。
- T1 = 对账门落位轮；T6k（6/12/18/24/30/36/42/48）= 对账轮；T50 = 收口对账轮。
- 全仓 verify 遇已入档满载偶红 flaky 按协议排除重跑（R48 口径）；-rf 续跑不满足快照门全 reactor 口径——从根跑为准（R48 入档）。

## 排程表（滚动追加——Wave 1 先行，后续波次前沿推进后毕业）

| T | effort | 主题 | 票 | impl | 状态 |
|---|---|---|---|---|---|
| T1 | #6000 | 6000 系对账门落位（TSession6000LedgerAuditTest 七应用） | T6201–T6202 | 2201 | ✅ |
| T2 | #6001 | Splay Tree 伸展树（Sleator-Tarjan 自调整 BST 思想） | T6203–T6204 | 2202 | ✅ |
| T3 | #6002 | Treap 树堆（Seidel-Aragon 随机优先级 BST 思想） | T6205–T6206 | 2203 | ✅ |

## Not yet specified（雾区——候选静脉见「借鉴定源」，前沿推进后逐波毕业成排程行）

- Wave 1（T2–T5）：结构与查询族——伸展树 / 树堆 / 稀疏表 / 单调队列。
- Wave 2（T7–T11）：文本与检索族——Aho-Corasick / Myers diff / BK 树 / 后缀数组 / Piece Table。
- Wave 3（T13–T17）：列式编码族——Elias-Fano / Gorilla XOR / Simple8b / 固定位宽打包 / 字典编码。
- Wave 4（T19–T23）：空间检索族——KD 树 / 四叉树 / Geohash / Hilbert 曲线 / 插值查找。
- Wave 5（T25–T29）：并发调度族——MPSC 队列 / 条带锁 / 索引堆 / 步幅调度 / 多级反馈队列。
- Wave 6（T31–T35）：内存存储族——伙伴分配器 / 外归并排序 / 可扩哈希 / 电梯扫掠 / 跳房子哈希。
- Wave 7（T37–T41）：估计采样族——Greenwald-Khanna / KLL / 稳定 Bloom / 二择一 / A-Chao 加权蓄水池。
- Wave 8（T43–T47）：公平治理族——等待图死锁检测 / AIMD 窗口 / MCS 锁 / 彩票调度 / Van Emde Boas。
- Wave 9（T49）：双堆中位数流（独件）。
- T6/T12/T18/T24/T30/T36/T42/T48 = 对账轮；T50 = 收口对账轮。
- 每轮落轮前 grep 复核是否已被并行会话占坑，占坑即换静脉下一候选。

## Out of scope

- Q–S 系余量号段保留各自会话续轮，T 不占用（S-5000 已收口不再占用）。
- Q/P/R/S 雾区候选静脉（其 map「借鉴定源」列出的未实现候选）原则上不抢。
- 已 mined 带不重复实现（A–S 全系）。
- 修改持久化 SPI 公共签名或跨模块依赖边界（星形白名单硬约束）。
