package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审计 Merkle 树（spec 404 / T699，Certificate Transparency 借鉴——发布根、
 * 按需出证明）：不可变快照树。叶摘要 = sha256(JCS canonical unsignedMap)
 * ——与链 prev_hash 同摘要基（同一记录两种证明路径互证）；层内奇数复制
 * 末叶（Bitcoin 式）；空树根 = sha256("")（与链创世同值）。
 */
public final class AuditMerkleTree {

    /** 包含证明一步：兄弟哈希 + 方向（true = 兄弟在右）。 */
    public record ProofStep(String siblingHex, boolean siblingOnRight) {
    }

    /** 包含证明（recordId 定位 + 路径 + 根）。 */
    public record InclusionProof(String recordId, int leafIndex,
            List<ProofStep> steps, String rootHex) {

        public InclusionProof {
            steps = steps == null ? List.of() : List.copyOf(steps);
        }
    }

    private final List<AgentAuditRecord> records;
    private final List<String> leafHexes;
    private final String root;

    private AuditMerkleTree(List<AgentAuditRecord> records) {
        this.records = List.copyOf(records);
        this.leafHexes = new ArrayList<>(this.records.size());
        for (AgentAuditRecord r : this.records) {
            leafHexes.add(leafHash(r));
        }
        this.root = computeRoot(new ArrayList<>(leafHexes));
    }

    /** 对记录快照建树（records 顺序即叶序）。 */
    public static AuditMerkleTree of(List<AgentAuditRecord> records) {
        return new AuditMerkleTree(records == null ? List.of() : records);
    }

    /** 叶摘要（与链链接同基——公开口径供第三方离线复算）。 */
    public static String leafHash(AgentAuditRecord record) {
        return sha256Hex(Jcs.canonicalize(record.unsignedMap()));
    }

    /** 根（hex）。 */
    public String rootHex() {
        return root;
    }

    /** 叶数。 */
    public int leafCount() {
        return leafHexes.size();
    }

    /** 按记录 ID 出包含证明；不在树内空。 */
    public Optional<InclusionProof> proof(String recordId) {
        int index = -1;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).recordId().equals(recordId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return Optional.empty();
        }
        List<ProofStep> steps = new ArrayList<>();
        List<String> level = new ArrayList<>(leafHexes);
        int pos = index;
        while (level.size() > 1) {
            if (level.size() % 2 == 1) {
                level.add(level.get(level.size() - 1)); // Bitcoin 式复制末叶
            }
            int sibling = pos % 2 == 0 ? pos + 1 : pos - 1;
            steps.add(new ProofStep(level.get(sibling), pos % 2 == 0));
            List<String> next = new ArrayList<>(level.size() / 2);
            for (int i = 0; i < level.size(); i += 2) {
                next.add(hashPair(level.get(i), level.get(i + 1)));
            }
            level = next;
            pos /= 2;
        }
        return Optional.of(new InclusionProof(recordId, index, steps, root));
    }

    /** 第三方验证：叶摘要 + 证明 + 已发布根 → 是否包含（零全链）。 */
    public static boolean verify(String leafHex, List<ProofStep> steps, String rootHex) {
        if (leafHex == null || rootHex == null) {
            return false;
        }
        String current = leafHex;
        for (ProofStep step : steps) {
            current = step.siblingOnRight()
                    ? hashPair(current, step.siblingHex())
                    : hashPair(step.siblingHex(), current);
        }
        return current.equalsIgnoreCase(rootHex);
    }

    /** 叶序（面板/导出面：recordId → 叶摘要）。 */
    public Map<String, String> leafMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < records.size(); i++) {
            map.put(records.get(i).recordId(), leafHexes.get(i));
        }
        return map;
    }

    private static String computeRoot(List<String> level) {
        if (level.isEmpty()) {
            return sha256Hex("");
        }
        while (level.size() > 1) {
            if (level.size() % 2 == 1) {
                level.add(level.get(level.size() - 1));
            }
            List<String> next = new ArrayList<>(level.size() / 2);
            for (int i = 0; i < level.size(); i += 2) {
                next.add(hashPair(level.get(i), level.get(i + 1)));
            }
            level = next;
        }
        return level.get(0);
    }

    private static String hashPair(String left, String right) {
        return sha256Hex(left + right);
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
