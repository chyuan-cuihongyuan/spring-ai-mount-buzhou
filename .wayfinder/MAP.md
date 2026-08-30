# Wayfinder 总索引 — Buzhou（effort #1–#15 全闭合）

> **单一目录**：2026-08-17 起，原按 effort 分裂的 `.wayfinder/`、`.wayfinder2/` … `.wayfinder15/` 十五个目录融合为本目录；**勿再新建 `.wayfinderN/`**。
> 每个 effort 一张 map（`maps/effort-NN.md`），其运行期 tracker 约定存档于 `maps/readme-effort-NN.md`（#2–#10）。
> 票号全局连续 **T1–T248**（T231–T239 跳号未用；**下一张 = T249**）；impl 切片 **01–195**（编号史见 [impl/README.md](impl/README.md)；**下一片 = 196**）。
> 累计 160 轮全仓 `mvn -B -ntp clean verify` 绿（截至 effort #15 收口）。

**开新 effort**：新建 `maps/effort-16.md`（Destination / Notes / Not yet specified / Out of scope），在下表登记一行；票从 T249 起、切片从 196 起全局续号。tracker 约定见 [README.md](README.md)。

| Effort | 主题 | Map | 票号 | impl |
|---|------|-----|------|------|
| #1 | core 做深做透（CI 根因 / alpha 措辞 / Spring AI 边界文档 / 可运行 demo / 真实 LLM 测试 / run_command 安全默认 / DoD 基线 / best-of-breed Tier-1 落地） | [maps/effort-01.md](maps/effort-01.md) | T1–T27 | 01–08（局部号；[SPEC.md](SPEC.md)） |
| #2 | 做完美 Tier-2/3 全量（FakeChatModel / 取消三档 / run 注册表 / 事件溯源工具日志 / interrupt-resume / time-travel fork / 压缩治理 / 向量 recall / 红队门 / 审计链 / 策略引擎 / 沙箱） | [maps/effort-02.md](maps/effort-02.md) | T28–T54 | 01–27（局部号） |
| #3 | 生产级收口（Turn deadline / 优雅停机 / schema 迁移 / 事务正确性 / 租约 fence / 事件背压 / 级联清理 / 保留策略 / 审计持久化 / 配置参数化） | [maps/effort-03.md](maps/effort-03.md) | T55–T68 | 28–43 |
| #4 | 全模块生产级收口（resilience 移植 / 可观测加固 / otel / dashboard 安全 / tools / mcp / skills / redteam 真实性 / CI 质量工程 / 文档终验） | [maps/effort-04.md](maps/effort-04.md) | T69–T80 | 44–55 |
| #5 | 生产级纵深（熔断器 / 回退模型链 / token 成本预算 / 会话配额 / 沙箱收敛 / 结构化输出 / session fork / 事件 webhook / 供应链 / perf 基线 / 多实例语义 / API 稳定） | [maps/effort-05.md](maps/effort-05.md) | T81–T102 | 56–77 |
| #6 | 生产级纵深（webhook 持久 outbox / 自适应 half-open / fork 证据生命周期 / 多模态输入 / 会话导出导入 / store fsck / 会话索引 / 工具结果上限 / 黄金轨迹评估） | [maps/effort-06.md](maps/effort-06.md) | T103–T111 | 78–86 |
| #7 | 生产级纵深（索引契约矩阵 / outbox 前缀扫描 / 压缩事件 / 黄金轨迹 A·B / half-open 探针 / skills 目录预算 / 媒体字节入档 / 导出扩展 / perf 哨兵 / 发布 SBOM） | [maps/effort-07.md](maps/effort-07.md) | T112–T131 | 87–104 |
| #8 | 生产级纵深（skill 搜索 / 死信重放 / 索引保留 / 阶梯压缩事件 / store 迁移器 / 黄金 C·D / 新面红队 / 背压审计 / 健康新维度 / 社区文件） | [maps/effort-08.md](maps/effort-08.md) | T132–T150 | 105–121 |
| #9 | 生产级纵深（spill 加密 / singleflight 闸 / 审计轮换锚 / 时钟注入 / 迁移器护栏 / 读降级 / 命令输出上限 / 配置校验 / 停机排空 / 指标强制） | [maps/effort-09.md](maps/effort-09.md) | T151–T169 | 122–138 |
| #10 | 运营可观测闭环与流量治理（TTFT/TPOT 指标 / 流式取消累积上限 / MDC 关联 / 轮次反馈与导出 / 加权金丝雀 / 影子分叉 / 模型池配额 / 错误码 / 退避抖动） | [maps/effort-10.md](maps/effort-10.md) | T170–T189 | 139–155 |
| #11 | 评估闭环（评估数据集存储 / 反馈导入 / evaluator SPI / eval runner / 结果查询 / 评估事件 / 红队 / perf / demo） | [maps/effort-11.md](maps/effort-11.md) | T190–T202 | 156–167 |
| #12 | 精确响应缓存（缓存 key / 写入 / 流式重放 / TTL / 指标 / 红队 / perf / demo） | [maps/effort-12.md](maps/effort-12.md) | T203–T213 | 168–177 |
| #13 | 配置与公共面治理（配置绑定矩阵 / API 面快照 / 治理红队 / 配置文档矩阵 / 演示） | [maps/effort-13.md](maps/effort-13.md) | T214–T221 | 178–184 |
| #14 | 多实例共享限流（backend SPI / Redis 后端 / starter 装配 / 共享配额容器 / 红队 / perf / 双实例演示 / runbook 共享闸） | [maps/effort-14.md](maps/effort-14.md) | T222–T230 | 185–190（记录随票） |
| #15 | 语义缓存（进程内向量存储 / cosine 阈值 / advisor 位序 / EmbeddingModel 装配 / 否定对红队 / 流式重放 / FAQ 演示 / 成本口径） | [maps/effort-15.md](maps/effort-15.md) | T240–T248 | 191–195（记录随票） |

## 跨 effort 悬留

- effort #1 的 [T10（修 CI OS 缺陷）](tickets/T10-fix-ci-os-specific-defect.md) 为 HITL/环境遗留票——后续 CI 已转绿（见各收口记录），该票状态以 ticket 文件为准。
- effort #15 的 Not-yet-specified 雾区（Redis 向量存储与跨实例共享语义缓存、共享熔断/配额原子化、outbox SCAN 下推、观测 OLAP、skill 语义排序）为 #16+ 候选，见 [maps/effort-15.md](maps/effort-15.md)。
