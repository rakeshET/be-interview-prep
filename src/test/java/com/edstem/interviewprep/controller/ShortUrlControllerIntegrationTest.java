package com.edstem.interviewprep.controller;

import com.edstem.interviewprep.dto.request.ShortenRequest;
import com.edstem.interviewprep.entity.ShortUrl;
import com.edstem.interviewprep.repository.ShortUrlRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ShortUrlControllerIntegrationTest {

    private static final String LONG_URL = "https://example.com/some/very/long/path?with=query&and=params";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @BeforeEach
    void cleanDatabase() {
        shortUrlRepository.deleteAll();
    }

    @Test
    void shortenReturnsUrlSafeCodeOfAtMost8CharactersAndShortUrl() throws Exception {
        JsonNode created = shorten(LONG_URL, null);

        String code = created.get("code").asText();
        assertThat(code).hasSizeLessThanOrEqualTo(8).matches("[A-Za-z0-9]+");
        assertThat(created.get("shortUrl").asText()).isEqualTo("http://localhost/r/" + code);
        assertThat(created.get("originalUrl").asText()).isEqualTo(LONG_URL);
    }

    @Test
    void visitingTheShortUrlRedirectsAndCountsEveryVisit() throws Exception {
        String code = shorten(LONG_URL, null).get("code").asText();

        mockMvc.perform(get("/r/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", LONG_URL));
        mockMvc.perform(get("/r/" + code)).andExpect(status().isFound());

        mockMvc.perform(get("/api/urls/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value(LONG_URL))
                .andExpect(jsonPath("$.visitCount").value(2))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shorteningTheSameUrlTwiceCreatesTwoIndependentLinks() throws Exception {
        String first = shorten(LONG_URL, null).get("code").asText();
        String second = shorten(LONG_URL, Instant.now().plusSeconds(3600)).get("code").asText();

        assertThat(first).isNotEqualTo(second);
        mockMvc.perform(get("/r/" + first)).andExpect(status().isFound());
        mockMvc.perform(get("/api/urls/" + second + "/stats"))
                .andExpect(jsonPath("$.visitCount").value(0))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a url", "example.com/no-scheme", "ftp://example.com/file", "javascript:alert(1)",
            "https://"})
    void invalidUrlsAreRejectedWith400(String url) throws Exception {
        mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequest(url, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("url"));
    }

    @Test
    void expiryInThePastIsRejected() throws Exception {
        mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ShortenRequest(LONG_URL, Instant.now().minusSeconds(60)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("expiresAt"));
    }

    @Test
    void unknownCodeReturns404ForRedirectAndStats() throws Exception {
        mockMvc.perform(get("/r/nope123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short URL 'nope123' not found"));
        mockMvc.perform(get("/api/urls/nope123/stats")).andExpect(status().isNotFound());
    }

    @Test
    void expiredCodeReturns410AndIsNotCounted() throws Exception {
        shortUrlRepository.save(new ShortUrl("expired1", LONG_URL, Instant.now().minusSeconds(60)));

        mockMvc.perform(get("/r/expired1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410));
        mockMvc.perform(get("/api/urls/expired1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitCount").value(0));
    }

    private JsonNode shorten(String url, Instant expiresAt) throws Exception {
        String response = mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequest(url, expiresAt))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/r/")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
