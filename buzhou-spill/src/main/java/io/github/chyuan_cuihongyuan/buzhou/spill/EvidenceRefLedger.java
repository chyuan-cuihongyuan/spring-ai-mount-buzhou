package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * spill 证据引用账本（spec 26 / T105 / impl-80）：evidence uri → 引用会话集合的
 * 持久化登记，支撑 fork「引用计数共享」语义——源会话删除时被 fork 引用的证据保留
 * （最后引用者关闭），悬垂读路径容错（EVIDENCE_GONE）。
 *
 * <p><b>持久化</b>：spill 根 {@code .evidence-refs.json}（每次变更原子重写；fork 场景
 * 写频 = fork 次数，非热路径）。损坏/缺失 → 空账本重建（lenient——重建后旧行为：
 * 无引用登记即随源会话物理删）。
 *
 * <p><b>语义</b>：证据创建时属主会话隐式入引用集；fork 时新会话入集；
 * 会话级联删除时该会话从所有 uri 的引用集摘除，引用集清空的 uri 物理删（延迟到最后
 * 引用者关闭）。属主会话目录被删但引用未清空 → 文件保留、目录保留（至最后一个引用者）。
 */
final class EvidenceRefLedger {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String LEDGER_FILE = ".evidence-refs.json";
    private static final System.Logger LOGGER = System.getLogger(EvidenceRefLedger.class.getName());

    private final Path file;
    /** uri → 引用会话集合（TreeSet 稳定序列化；并发由方法级 synchronized 串行——写频低）。 */
    private final Map<String, Set<String>> refs = new ConcurrentHashMap<>();

    EvidenceRefLedger(Path spillRoot) {
        this.file = spillRoot.resolve(LEDGER_FILE);
        load();
    }

    /** 登记引用（evidence 属主创建时 / fork 拷贝时）。文件不存在时静默跳过（悬垂历史）。 */
    synchronized void acquire(String uri, String referrerSessionId) {
        refs.computeIfAbsent(uri, k -> new TreeSet<>()).add(referrerSessionId);
        save();
    }

    /** 摘除一个引用；返回引用集是否因此清空（清空 = 物理删除时机）。 */
    synchronized boolean release(String uri, String referrerSessionId) {
        Set<String> set = refs.get(uri);
        if (set == null) {
            return false;
        }
        set.remove(referrerSessionId);
        if (set.isEmpty()) {
            refs.remove(uri);
            save();
            return true;
        }
        save();
        return false;
    }

    /** 摘除某会话持有的全部引用；返回引用集清空（应物理删除）的 uri 列表。 */
    synchronized List<String> releaseAllFor(String referrerSessionId) {
        java.util.List<String> drained = new java.util.ArrayList<>();
        for (Map.Entry<String, Set<String>> e : refs.entrySet()) {
            if (e.getValue().remove(referrerSessionId) && e.getValue().isEmpty()) {
                drained.add(e.getKey());
            }
        }
        drained.forEach(refs::remove);
        if (!drained.isEmpty()) {
            save();
        }
        return drained;
    }

    /** 当前引用集合（空集 = 无引用登记，随属主物理删）。 */
    Set<String> referrers(String uri) {
        Set<String> set = refs.get(uri);
        return set == null ? Set.of() : Set.copyOf(set);
    }

    /** 摘除账本条目（单证据显式删除路径）。 */
    synchronized void remove(String uri) {
        if (refs.remove(uri) != null) {
            save();
        }
    }

    int size() {
        return refs.size();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            java.util.LinkedHashMap<String, TreeSet<String>> loaded = MAPPER.readValue(
                    Files.readString(file),
                    new TypeReference<java.util.LinkedHashMap<String, TreeSet<String>>>() {
                    });
            refs.putAll(loaded);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "证据引用账本损坏，按空账本重建（" + e.getMessage() + "）");
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, MAPPER.writeValueAsString(refs));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "证据引用账本写入失败（引用登记可能丢失）：" + e.getMessage());
        }
    }
}
