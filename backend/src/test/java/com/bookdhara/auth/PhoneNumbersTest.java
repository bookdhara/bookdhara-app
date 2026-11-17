package com.bookdhara.auth;

import com.bookdhara.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "+919876543210", "919876543210", "09876543210", "98765 43210", "+91-98765-43210"})
    void normalizesCommonFormats(String input) {
        assertThat(PhoneNumbers.normalize(input)).isEqualTo("+919876543210");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "5876543210", "98765432101", "abcdefghij", ""})
    void rejectsInvalidNumbers(String input) {
        assertThatThrownBy(() -> PhoneNumbers.normalize(input)).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> PhoneNumbers.normalize(null)).isInstanceOf(ApiException.class);
    }
}
