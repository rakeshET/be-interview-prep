package com.edstem.interviewprep.shortener;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Random Base62 codes: URL-safe without encoding, and 62^7 ≈ 3.5 trillion combinations,
 * so collisions are rare and handled by a retry in {@link ShortUrlService}.
 */
@Component
public class ShortCodeGenerator {

    static final int CODE_LENGTH = 7;
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
