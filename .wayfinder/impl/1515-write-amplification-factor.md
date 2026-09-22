# impl 1515 — WriteAmplificationFactor LSM 写放大读面（R115 = effort #1914 / spec 1914 / T3029-T3030）

**What**：`WriteAmplificationFactor`（core/cleanup 静态纯函数）——
waf（落盘/逻辑写入比值）+ compactionDebtRatio（压实债占比）+
needsThrottle（WAF ≥ 阈值限速建议）；written≥0/logical≥1/capacity≥1/
阈值≥1 fail-fast。

**Why**：RocksDB/LSM 写放大——盘 IO 涨三倍但业务写入没变（压实
重写同份数据）；WAF 读面分得清「写多」还是「重写多」。与墓碑占比
互补。落轮 grep 复核半衰期被 FactDecayPolicy 占坑换静脉。

**Verify**：`WriteAmplificationFactorTest` 4 用例全绿（WAF 3.0 与
零写入/压实债 0.1/限速判定恰 10 含上/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
