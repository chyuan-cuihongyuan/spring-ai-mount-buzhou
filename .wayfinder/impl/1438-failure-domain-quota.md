# impl 1438 — FailureDomainQuota 失败域配额（R38 = effort #1837 / spec 1837 / T2875-T2876）

**What**：`FailureDomainQuota`（core/budget 静态纯函数）——admit 三态准入
（域桶先花/借全局保留/拒）+ census 域普查（atCap/borrowing/tightest
并列取首/reserveUtilization -1 哨兵）；负计数/空白域 fail-fast。

**Why**：k8s failure-domain/供应链分域备货思想——失败重试风暴吃光全局预算
连坐健康域 vs 平均分桶无弹性两难；域桶+中央保留让隔离与兜底同时成立。

**Verify**：`FailureDomainQuotaTest` 4 用例全绿（首跑红为并行会话编辑窗口
——BuzhouMemoryAdvisorTest 主测不同步，收尾后绿，跨会话干扰第三次入档）。

**Status**：done（2026-09-16）
