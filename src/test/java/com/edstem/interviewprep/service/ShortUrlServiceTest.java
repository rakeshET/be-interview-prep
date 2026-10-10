package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.request.ShortenRequest;
import com.edstem.interviewprep.entity.ShortUrl;
import com.edstem.interviewprep.exception.GoneException;
import com.edstem.interviewprep.repository.ShortUrlRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortCodeGenerator codeGenerator;

    @InjectMocks
    private ShortUrlService shortUrlService;

    private final ShortenRequest request = new ShortenRequest("https://example.com", null);

    @Test
    void retriesWithANewCodeWhenTheGeneratedCodeIsTaken() {
        when(codeGenerator.generate()).thenReturn("taken01", "fresh01");
        when(shortUrlRepository.existsByCode("taken01")).thenReturn(true);
        when(shortUrlRepository.existsByCode("fresh01")).thenReturn(false);
        when(shortUrlRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(shortUrlService.shorten(request).getCode()).isEqualTo("fresh01");
    }

    @Test
    void retriesWhenAConcurrentInsertWinsTheRaceForTheSameCode() {
        when(codeGenerator.generate()).thenReturn("raced01", "fresh01");
        when(shortUrlRepository.existsByCode(anyString())).thenReturn(false);
        when(shortUrlRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_short_urls_code"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(shortUrlService.shorten(request).getCode()).isEqualTo("fresh01");
    }

    @Test
    void givesUpAfterMaxAttempts() {
        when(codeGenerator.generate()).thenReturn("taken01");
        when(shortUrlRepository.existsByCode("taken01")).thenReturn(true);

        assertThatThrownBy(() -> shortUrlService.shorten(request)).isInstanceOf(IllegalStateException.class);
        verify(codeGenerator, times(ShortUrlService.MAX_CODE_ATTEMPTS)).generate();
        verify(shortUrlRepository, never()).saveAndFlush(any());
    }

    @Test
    void expiredLinkIsNotCounted() {
        when(shortUrlRepository.findByCode("old0001"))
                .thenReturn(Optional.of(new ShortUrl("old0001", "https://example.com", Instant.now().minusSeconds(1))));

        assertThatThrownBy(() -> shortUrlService.resolveAndCountVisit("old0001")).isInstanceOf(GoneException.class);
        verify(shortUrlRepository, never()).incrementVisitCount(anyString());
    }

    @Test
    void generatedCodesAreUrlSafeAndAtMost8Characters() {
        ShortCodeGenerator generator = new ShortCodeGenerator();
        for (int i = 0; i < 1000; i++) {
            assertThat(generator.generate()).hasSizeLessThanOrEqualTo(8).matches("[A-Za-z0-9]+");
        }
    }
}
