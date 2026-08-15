# impl-126 — 迁移器防护

**What to build:** 旧构建对新高版本库拒绝运行；已应用脚本事后改动可检出（checksum）。

**Blocked by:** None

**Status:** done

- [x] 版本表 checksum 列（新 DDL + 存量幂等 ALTER，大小写两形态探测）
- [x] 未来版本拒绝 + validateChecksums（NULL 回填/不等拒绝）+ apply 同事务写 checksum
- [x] 测试：V999 拒绝/篡改检出/回填幂等 + 既有四用例不回归——store-jdbc 77 绿（27 docker 门控跳过）
