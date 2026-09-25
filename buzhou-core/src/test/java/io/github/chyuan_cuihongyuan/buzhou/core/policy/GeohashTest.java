package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6021：Geohash 合同——经纬二分交织 Base32 网格编码。
 * 已知锚（null island 7zzz…）；互逆性；包围盒收缩随精度；
 * 前缀共享邻近性；fail-fast。
 */
class GeohashTest {

    @Test
    void knownAnchors() {
        assertThat(Geohash.encode(42.605, -5.603, 5)).isEqualTo("ezs42");
        assertThat(Geohash.encode(0.0, 0.0, 1)).isEqualTo("s");
        assertThat(Geohash.encode(0.0, 0.0, 8)).isEqualTo("s0000000");
        assertThat(Geohash.decode("ezs42").latMin()).isLessThanOrEqualTo(42.605);
        assertThat(Geohash.decode("ezs42").latMax()).isGreaterThanOrEqualTo(42.605);
        assertThat(Geohash.decode("ezs42").lonMin()).isLessThanOrEqualTo(-5.603);
        assertThat(Geohash.decode("ezs42").lonMax()).isGreaterThanOrEqualTo(-5.603);
    }

    @Test
    void decodeEncodeAreInverse() {
        Random rng = new Random(6021L);
        for (int i = 0; i < 200; i++) {
            double lat = rng.nextDouble() * 180 - 90;
            double lon = rng.nextDouble() * 360 - 180;
            int precision = 1 + rng.nextInt(9);
            String hash = Geohash.encode(lat, lon, precision);
            Geohash.Box box = Geohash.decode(hash);
            assertThat(box.latMin()).as("lat in box").isLessThanOrEqualTo(lat);
            assertThat(box.latMax()).isGreaterThanOrEqualTo(lat);
            assertThat(box.lonMin()).isLessThanOrEqualTo(lon);
            assertThat(box.lonMax()).isGreaterThanOrEqualTo(lon);
            assertThat(Geohash.encode(box.centerLatitude(), box.centerLongitude(), precision))
                    .as("中心再编码同串").isEqualTo(hash);
        }
    }

    @Test
    void bboxShrinksWithPrecision() {
        Geohash.Box coarse = Geohash.decode(Geohash.encode(31.2304, 121.4737, 1));
        Geohash.Box fine = Geohash.decode(Geohash.encode(31.2304, 121.4737, 6));
        assertThat(fine.latMax() - fine.latMin())
                .isLessThan(coarse.latMax() - coarse.latMin());
        assertThat(fine.lonMax() - fine.lonMin())
                .isLessThan(coarse.lonMax() - coarse.lonMin());
        assertThat(coarse.latMax() - coarse.latMin()).isLessThanOrEqualTo(45.0);
        assertThat(coarse.lonMax() - coarse.lonMin()).isLessThanOrEqualTo(45.0);
    }

    @Test
    void sharedPrefixImpliesProximity() {
        String a = Geohash.encode(39.9042, 116.4074, 6);
        String b = Geohash.encode(39.9043, 116.4075, 6);
        assertThat(b).startsWith(a.substring(0, 5));
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> Geohash.encode(91, 0, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geohash.encode(0, 181, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geohash.encode(0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geohash.encode(0, 0, 13)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geohash.decode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geohash.decode("ez!42")).isInstanceOf(IllegalArgumentException.class);
    }
}
