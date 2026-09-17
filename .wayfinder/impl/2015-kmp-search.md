# impl 2015 — Q 会话 R15 KMP 字符串搜索（spec 3014 / T5029–T5030 / R15）

纵切片：KmpSearch（core/metrics）——lps 失配函数 + O(n+m) 扫描 +
可重叠 findAll + 读数面。

- 验证：`mvn -pl buzhou-core test -Dtest='KmpSearchTest'` 全绿。
