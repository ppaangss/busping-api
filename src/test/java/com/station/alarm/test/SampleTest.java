package com.station.alarm.test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SampleTest {

    @Test
    void 덧셈_테스트() {
        int result = 1 + 2;

        assertThat(result).isEqualTo(3);
    }
}

