# Wayfinder 总索引 — Buzhou（effort #1–#349 全闭合）

> **单一目录**：2026-08-17 起，原按 effort 分裂的 `.wayfinder/`、`.wayfinder2/` … `.wayfinder15/` 十五个目录融合为本目录；2026-09-07 二次融合，其后又按 effort 分裂的 `.wayfinder16/` … `.wayfinder155/`、`.wayfinder200/` … `.wayfinder227/`、`.wayfinder300/` … `.wayfinder349/` 共 217 个目录同样并入（map → `maps/effort-<N>.md`，票 → 全局 `tickets/`）——**勿再新建 `.wayfinderN/`**。
> 每个 effort 一张 map（`maps/effort-<N>.md`），#2–#10 的运行期 tracker 约定存档于 `maps/readme-effort-NN.md`；C 会话进度台账 = `maps/progress-effort-300.md`。
> 票号全局连续 **T1–T690**（T231–T239 跳号未用；#222 的 T579/T580 仅记于 map、票文件未落盘；**下一张 = T691**）；impl 切片文件 **01–184**（#14 起 impl 记录随票，票内编号累计至 372；编号史见 [impl/README.md](impl/README.md)）。
> effort #1–#349 全部闭合（A/B/C 三期自迭代会话收口，C 会话台账见 [maps/progress-effort-300.md](maps/progress-effort-300.md)）；D 会话（400 系，#400–#429 / specs 400–429 / T691–T750 / impl 373–402）已收口。
> **并行会话**：E 会话（500 系 / T751+ / impl403+，分支 e-session-500-series 进行中）；F 会话（600 系 / T851–T950 / impl 453–502 / specs 600–649，2026-09-12 启动）——新号段裁决见各自 map。

**开新 effort**：新建 `maps/effort-350.md` 起续号（`.wayfinder350–399` 号段为 C 会话保留段，新自迭代会话按其台账裁决开 400 系），在下表登记一行；票从 T691 起、impl 从 373 起全局续号。tracker 约定见 [README.md](README.md)。

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
| #16 | 跨实例原子配额扣减 | [maps/effort-16.md](maps/effort-16.md) | T249–T253 | 196–198 |
| #17 | 共享熔断闸 | [maps/effort-17.md](maps/effort-17.md) | T254–T258 | 199–201 |
| #18 | outbox SCAN 读放大消减 | [maps/effort-18.md](maps/effort-18.md) | T259–T263 | 202–203 |
| #19 | 技能目录语义排序 | [maps/effort-19.md](maps/effort-19.md) | T264–T268 | 204–205 |
| #20 | 观测 OLAP JSONL 导出 | [maps/effort-20.md](maps/effort-20.md) | T269–T272 | 206 |
| #21 | LLM-as-judge 评估器 | [maps/effort-21.md](maps/effort-21.md) | T273–T274 | 207 |
| #22 | 计数写路径原子化推广 | [maps/effort-22.md](maps/effort-22.md) | T275–T276 | 208 |
| #23 | 成对对比评估 | [maps/effort-23.md](maps/effort-23.md) | T277–T278 | 209 |
| #24 | 延迟感知备模型排序 | [maps/effort-24.md](maps/effort-24.md) | T279–T280 | 210 |
| #25 | agent 级成本归集 | [maps/effort-25.md](maps/effort-25.md) | T281–T282 | 211 |
| #26 | 前缀稳定注入序 | [maps/effort-26.md](maps/effort-26.md) | T283–T284 | 212 |
| #27 | OLAP 增量导出 | [maps/effort-27.md](maps/effort-27.md) | T285–T286 | 213 |
| #28 | 评估并行执行 | [maps/effort-28.md](maps/effort-28.md) | T287–T288 | 214 |
| #29 | 崩溃自愈 watchdog | [maps/effort-29.md](maps/effort-29.md) | T289–T290 | 215 |
| #30 | 边界机会压缩 | [maps/effort-30.md](maps/effort-30.md) | T291–T292 | 216 |
| #31 | A/B 成对评估 runner | [maps/effort-31.md](maps/effort-31.md) | T293–T294 | 217 |
| #32 | 会话轨迹→数据集回流 | [maps/effort-32.md](maps/effort-32.md) | T295–T296 | 218 |
| #33 | skill_search 语义面 | [maps/effort-33.md](maps/effort-33.md) | T297–T298 | 219 |
| #34 | A/B run 落盘与查询 | [maps/effort-34.md](maps/effort-34.md) | T299–T300 | 220 |
| #35 | 文档治理收口 | [maps/effort-35.md](maps/effort-35.md) | T301–T302 | 221 |
| #36 | A/B run 完成事件面 | [maps/effort-36.md](maps/effort-36.md) | T303–T304 | 222 |
| #37 | A/B run 明细查询 | [maps/effort-37.md](maps/effort-37.md) | T305–T306 | 223 |
| #38 | 活跃 run 注册表 gauge | [maps/effort-38.md](maps/effort-38.md) | T307–T308 | 224 |
| #39 | 键序区间扫描 SPI | [maps/effort-39.md](maps/effort-39.md) | T309–T310 | 225 |
| #40 | outbox due-time 索引 | [maps/effort-40.md](maps/effort-40.md) | T311–T312 | 226 |
| #41 | 评估回归门 | [maps/effort-41.md](maps/effort-41.md) | T313–T314 | 227 |
| #42 | run 对比 diff | [maps/effort-42.md](maps/effort-42.md) | T317–T318 | 228 |
| #43 | 数据集指纹 | [maps/effort-43.md](maps/effort-43.md) | T319–T320 | 229 |
| #44 | 错误签名聚类 | [maps/effort-44.md](maps/effort-44.md) | T321–T322 | 230 |
| #45 | agent 并发 Turn 隔离舱 | [maps/effort-45.md](maps/effort-45.md) | T323–T324 | 231 |
| #46 | 错误签名健康面 | [maps/effort-46.md](maps/effort-46.md) | T327–T328 | 232 |
| #47 | 工具输出 PII 脱敏 | [maps/effort-47.md](maps/effort-47.md) | T331–T332 | 233 |
| #48 | Ragas 系数值评估器 | [maps/effort-48.md](maps/effort-48.md) | T335–T336 | 234 |
| #49 | eval run JSONL 导出 | [maps/effort-49.md](maps/effort-49.md) | T339–T340 | 235 |
| #50 | G-Eval 自定义维度打分 | [maps/effort-50.md](maps/effort-50.md) | T341–T342 | 236 |
| #51 | 语义漂移触发压缩 | [maps/effort-51.md](maps/effort-51.md) | T343–T344 | 237 |
| #52 | 配置体检 doctor | [maps/effort-52.md](maps/effort-52.md) | T347–T348 | 238 |
| #53 | 隔离舱健康面 | [maps/effort-53.md](maps/effort-53.md) | T349–T350 | 239 |
| #54 | A/B run 指纹 | [maps/effort-54.md](maps/effort-54.md) | T353–T354 | 240 |
| #55 | A/B run JSONL 导出 | [maps/effort-55.md](maps/effort-55.md) | T357–T358 | 241 |
| #56 | 摘要折入 trigger 溯源 | [maps/effort-56.md](maps/effort-56.md) | T359–T360 | 242 |
| #57 | outbox due 索引审计 | [maps/effort-57.md](maps/effort-57.md) | T361–T362 | 243 |
| #58 | 会话归档冷层 | [maps/effort-58.md](maps/effort-58.md) | T365–T366 | 244 |
| #59 | Redis 键序区间覆写 | [maps/effort-59.md](maps/effort-59.md) | T367–T368 | 245 |
| #61 | 折入速率指标 | [maps/effort-61.md](maps/effort-61.md) | T371–T372 | 246 |
| #62 | 数据集快照副本 | [maps/effort-62.md](maps/effort-62.md) | T373–T374 | 247 |
| #63 | A/B 胜率门 | [maps/effort-63.md](maps/effort-63.md) | T377–T378 | 248 |
| #64 | 会话归档健康面 | [maps/effort-64.md](maps/effort-64.md) | T381–T382 | 249 |
| #65 | 归档 TTL 治理 | [maps/effort-65.md](maps/effort-65.md) | T385–T386 | 250 |
| #66 | 模型失败签名接线 | [maps/effort-66.md](maps/effort-66.md) | T387–T388 | 251 |
| #67 | webhook 订阅类型过滤 | [maps/effort-67.md](maps/effort-67.md) | T389–T390 | 252 |
| #68 | 用户输入 PII 脱敏 | [maps/effort-68.md](maps/effort-68.md) | T393–T394 | 253 |
| #69 | 配置体检健康段 | [maps/effort-69.md](maps/effort-69.md) | T395–T396 | 254 |
| #70 | 工具调用时长 timer | [maps/effort-70.md](maps/effort-70.md) | T399–T400 | 255 |
| #71 | 观测导出 gzip 面 | [maps/effort-71.md](maps/effort-71.md) | T403–T404 | 256 |
| #72 | 技能目录注入遥测 | [maps/effort-72.md](maps/effort-72.md) | T405–T406 | 257 |
| #73 | 评估 run 时长 timer | [maps/effort-73.md](maps/effort-73.md) | T407–T408 | 258 |
| #74 | 错误签名 JSONL 导出 | [maps/effort-74.md](maps/effort-74.md) | T409–T410 | 259 |
| #75 | 按数据集版本查 run | [maps/effort-75.md](maps/effort-75.md) | T411–T412 | 260 |
| #76 | AB 面按版本查 run | [maps/effort-76.md](maps/effort-76.md) | T413–T414 | 261 |
| #77 | 配置体检跨键规则 | [maps/effort-77.md](maps/effort-77.md) | T415–T416 | 262 |
| #78 | skill_search 遥测 | [maps/effort-78.md](maps/effort-78.md) | T419–T420 | 263 |
| #79 | bulkhead 拒绝计数 | [maps/effort-79.md](maps/effort-79.md) | T423–T424 | 264 |
| #80 | 自定义 PII 规则 | [maps/effort-80.md](maps/effort-80.md) | T427–T428 | 265 |
| #81 | 会话级 gzip 导出 | [maps/effort-81.md](maps/effort-81.md) | T429–T430 | 266 |
| #82 | 归档详情查询 | [maps/effort-82.md](maps/effort-82.md) | T433–T434 | 267 |
| #83 | 错误签名窗口化清零 | [maps/effort-83.md](maps/effort-83.md) | T437–T438 | 268 |
| #84 | 后半程文档收口 | [maps/effort-84.md](maps/effort-84.md) | T439–T440 | 269 |
| #85 | 50 轮自迭代会话收口 | [maps/effort-85.md](maps/effort-85.md) | T441–T442 | 270 |
| #86 | superstep 原子批 | [maps/effort-86.md](maps/effort-86.md) | T443–T446 | 271–272 |
| #87 | spawn 优先级调度 | [maps/effort-87.md](maps/effort-87.md) | T445–T446 | 272 |
| #88 | 虚拟 key 配额 | [maps/effort-88.md](maps/effort-88.md) | T449–T450 | 274 |
| #89 | 租户隔离沙箱 | [maps/effort-89.md](maps/effort-89.md) | T451–T452 | 275 |
| #90 | 提示前缀缓存 | [maps/effort-90.md](maps/effort-90.md) | T453–T454 | 276 |
| #91 | 共享 Redis 语义向量缓存 | [maps/effort-91.md](maps/effort-91.md) | T471–T472 | 275 |
| #92 | 跨实例舱占用聚合 | [maps/effort-92.md](maps/effort-92.md) | T455–T474 | 276–277 |
| #93 | PII yml 声明式规则 | [maps/effort-93.md](maps/effort-93.md) | T475–T476 | 277 |
| #94 | 工具级熔断 | [maps/effort-94.md](maps/effort-94.md) | T477–T478 | 278 |
| #95 | 幂等工具重试 | [maps/effort-95.md](maps/effort-95.md) | T479–T480 | 279 |
| #96 | outbox 积压滞后面 | [maps/effort-96.md](maps/effort-96.md) | T483–T484 | 280 |
| #97 | 模型对冲请求 | [maps/effort-97.md](maps/effort-97.md) | T485–T486 | 281 |
| #98 | 在飞工具调用合并 | [maps/effort-98.md](maps/effort-98.md) | T487–T488 | 282 |
| #99 | 角色工具权限 | [maps/effort-99.md](maps/effort-99.md) | T491–T492 | 283 |
| #100 | 会话隔离检疫 | [maps/effort-100.md](maps/effort-100.md) | T495–T496 | 284 |
| #101 | 自适应并发 | [maps/effort-101.md](maps/effort-101.md) | T499–T500 | 285 |
| #102 | turn 内工具结果 memo | [maps/effort-102.md](maps/effort-102.md) | T501–T502 | 286 |
| #103 | 模型端点离群驱逐 | [maps/effort-103.md](maps/effort-103.md) | T505–T506 | 287 |
| #104 | 多 sink webhook 扇出 | [maps/effort-104.md](maps/effort-104.md) | T507–T508 | 288 |
| #105 | 凭证租约 | [maps/effort-105.md](maps/effort-105.md) | T511–T512 | 289 |
| #106 | 会话优雅排水 | [maps/effort-106.md](maps/effort-106.md) | T513–T514 | 290 |
| #107 | 弹性预算池 | [maps/effort-107.md](maps/effort-107.md) | T515–T516 | 291 |
| #108 | 投递序列号围栏 | [maps/effort-108.md](maps/effort-108.md) | T517–T518 | 292 |
| #109 | 会话特征抽取 | [maps/effort-109.md](maps/effort-109.md) | T519–T520 | 293 |
| #110 | 配置热重载 | [maps/effort-110.md](maps/effort-110.md) | T523–T524 | 294 |
| #111 | tag 基数守卫 | [maps/effort-111.md](maps/effort-111.md) | T457–T458 | 278 |
| #112 | 数据集期望套件 | [maps/effort-112.md](maps/effort-112.md) | T459–T460 | 279 |
| #113 | 观测导出尾采样 | [maps/effort-113.md](maps/effort-113.md) | T461–T462 | 280 |
| #114 | 轮次心跳 | [maps/effort-114.md](maps/effort-114.md) | T463–T464 | 281 |
| #115 | 技能使用统计 | [maps/effort-115.md](maps/effort-115.md) | T465–T466 | 282 |
| #116 | 体检陈旧度 | [maps/effort-116.md](maps/effort-116.md) | T467–T468 | 283 |
| #117 | PII 命中统计 | [maps/effort-117.md](maps/effort-117.md) | T469–T470 | 284 |
| #118 | 导出清单 | [maps/effort-118.md](maps/effort-118.md) | T471–T472 | 285 |
| #119 | key 级预算闸 | [maps/effort-119.md](maps/effort-119.md) | T501–T502 | 286 |
| #120 | 期望门禁接线 | [maps/effort-120.md](maps/effort-120.md) | T503–T504 | 287 |
| #121 | 心跳钩子接线 | [maps/effort-121.md](maps/effort-121.md) | T505–T506 | 288 |
| #122 | 虚拟 key 健康面 | [maps/effort-122.md](maps/effort-122.md) | T507–T508 | 289 |
| #123 | 虚拟 key yml 装配 | [maps/effort-123.md](maps/effort-123.md) | T511–T512 | 290 |
| #124 | 基数守卫装配 | [maps/effort-124.md](maps/effort-124.md) | T513–T514 | 291 |
| #125 | 停滞巡检犬 | [maps/effort-125.md](maps/effort-125.md) | T515–T516 | 292 |
| #126 | 输入侧命中统计接线 | [maps/effort-126.md](maps/effort-126.md) | T517–T518 | 293 |
| #127 | PII 命中报表导出 | [maps/effort-127.md](maps/effort-127.md) | T519–T520 | 294 |
| #128 | 目录渲染缓存 | [maps/effort-128.md](maps/effort-128.md) | T522–T523 | 295 |
| #129 | 技能使用报表导出 | [maps/effort-129.md](maps/effort-129.md) | T524–T525 | 296 |
| #130 | 归档清理键矩阵登记 | [maps/effort-130.md](maps/effort-130.md) | T526–T527 | 297 |
| #131 | 模型成本台账 | [maps/effort-131.md](maps/effort-131.md) | T529–T530 | 298 |
| #132 | 成本台账接线 | [maps/effort-132.md](maps/effort-132.md) | T532–T533 | 299 |
| #133 | 重试预算 | [maps/effort-133.md](maps/effort-133.md) | T535–T536 | 300 |
| #134 | 性质测试轮 | [maps/effort-134.md](maps/effort-134.md) | T537–T538 | 301 |
| #135 | 文件咨询锁 | [maps/effort-135.md](maps/effort-135.md) | T540–T541 | 302 |
| #136 | 归档清理接锁 | [maps/effort-136.md](maps/effort-136.md) | T543–T544 | 303 |
| #137 | 巡检犬接锁 | [maps/effort-137.md](maps/effort-137.md) | T546–T547 | 304 |
| #138 | 成本账单导出 | [maps/effort-138.md](maps/effort-138.md) | T548–T549 | 305 |
| #139 | 模型成本健康面 | [maps/effort-139.md](maps/effort-139.md) | T551–T552 | 306 |
| #140 | 成本健康段装配 | [maps/effort-140.md](maps/effort-140.md) | T554–T555 | 307 |
| #141 | manifestGzip + resetAll | [maps/effort-141.md](maps/effort-141.md) | T556–T557 | 308 |
| #142 | 签名分面 top + 心跳单查 | [maps/effort-142.md](maps/effort-142.md) | T559–T560 | 309 |
| #143 | 门禁宽松档 | [maps/effort-143.md](maps/effort-143.md) | T562–T563 | 310 |
| #144 | starter 全量验证轮 | [maps/effort-144.md](maps/effort-144.md) | T564–T565 | 311 |
| #145 | README 归档轮 | [maps/effort-145.md](maps/effort-145.md) | T566–T567 | 312 |
| #146 | 守卫折入指标 | [maps/effort-146.md](maps/effort-146.md) | T568–T569 | 313 |
| #147 | 预算批量存入 | [maps/effort-147.md](maps/effort-147.md) | T570–T571 | 314 |
| #148 | 性质测试 II | [maps/effort-148.md](maps/effort-148.md) | T572–T573 | 315 |
| #149 | runbook 补段轮 | [maps/effort-149.md](maps/effort-149.md) | T574–T575 | 316 |
| #150 | 期望默认组合 | [maps/effort-150.md](maps/effort-150.md) | T576–T577 | 317 |
| #151 | clearAll + usageAll | [maps/effort-151.md](maps/effort-151.md) | T579–T580 | 318 |
| #152 | 会话雾账本轮 | [maps/effort-152.md](maps/effort-152.md) | T581–T582 | 319 |
| #153 | A/B 撞号台账归一 | [maps/effort-153.md](maps/effort-153.md) | T583–T584 | 320 |
| #154 | 收口预检轮 | [maps/effort-154.md](maps/effort-154.md) | T585–T586 | 321 |
| #155 | 第二期收口 | [maps/effort-155.md](maps/effort-155.md) | T587–T588 | 322 |
| #200 | 工具健康探测 | [maps/effort-200.md](maps/effort-200.md) | T527–T528 | 295 |
| #201 | B 会话文档轮 | [maps/effort-201.md](maps/effort-201.md) | T529–T530 | 296 |
| #202 | 同输入泛洪防护 | [maps/effort-202.md](maps/effort-202.md) | T531–T532 | 297 |
| #203 | 工具结果裁剪装饰器 | [maps/effort-203.md](maps/effort-203.md) | T533–T534 | 298 |
| #204 | 摘要溯源台账 | [maps/effort-204.md](maps/effort-204.md) | T537–T538 | 299 |
| #205 | 工具泳道并发闸 | [maps/effort-205.md](maps/effort-205.md) | T541–T542 | 300 |
| #206 | 工具目录指纹 | [maps/effort-206.md](maps/effort-206.md) | T545–T546 | 301 |
| #207 | 事件载荷出站脱敏 | [maps/effort-207.md](maps/effort-207.md) | T547–T548 | 302 |
| #208 | 空闲会话水位监控 | [maps/effort-208.md](maps/effort-208.md) | T551–T552 | 303 |
| #209 | 上下文余量水位 | [maps/effort-209.md](maps/effort-209.md) | T553–T554 | 304 |
| #210 | 跨轮 TTL 工具缓存 | [maps/effort-210.md](maps/effort-210.md) | T555–T556 | 305 |
| #211 | per-tool 会话配额 | [maps/effort-211.md](maps/effort-211.md) | T557–T558 | 306 |
| #212 | 生效配置指纹 | [maps/effort-212.md](maps/effort-212.md) | T559–T560 | 307 |
| #213 | 影子读探针 | [maps/effort-213.md](maps/effort-213.md) | T561–T562 | 308 |
| #214 | 轮次时延计时 | [maps/effort-214.md](maps/effort-214.md) | T563–T564 | 309 |
| #215 | 导出防篡改清单 | [maps/effort-215.md](maps/effort-215.md) | T565–T566 | 310 |
| #216 | 降级链演练 | [maps/effort-216.md](maps/effort-216.md) | T567–T568 | 311 |
| #217 | 轮内模型调用循环闸 | [maps/effort-217.md](maps/effort-217.md) | T569–T570 | 312 |
| #218 | 平滑加权路由 | [maps/effort-218.md](maps/effort-218.md) | T571–T572 | 313 |
| #219 | 工具目录漂移看门狗 | [maps/effort-219.md](maps/effort-219.md) | T573–T574 | 314 |
| #220 | 事件去重抑制 | [maps/effort-220.md](maps/effort-220.md) | T575–T576 | 315 |
| #221 | 维护模式门 | [maps/effort-221.md](maps/effort-221.md) | T577–T578 | 316 |
| #222 | B 会话文档轮 2 | [maps/effort-222.md](maps/effort-222.md) | T579–T580（票未落盘，见 map） | 317 |
| #223 | 降级链单窗视图 | [maps/effort-223.md](maps/effort-223.md) | T581–T582 | 318 |
| #224 | 事件 schema 检查器 | [maps/effort-224.md](maps/effort-224.md) | T583–T584 | 319 |
| #225 | webhook 族组合 E2E | [maps/effort-225.md](maps/effort-225.md) | T585–T586 | 320 |
| #226 | spec 文档覆盖门 | [maps/effort-226.md](maps/effort-226.md) | T587–T588 | 321 |
| #227 | B 会话 50 轮自迭代收口 | [maps/effort-227.md](maps/effort-227.md) | T589–T590 | 322 |
| #300 | 批内工具合并接线 | [maps/effort-300.md](maps/effort-300.md) | T591–T592 | 323 |
| #301 | 对冲装配面 | [maps/effort-301.md](maps/effort-301.md) | T593–T594 | 324 |
| #302 | 重试预算接线 | [maps/effort-302.md](maps/effort-302.md) | T595–T596 | 325 |
| #303 | 序号围栏跨重启持久纪元 | [maps/effort-303.md](maps/effort-303.md) | T597–T598 | 326 |
| #304 | 事务批补偿 | [maps/effort-304.md](maps/effort-304.md) | T599–T600 | 327 |
| #305 | 工具健康探测装配 | [maps/effort-305.md](maps/effort-305.md) | T601–T602 | 328 |
| #306 | 工具熔断 yml 装配 | [maps/effort-306.md](maps/effort-306.md) | T603–T604 | 329 |
| #307 | 事件 schema yml 声明 | [maps/effort-307.md](maps/effort-307.md) | T605–T606 | 330 |
| #308 | deadline 跨工具传播 | [maps/effort-308.md](maps/effort-308.md) | T607–T608 | 331 |
| #309 | 影子对照明细 JSONL 导出 | [maps/effort-309.md](maps/effort-309.md) | T609–T610 | 332 |
| #310 | 空闲会话后台压缩 | [maps/effort-310.md](maps/effort-310.md) | T611–T612 | 333 |
| #311 | 时间旅行 fork | [maps/effort-311.md](maps/effort-311.md) | T613–T614 | 334 |
| #312 | 健康告警规则 | [maps/effort-312.md](maps/effort-312.md) | T615–T616 | 335 |
| #313 | PII 命中分侧 | [maps/effort-313.md](maps/effort-313.md) | T617–T618 | 336 |
| #314 | 价目快照随单 | [maps/effort-314.md](maps/effort-314.md) | T619–T620 | 337 |
| #315 | 虚拟 key 配额 Redis 共享 | [maps/effort-315.md](maps/effort-315.md) | T621–T622 | 338 |
| #316 | 泳道 Redis 共享 | [maps/effort-316.md](maps/effort-316.md) | T623–T624 | 339 |
| #317 | 导出族 gzip 合流打包 | [maps/effort-317.md](maps/effort-317.md) | T625–T626 | 340 |
| #318 | 会话扰乱预算 | [maps/effort-318.md](maps/effort-318.md) | T627–T628 | 341 |
| #319 | 舱压伸缩建议 | [maps/effort-319.md](maps/effort-319.md) | T629–T630 | 342 |
| #320 | 舱容量热调整 | [maps/effort-320.md](maps/effort-320.md) | T631–T632 | 343 |
| #321 | SLO 错误预算燃尽率 | [maps/effort-321.md](maps/effort-321.md) | T633–T634 | 344 |
| #322 | 工具混沌注入 | [maps/effort-322.md](maps/effort-322.md) | T635–T636 | 345 |
| #323 | 干跑拦截/执行计划 | [maps/effort-323.md](maps/effort-323.md) | T637–T638 | 346 |
| #324 | 工具金丝雀 | [maps/effort-324.md](maps/effort-324.md) | T639–T640 | 347 |
| #325 | 工具紧急停用 | [maps/effort-325.md](maps/effort-325.md) | T641–T642 | 348 |
| #326 | 轮次重复检测 | [maps/effort-326.md](maps/effort-326.md) | T643–T644 | 349 |
| #327 | 工具循环断路器 | [maps/effort-327.md](maps/effort-327.md) | T645–T646 | 350 |
| #328 | 干跑计划 JSONL 导出 | [maps/effort-328.md](maps/effort-328.md) | T647–T648 | 351 |
| #329 | API 快照收口 | [maps/effort-329.md](maps/effort-329.md) | T649–T650 | 352 |
| #330 | 告警静默窗与抑制规则 | [maps/effort-330.md](maps/effort-330.md) | T651–T652 | 353 |
| #331 | 后台任务选主 | [maps/effort-331.md](maps/effort-331.md) | T653–T654 | 354 |
| #332 | 健康三探针分层 | [maps/effort-332.md](maps/effort-332.md) | T655–T656 | 355 |
| #333 | 消息静态信封加密 | [maps/effort-333.md](maps/effort-333.md) | T657–T658 | 356 |
| #334 | 成本归因台账 | [maps/effort-334.md](maps/effort-334.md) | T659–T660 | 357 |
| #335 | 错误预算政策·烧穿自动降级 | [maps/effort-335.md](maps/effort-335.md) | T661–T662 | 358 |
| #336 | 摘要槽信封加密·333 扩散 | [maps/effort-336.md](maps/effort-336.md) | T663–T664 | 359 |
| #337 | 工具上下文行李 | [maps/effort-337.md](maps/effort-337.md) | T665–T666 | 360 |
| #338 | 预算软预警线 | [maps/effort-338.md](maps/effort-338.md) | T667–T668 | 361 |
| #339 | 多模型加权路由装配收尾 | [maps/effort-339.md](maps/effort-339.md) | T669–T670 | 362 |
| #340 | 路由权重热调整 | [maps/effort-340.md](maps/effort-340.md) | T671–T672 | 363 |
| #341 | 选主扩散：归档清理与空闲压缩 | [maps/effort-341.md](maps/effort-341.md) | T673–T674 | 364 |
| #342 | 维护窗口 cordon | [maps/effort-342.md](maps/effort-342.md) | T675–T676 | 365 |
| #343 | 生效配置自描述端点 | [maps/effort-343.md](maps/effort-343.md) | T677–T678 | 366 |
| #344 | 审计链完整性巡检 | [maps/effort-344.md](maps/effort-344.md) | T679–T680 | 367 |
| #345 | 告警面板端点 | [maps/effort-345.md](maps/effort-345.md) | T681–T682 | 368 |
| #346 | 会话面板端点 | [maps/effort-346.md](maps/effort-346.md) | T683–T684 | 369 |
| #347 | 告警注解随发 | [maps/effort-347.md](maps/effort-347.md) | T685–T686 | 370 |
| #348 | 重试预算健康面 | [maps/effort-348.md](maps/effort-348.md) | T687–T688 | 371 |
| #349 | C 会话收官终验 | [maps/effort-349.md](maps/effort-349.md) | T689–T690 | 372 |
| #400 | 密钥扫描护栏 | [maps/effort-400.md](maps/effort-400.md) | T691–T692 | 373 |
| #401 | 提示词注册表 | [maps/effort-401.md](maps/effort-401.md) | T693–T694 | 374 |
| #402 | 结构化输出执法 | [maps/effort-402.md](maps/effort-402.md) | T695–T696 | 375 |
| #403 | 成本预测外推 | [maps/effort-403.md](maps/effort-403.md) | T697–T698 | 376 |
| #404 | 审计 Merkle 根与包含证明 | [maps/effort-404.md](maps/effort-404.md) | T699–T700 | 377 |
| #405 | 健康事件时间线 | [maps/effort-405.md](maps/effort-405.md) | T701–T702 | 378 |
| #406 | 工具退役通告 | [maps/effort-406.md](maps/effort-406.md) | T703–T704 | 379 |
| #407 | 在线采样入评测集 | [maps/effort-407.md](maps/effort-407.md) | T705–T706 | 380 |
| #408 | 预算日历周期 | [maps/effort-408.md](maps/effort-408.md) | T707–T708 | 381 |
| #409 | 工具结果 schema 校验 | [maps/effort-409.md](maps/effort-409.md) | T709–T710 | 382 |
| #410 | 共享事实库 ACL | [maps/effort-410.md](maps/effort-410.md) | T711–T712 | 383 |
| #411 | 泳道优先级原语 | [maps/effort-411.md](maps/effort-411.md) | T713–T714 | 384 |
| #412 | 时间桶预聚合 | [maps/effort-412.md](maps/effort-412.md) | T715–T716 | 385 |
| #413 | 延迟作业原语 | [maps/effort-413.md](maps/effort-413.md) | T717–T718 | 386 |
| #414 | 配置漂移审计 | [maps/effort-414.md](maps/effort-414.md) | T719–T720 | 387 |
| #415 | 会话黏性路由提示 | [maps/effort-415.md](maps/effort-415.md) | T721–T722 | 388 |
| #416 | 时间桶延迟分位数 | [maps/effort-416.md](maps/effort-416.md) | T723–T724 | 389 |
| #417 | 价目热更新 | [maps/effort-417.md](maps/effort-417.md) | T725–T726 | 390 |
| #418 | 秘密命中统计与导出 | [maps/effort-418.md](maps/effort-418.md) | T727–T728 | 391 |
| #419 | 周期预算健康面 | [maps/effort-419.md](maps/effort-419.md) | T729–T730 | 392 |
| #420 | 工具目录 lint | [maps/effort-420.md](maps/effort-420.md) | T731–T732 | 393 |
| #421 | 审计封印导出 | [maps/effort-421.md](maps/effort-421.md) | T733–T734 | 394 |
| #422 | 工具泳道优先级装配 | [maps/effort-422.md](maps/effort-422.md) | T735–T736 | 395 |
| #423 | 错误偏向采样 | [maps/effort-423.md](maps/effort-423.md) | T737–T738 | 396 |
| #424 | 提示词使用统计 | [maps/effort-424.md](maps/effort-424.md) | T739–T740 | 397 |
| #425 | 轮次租户限速 | [maps/effort-425.md](maps/effort-425.md) | T741–T742 | 398 |
| #426 | 模型并发舱 | [maps/effort-426.md](maps/effort-426.md) | T743–T744 | 399 |
| #427 | 非流式错误回调对称化 | [maps/effort-427.md](maps/effort-427.md) | T745–T746 | 400 |
| #428 | Webhook 验签与防重放 | [maps/effort-428.md](maps/effort-428.md) | T747–T748 | 401 |
| #429 | 模型并发舱热更新 | [maps/effort-429.md](maps/effort-429.md) | T749–T750 | 402 |
| #500 | 流式回复 PII 脱敏 | [maps/effort-500.md](maps/effort-500.md) | T751–T752 | 403 |
| #501 | 请求幂等键 | [maps/effort-501.md](maps/effort-501.md) | T753–T754 | 404 |
| #502 | 模型能力注册表与能力门 | [maps/effort-502.md](maps/effort-502.md) | T755–T756 | 405 |
| #503 | 时段路由窗口 | [maps/effort-503.md](maps/effort-503.md) | T757–T758 | 406 |
| #504 | MCP 服务器级聚合熔断 | [maps/effort-504.md](maps/effort-504.md) | T759–T760 | 407 |
| #505 | 在线实验分桶 | [maps/effort-505.md](maps/effort-505.md) | T761–T762 | 408 |
| #506 | 工具入参限幅 | [maps/effort-506.md](maps/effort-506.md) | T763–T764 | 409 |
| #507 | 可逆 PII 代管库 | [maps/effort-507.md](maps/effort-507.md) | T765–T766 | 410 |
| #508 | 成本异常尖峰检测 | [maps/effort-508.md](maps/effort-508.md) | T767–T768 | 411 |
| #509 | 时延 SLO 燃尽 | [maps/effort-509.md](maps/effort-509.md) | T769–T770 | 412 |
| #510 | 会话导出加密 | [maps/effort-510.md](maps/effort-510.md) | T771–T772 | 413 |
| #511 | 归档冷存完整性校验 | [maps/effort-511.md](maps/effort-511.md) | T773–T774 | 414 |
| #512 | 提示词模板严格渲染 | [maps/effort-512.md](maps/effort-512.md) | T775–T776 | 415 |
| #513 | 评估 A/A 抖动检测 | [maps/effort-513.md](maps/effort-513.md) | T777–T778 | 416 |
| #514 | 投递时延分位数 | [maps/effort-514.md](maps/effort-514.md) | T779–T780 | 417 |
| #515 | 内容安全词表过滤 | [maps/effort-515.md](maps/effort-515.md) | T781–T782 | 418 |
| #516 | judge 校准跟踪 | [maps/effort-516.md](maps/effort-516.md) | T783–T784 | 419 |
| #517 | 记忆压缩率分布观测 | [maps/effort-517.md](maps/effort-517.md) | T785–T786 | 420 |
| #518 | 会话导出脱敏 | [maps/effort-518.md](maps/effort-518.md) | T787–T788 | 421 |
| #519 | 工具调用图谱统计 | [maps/effort-519.md](maps/effort-519.md) | T789–T790 | 422 |
| #520 | 评估 run 预算闸 | [maps/effort-520.md](maps/effort-520.md) | T791–T792 | 423 |
| #521 | 事故复盘一键包 | [maps/effort-521.md](maps/effort-521.md) | T793–T794 | 424 |
| #522 | session.opened 事件补齐 | [maps/effort-522.md](maps/effort-522.md) | T795–T796 | 425 |
| #523 | 失败轮快照面 | [maps/effort-523.md](maps/effort-523.md) | T797–T798 | 426 |
| #524 | MCP 建连退避重试 | [maps/effort-524.md](maps/effort-524.md) | T801–802 | 427 |
| #525 | 评估集合成扩增 | [maps/effort-525.md](maps/effort-525.md) | T803–T804 | 428 |
| #526 | 水位告警桥接 | [maps/effort-526.md](maps/effort-526.md) | T805–806 | 429 |
| #527 | 数据集 CSV 互操作 | [maps/effort-527.md](maps/effort-527.md) | T807–808 | 430 |
| #528 | 跨会话泄漏金丝雀 | [maps/effort-528.md](maps/effort-528.md) | T809–T810 | 431 |
| #529 | per-tool 超时预算覆盖 | [maps/effort-529.md](maps/effort-529.md) | T811–812 | 432 |
| #530 | per-model 预算闸 | [maps/effort-530.md](maps/effort-530.md) | T813–814 | 433 |
| #531 | 装配绑定审计修复 | [maps/effort-531.md](maps/effort-531.md) | T815–816 | 434 |
| #533 | webhook 载荷大小上限 | [maps/effort-533.md](maps/effort-533.md) | T819–820 | 435 |
| #534 | 提示词版本行级 diff | [maps/effort-534.md](maps/effort-534.md) | T821–822 | 436 |
| #535 | error 项重试一次 | [maps/effort-535.md](maps/effort-535.md) | T823–824 | 437 |
| #536 | 流式回复秘密扫描 | [maps/effort-536.md](maps/effort-536.md) | T825–826 | 438 |
| #537 | 死信原因分类计数 | [maps/effort-537.md](maps/effort-537.md) | T827–828 | 438 |
| #538 | store fsck 定时巡检 | [maps/effort-538.md](maps/effort-538.md) | T829–830 | 439 |
| #539 | spill 回读审计 | [maps/effort-539.md](maps/effort-539.md) | T831–832 | 440 |
| #540 | 签名双密钥轮换验签 | [maps/effort-540.md](maps/effort-540.md) | T833–834 | 441 |
| #541 | 双 judge 一致率 | [maps/effort-541.md](maps/effort-541.md) | T835–836 | 442 |
| #542 | 死信 JSONL 导出 | [maps/effort-542.md](maps/effort-542.md) | T837–838 | 444 |
| #543 | span 状态分布读数 | [maps/effort-543.md](maps/effort-543.md) | T841–842 | 445 |
| #544 | 评估 run 项耗时分布 | [maps/effort-544.md](maps/effort-544.md) | T843–844 | 446 |
| #545 | 注册表快照导出/导入 | [maps/effort-545.md](maps/effort-545.md) | T847–848 | 447 |
| #546 | 技能正文规模审计 | [maps/effort-546.md](maps/effort-546.md) | T851–852 | 448 |
| #547 | 会话导出校验和 | [maps/effort-547.md](maps/effort-547.md) | T853–854 | 449 |
| #548 | fsck 巡检健康面 | [maps/effort-548.md](maps/effort-548.md) | T855–856 | 450 |
| #549 | guard 装配摘要读数 | [maps/effort-549.md](maps/effort-549.md) | T859–860 | 451 |
| **#600（总图）** | **进行中** — F 会话 600 系 50 轮自迭代（借鉴高价值开源项目；MCP 注解观测面 / 驱逐恐慌阈值 / fork 谱系 / …台账见 map） | [maps/effort-600.md](maps/effort-600.md) | T851–（至 T950） | 453–（至 502） |
| #700 | 能力门决策审计读数 | [maps/effort-700.md](maps/effort-700.md) | T1000–1001 | 600 |
| #701 | 语义缓存权重预算驱逐 | [maps/effort-701.md](maps/effort-701.md) | T1002–1003 | 601 |
| #702 | 断路器变迁事件流读数 | [maps/effort-702.md](maps/effort-702.md) | T1004–1005 | 602 |
| #703 | 健康加权路由抑制原语 | [maps/effort-703.md](maps/effort-703.md) | T1006–1007 | 603 |
| #704 | 提示词角色构成拆解读数 | [maps/effort-704.md](maps/effort-704.md) | T1008–1009 | 604 |
| #705 | Redis 键命名空间碰撞审计 | [maps/effort-705.md](maps/effort-705.md) | T1010–1011 | 605 |
| #706 | MCP 工具目录差异报告 | [maps/effort-706.md](maps/effort-706.md) | T1012–1013 | 606 |
| #707 | spill 双文件配对完整性巡检 | [maps/effort-707.md](maps/effort-707.md) | T1014–1015 | 607 |
| #708 | 评估项结果记忆化 | [maps/effort-708.md](maps/effort-708.md) | T1016–1017 | 608 |
| #709 | 实验到期自动停 | [maps/effort-709.md](maps/effort-709.md) | T1018–1019 | 609 |
| #710 | 全局 holdout 层 | [maps/effort-710.md](maps/effort-710.md) | T1020–1021 | 610 |
| #711 | 消息序列连续性审计 | [maps/effort-711.md](maps/effort-711.md) | T1022–1023 | 611 |
| #712 | span 状态分布读数 | [maps/effort-712.md](maps/effort-712.md) | T1024–1025 | 612 |
| #713 | 数据集标签与过滤 | [maps/effort-713.md](maps/effort-713.md) | T1026–1027 | 613 |
| #714 | 相似度阈值判定器 | [maps/effort-714.md](maps/effort-714.md) | T1028–1029 | 614 |
| #715 | PII 格式保形掩码 | [maps/effort-715.md](maps/effort-715.md) | T1030–1031 | 615 |
| #716 | Todo 陈旧度审计读数 | [maps/effort-716.md](maps/effort-716.md) | T1032–1033 | 616 |

## 跨 effort 悬留

- effort #1 的 [T10（修 CI OS 缺陷）](tickets/T10-fix-ci-os-specific-defect.md) 为 HITL/环境遗留票——后续 CI 已转绿（见各收口记录），该票状态以 ticket 文件为准。
- effort #15 的 Not-yet-specified 雾区（Redis 向量存储与跨实例共享语义缓存、共享熔断/配额原子化、outbox SCAN 下推、观测 OLAP、skill 语义排序）为 #16+ 候选，见 [maps/effort-15.md](maps/effort-15.md)。
