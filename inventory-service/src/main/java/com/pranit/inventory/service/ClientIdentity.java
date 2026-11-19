package com.pranit.inventory.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class ClientIdentity {

    /**
     * Hash PII (phone/email/username) so it never appears in Redis keys or MONITOR output.
     */
    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Real client IP. getRemoteAddr() is only correct when
     * server.forward-headers-strategy=native is set AND only your trusted
     * proxy can set X-Forwarded-For. Never parse that header manually.
     */
    public String ip(HttpServletRequest req) {
        String raw = req.getRemoteAddr();
        try {
            InetAddress addr = InetAddress.getByName(raw);
            if (addr instanceof Inet6Address v6) {
                // One user typically controls a whole /64, so bucket by prefix
                return HexFormat.of().formatHex(v6.getAddress(), 0, 8) + "::/64";
            }
        } catch (UnknownHostException ignored) {
        }
        return raw;
    }

    /**
     * Authenticated principal name, or null if anonymous.
     */
    public String user(HttpServletRequest req) {
        Principal p = req.getUserPrincipal();
        return p != null ? p.getName() : null;
    }
}