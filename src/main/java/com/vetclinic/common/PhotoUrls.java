package com.vetclinic.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Decides which stored photo URLs the owner portal may put into an {@code <img src>}. */
public final class PhotoUrls {

    private static final String LOCAL_PREFIX = "/images/";

    private PhotoUrls() {
    }

    /** True if the URL is an https:// address with a host and no user-info, or a site-relative path under /images/. */
    public static boolean isAllowed(String url) {
        if (url == null || url.isBlank() || url.chars().anyMatch(c -> c <= ' ' || c == '\\' || c == 127)) {
            return false;
        }
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null) {
                boolean local = url.startsWith(LOCAL_PREFIX) && uri.getHost() == null && !url.contains("..");
                return local;
            }
            boolean https = uri.getScheme().toLowerCase(Locale.ROOT).equals("https")
                    && uri.getHost() != null && uri.getUserInfo() == null;
            return https;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
