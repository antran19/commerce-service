package com.nexus.commerce.application.usecase;

import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Where Stripe sends the buyer after paying or cancelling. The caller supplies these URLs, so
// without a check anyone could send a buyer, straight after a REAL payment page, to a look-alike
// site of their own (phishing). Only addresses on the front end's own origin(s) are accepted.
//
// An origin is scheme + host + port. The comparison is on the PARSED address, not on text prefix:
// "http://localhost:5173.evil.com" and "http://localhost:5173@evil.com" both start with an allowed
// origin as text, and both are rejected.
public class RedirectUrlPolicy {

    // Stripe's placeholder for the session id; it is not valid in a URL, so swap it before parsing.
    private static final String SESSION_PLACEHOLDER = "{CHECKOUT_SESSION_ID}";

    private final Set<String> allowedOrigins;

    public RedirectUrlPolicy(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .map(RedirectUrlPolicy::originOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    // No origin configured means nothing is allowed: failing closed is safer than letting every URL in.
    public void requireAllowed(String field, String url) {
        String origin = url == null ? null : originOf(url.replace(SESSION_PLACEHOLDER, "x"));
        if (origin == null || !allowedOrigins.contains(origin)) {
            throw new ValidationException(List.of(new FieldError(field,
                    "must be an address on the website itself (allowed origins: " + String.join(", ", allowedOrigins) + ")")));
        }
    }

    // scheme://host[:port] in lower case with default ports dropped, or null when the address is
    // unusable (not http/https, no host, has "user@" in it, cannot be parsed).
    static String originOf(String address) {
        try {
            URI uri = new URI(address.trim());
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                return null;
            }
            if (uri.getUserInfo() != null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            boolean defaultPort = port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
            return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
