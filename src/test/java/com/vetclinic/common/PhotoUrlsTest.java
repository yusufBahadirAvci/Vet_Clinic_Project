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
        assertThat(PhotoUrls.isAllowed(url)).isFalse();
    }

    @Test
    void acceptsHttpsUrls() {
        assertThat(PhotoUrls.isAllowed("https://example.com/rex.png")).isTrue();
        assertThat(PhotoUrls.isAllowed("HTTPS://example.com/rex.png")).isTrue();
    }

    @Test
    void acceptsSiteRelativeImagePaths() {
        assertThat(PhotoUrls.isAllowed("/images/pets/rex.png")).isTrue();
    }
}
