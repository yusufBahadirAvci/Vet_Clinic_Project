package com.vetclinic.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhotoUrlsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com/rex.png",
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            "data:image/png;base64,AAAA",
            "//evil.example/rex.png",
            "/images/../secret.png",
            "/other/rex.png",
            "rex.png",
            "https://user:pw@example.com/rex.png",
            "https:///rex.png",
            "https://example.com/a b.png",
            "/images/\\evil",
            "  "})
    @NullAndEmptySource
    void rejectsUnsafeUrls(String url) {
        assertThat(PhotoUrls.safeOrNull(url)).isNull();
    }

    @Test
    void acceptsHttpsUrls() {
        assertThat(PhotoUrls.safeOrNull("https://example.com/rex.png")).isEqualTo("https://example.com/rex.png");
        assertThat(PhotoUrls.safeOrNull("HTTPS://example.com/rex.png")).isEqualTo("HTTPS://example.com/rex.png");
    }

    @Test
    void acceptsSiteRelativeImagePaths() {
        assertThat(PhotoUrls.safeOrNull("/images/pets/rex.png")).isEqualTo("/images/pets/rex.png");
    }
}
