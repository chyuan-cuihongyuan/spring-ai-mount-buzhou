# impl 2207 — T 会话 T7 Aho-Corasick 自动机（spec 6006 / T6213–T6214 / T7）

纵切片：AhoCorasick（core/metrics）——Trie+BFS 失配链单次
扫描多模式全命中 + canonical 序（起始升序+模式文本字典序，
重复模式并列——注册序在重复注册下折叠，改字典序并列序）。

- 验证：`mvn -pl buzhou-core test -Dtest='AhoCorasickTest'` 全绿（MVN_EXIT=0）。
