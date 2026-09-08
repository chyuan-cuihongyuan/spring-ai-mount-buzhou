package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 404 §Testing / T699–T700：Merkle 化——根确定性/每叶可验/篡改即假/
 * 奇数叶/空树根；链封印回路（seal→proof→verify）+ 追加不影响旧印。
 */
class AuditMerkleTest {

    private static List<AgentAuditRecord> records(int n) {
        AuditChain chain = new AuditChain("agent", "1.0");
        for (int i = 0; i < n; i++) {
            chain.append("s1", "TOOL_CALL", "tool-" + i, "OK");
        }
        return chain.records();
    }

    @Test
    void shouldProveInclusionForEveryLeaf_whenTreeBuilt() {
        for (int n : new int[] {1, 2, 3, 5, 8}) { // 奇偶混合
            List<AgentAuditRecord> rs = records(n);
            AuditMerkleTree tree = AuditMerkleTree.of(rs);
            assertThat(tree.leafCount()).isEqualTo(n);
            // 确定性：同记录序列同根
            assertThat(AuditMerkleTree.of(rs).rootHex()).isEqualTo(tree.rootHex());
            for (AgentAuditRecord r : rs) {
                Optional<AuditMerkleTree.InclusionProof> proof = tree.proof(r.recordId());
                assertThat(proof).isPresent();
                assertThat(AuditMerkleTree.verify(
                        AuditMerkleTree.leafHash(r), proof.get().steps(), tree.rootHex()))
                        .as("n=" + n + " record=" + r.recordId())
                        .isTrue();
            }
        }
    }

    @Test
    void shouldRejectTamperedLeafOrForeignRoot() {
        List<AgentAuditRecord> rs = records(6);
        AuditMerkleTree tree = AuditMerkleTree.of(rs);
        AgentAuditRecord victim = rs.get(3);
        var proof = tree.proof(victim.recordId()).orElseThrow();

        // 篡改叶（同 recordId 不同内容——摘要变）
        AgentAuditRecord tampered = new AgentAuditRecord(
                victim.recordId(), victim.timestamp() + 1, victim.agentId(),
                victim.agentVersion(), victim.sessionId(), victim.actionType(),
                "被篡改的明细", victim.outcome(), victim.trustLevel(),
                victim.parentRecordId(), victim.prevHash(), victim.signature(),
                victim.keyVersion());
        assertThat(AuditMerkleTree.verify(AuditMerkleTree.leafHash(tampered),
                proof.steps(), tree.rootHex())).isFalse();

        // 换根（另一次生成的树）
        AuditMerkleTree other = AuditMerkleTree.of(records(7));
        assertThat(AuditMerkleTree.verify(AuditMerkleTree.leafHash(victim),
                proof.steps(), other.rootHex())).isFalse();

        // 不在树内的记录：无证明
        assertThat(tree.proof("no-such-record")).isEmpty();
    }

    @Test
    void shouldRootEmptyTreeAtGenesisHash() {
        AuditMerkleTree empty = AuditMerkleTree.of(List.of());
        assertThat(empty.leafCount()).isZero();
        assertThat(empty.rootHex()).hasSize(64); // sha256 hex
        // 与链创世同值：sha256("") —— 空 AuditChain 的首 prevHash 即它
        AuditChain chain = new AuditChain("a", "1.0");
        chain.append("s", "GENESIS", "detail", "OK");
        String genesisPrev = chain.records().get(0).prevHash();
        assertThat(empty.rootHex()).isEqualTo(genesisPrev);
    }

    @Test
    void shouldSealAndVerifyThroughChainRoundTrip() {
        AuditChain chain = new AuditChain("agent", "1.0");
        for (int i = 0; i < 5; i++) {
            chain.append("s1", "TOOL_CALL", "tool-" + i, "OK");
        }
        AuditMerkleSeal seal = chain.sealMerkle();
        assertThat(seal.recordCount()).isEqualTo(5);
        assertThat(chain.merkleSeals()).containsExactly(seal);

        // 回路：链记录 → 叶摘要 + 证明 + 印根 → 验过
        AgentAuditRecord any = chain.records().get(2);
        AuditMerkleTree tree = AuditMerkleTree.of(chain.records());
        assertThat(tree.rootHex()).isEqualTo(seal.rootHex());
        var proof = tree.proof(any.recordId()).orElseThrow();
        assertThat(AuditMerkleTree.verify(AuditMerkleTree.leafHash(any),
                proof.steps(), seal.rootHex())).isTrue();

        // 封印后追加：旧印不变，新印覆盖更多记录
        chain.append("s1", "TOOL_CALL", "tool-5", "OK");
        assertThat(chain.merkleSeals()).containsExactly(seal); // 旧印不动
        AuditMerkleSeal seal2 = chain.sealMerkle();
        assertThat(seal2.recordCount()).isEqualTo(6);
        assertThat(chain.merkleSeals()).containsExactly(seal, seal2);
    }
}
