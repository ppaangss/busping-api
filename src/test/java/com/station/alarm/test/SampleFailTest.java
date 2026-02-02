package com.station.alarm.test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SampleFailTest {

    @Test
    void 덧셈_오류테스트() {
        int result = 1 + 2;

        assertThat(result).isEqualTo(4);
    }
}
