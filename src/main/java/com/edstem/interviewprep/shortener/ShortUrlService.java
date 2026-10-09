package com.edstem.interviewprep.shortener;

import com.edstem.interviewprep.common.error.GoneException;
import com.edstem.interviewprep.common.error.NotFoundException;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

    static final int MAX_CODE_ATTEMPTS = 5;

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator codeGenerator;

    public ShortUrlService(ShortUrlRepository shortUrlRepository, ShortCodeGenerator codeGenerator) {
        this.shortUrlRepository = shortUrlRepository;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Every call creates a new code, even for a URL that was shortened before (see PR for why).
     * Deliberately not @Transactional: each saveAndFlush is its own transaction, so a unique-constraint
     * violation from a code collision doesn't poison an outer transaction and we can simply retry.
     */
    public ShortUrl shorten(ShortenRequest request) {
        String url = request.url().trim();
        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (shortUrlRepository.existsByCode(code)) {
                continue;
            }
            try {
                return shortUrlRepository.saveAndFlush(new ShortUrl(code, url, request.expiresAt()));
            } catch (DataIntegrityViolationException e) {
                // Another request inserted the same code between our check and insert: try a new one.
            }
        }
        throw new IllegalStateException("Could not generate a unique short code after " + MAX_CODE_ATTEMPTS
                + " attempts");
    }

    /**
     * Resolves a code for a redirect and counts the visit atomically in the database.
     */
    @Transactional
    public String resolveAndCountVisit(String code) {
        ShortUrl shortUrl = findOrThrow(code);
        if (shortUrl.isExpired(Instant.now())) {
            throw new GoneException("Short URL '" + code + "' has expired");
        }
        shortUrlRepository.incrementVisitCount(code);
        return shortUrl.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortUrlStatsResponse stats(String code) {
        return ShortUrlStatsResponse.from(findOrThrow(code));
    }

    private ShortUrl findOrThrow(String code) {
        return shortUrlRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Short URL '" + code + "' not found"));
    }
}
