# impl 554 — RedisValueSizeAudit（effort #801）

## 切片

- `buzhou-store-redis/src/main/java/.../store/redis/RedisValueSizeAudit.java` — 纯函数；familyOf 前缀判定（buzhou: 前缀剥离、semvec 二级段、九族+other）；audit(Map<String,Long>, warnBytes)：WARN≥1×/CRIT≥2×、Top32 降序、族聚合（bytes/keys/overThreshold）、hintFor 九族人话提示；脏样本（null key/value、负值）跳过；TOP_LIMIT=32。
- `buzhou-store-redis/src/test/java/.../store/redis/RedisValueSizeAuditTest.java` — 5 例。

## 口径

- 采样归调用方（SCAN+STRLEN/LLEN/HLEN 折算策略不进本类——确定性职责切分）。
- 低于阈值的键不进 top 但计入族聚合（族级膨胀可见——msg 族 1000 个 9KB 键同样是问题）。
- sampledKeys=输入 map 大小（含脏占位——输入即事实）；sampledBytes 只计有效样本。

## 验证

mvn -pl buzhou-store-redis -am test -Dtest='RedisValueSizeAuditTest' → 5/5 绿；快照再生 1 新公共类型。
