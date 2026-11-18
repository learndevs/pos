package com.retailpos.server.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void hasVersion7AndRfcVariant() {
        UUID id = UuidV7.next();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void embedsTimestampAndSortsByTime() {
        UUID earlier = UuidV7.next(1_700_000_000_000L);
        UUID later = UuidV7.next(1_700_000_000_001L);
        assertThat(earlier.getMostSignificantBits() >>> 16).isEqualTo(1_700_000_000_000L);
        assertThat(earlier.toString()).isLessThan(later.toString());
    }

    @Test
    void isUnique() {
        assertThat(UuidV7.next()).isNotEqualTo(UuidV7.next());
    }
}
