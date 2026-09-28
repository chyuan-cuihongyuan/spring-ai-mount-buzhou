package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7024：ShamirSecretSharing 合同——门限信息论安全。
 * 全份额/任意门限子集还原；少门限 fail-fast；重复 x；
 * 种子确定性；fail-fast。
 */
class ShamirSecretSharingTest {

    @Test
    void allSharesRoundTrip() {
        for (int secret = 0; secret <= 255; secret += 17) {
            List<ShamirSecretSharing.Share> shares =
                    ShamirSecretSharing.split(secret, 3, 5, new Random(secret));
            assertThat(ShamirSecretSharing.combine(shares, 3)).isEqualTo(secret);
        }
    }

    @Test
    void anyThresholdSubsetRecovers() {
        List<ShamirSecretSharing.Share> shares =
                ShamirSecretSharing.split(202, 3, 5, new Random(7024L));
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                for (int k = j + 1; k < 5; k++) {
                    List<ShamirSecretSharing.Share> subset = new ArrayList<>();
                    subset.add(shares.get(i));
                    subset.add(shares.get(j));
                    subset.add(shares.get(k));
                    assertThat(ShamirSecretSharing.combine(subset, 3))
                            .as("子集 %d,%d,%d", i, j, k).isEqualTo(202);
                }
            }
        }
    }

    @Test
    void belowThresholdFailsFastAndDuplicatesRejected() {
        List<ShamirSecretSharing.Share> shares =
                ShamirSecretSharing.split(99, 3, 5, new Random(7L));
        assertThatThrownBy(() -> ShamirSecretSharing.combine(shares.subList(0, 2), 3))
                .isInstanceOf(IllegalArgumentException.class);
        List<ShamirSecretSharing.Share> duplicated = new ArrayList<>();
        duplicated.add(shares.get(0));
        duplicated.add(new ShamirSecretSharing.Share(shares.get(0).x(), 0));
        duplicated.add(shares.get(1));
        assertThatThrownBy(() -> ShamirSecretSharing.combine(duplicated, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void seededDeterminismAndFailFast() {
        List<ShamirSecretSharing.Share> a = ShamirSecretSharing.split(77, 2, 4, new Random(99L));
        List<ShamirSecretSharing.Share> b = ShamirSecretSharing.split(77, 2, 4, new Random(99L));
        for (int i = 0; i < a.size(); i++) {
            assertThat(b.get(i).x()).isEqualTo(a.get(i).x());
            assertThat(b.get(i).y()).isEqualTo(a.get(i).y());
        }
        assertThatThrownBy(() -> ShamirSecretSharing.split(256, 2, 3, new Random(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShamirSecretSharing.split(1, 1, 3, new Random(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShamirSecretSharing.split(1, 4, 3, new Random(1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
