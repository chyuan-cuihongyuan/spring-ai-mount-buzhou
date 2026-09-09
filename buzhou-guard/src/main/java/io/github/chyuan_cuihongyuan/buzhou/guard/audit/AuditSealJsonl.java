package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 审计封印 JSONL 导出（spec 421 / T733，CT log STH 公示节奏 + 418 追加
 * 快照同族）：把链上当前全部封印逐行追加 {sealedAt, recordCount, rootHex,
 * verified}；verified = 以当前全记录建树、树根与印根比对——<b>旧印在链
 * 追加后恒 false 属正常（印后又有记录），只有最新一印期望 true</b>；若
 * 最新印也 false = 链被动过。印有界 32 → 每轮 ≤32 行追加。写失败上抛
 * （导出是显式动作该红）。宿主定时调用（DelayedJobQueue 组合）即 CT 式
 * 公示节奏。
 */
public final class AuditSealJsonl {

    private AuditSealJsonl() {
    }

    /** 追加一轮全封印（返回写入行数）。 */
    public static int appendSeals(AuditChain chain, Path path) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        var seals = chain.merkleSeals();
        if (seals.isEmpty()) {
            return 0;
        }
        String currentRoot = AuditMerkleTree.of(chain.records()).rootHex();
        try (BufferedWriter writer = Files.newBufferedWriter(absolute,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            for (AuditMerkleSeal seal : seals) {
                boolean verified = currentRoot.equals(seal.rootHex());
                writer.write("{\"sealedAt\":" + seal.sealedAtEpochMs()
                        + ",\"recordCount\":" + seal.recordCount()
                        + ",\"rootHex\":\"" + seal.rootHex()
                        + "\",\"verified\":" + verified + "}");
                writer.newLine();
            }
        }
        return seals.size();
    }
}
