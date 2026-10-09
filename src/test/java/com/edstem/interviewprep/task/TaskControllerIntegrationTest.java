package com.edstem.interviewprep.task;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    void createReturns201WithDefaultsAndLocation() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Write tests", "cover the API", null, LocalDate.now().plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/tasks/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void invalidInputReturns400WithAMessagePerField() throws Exception {
        String body = """
                {"title": "", "dueDate": "%s"}
                """.formatted(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/tasks"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("title", "dueDate")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='dueDate')].message")
                        .value("Due date cannot be in the past"));
    }

    @Test
    void titleLongerThan100CharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content(json("x".repeat(101), null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Title must be at most 100 characters"));
    }

    @Test
    void unknownStatusValueInBodyIsAFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"t\", \"status\": \"BLOCKED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    void malformedJsonReturns400InTheSameFormat() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void unsupportedContentTypeReturns415NotA500() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.TEXT_PLAIN).content("title"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.path").value("/api/tasks"));
    }

    @Test
    void unknownTaskReturns404ForGetUpdateAndDelete() throws Exception {
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task with id 999 not found"));
        mockMvc.perform(put("/api/tasks/999").contentType(MediaType.APPLICATION_JSON)
                        .content(json("t", null, null, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullLifecycleAndStatusFilter() throws Exception {
        long first = createTask("First", TaskStatus.TODO);
        createTask("Second", TaskStatus.DONE);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Second"));

        mockMvc.perform(put("/api/tasks/" + first).contentType(MediaType.APPLICATION_JSON)
                        .content(json("First (edited)", "now in progress", TaskStatus.IN_PROGRESS, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First (edited)"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/tasks").param("status", "IN_PROGRESS"))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/tasks/" + first)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tasks/" + first)).andExpect(status().isNotFound());
    }

    @Test
    void invalidStatusFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "BLOCKED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    private long createTask(String title, TaskStatus status) throws Exception {
        String response = mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content(json(title, null, status, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private String json(String title, String description, TaskStatus status, LocalDate dueDate) throws Exception {
        return objectMapper.writeValueAsString(new TaskRequest(title, description, status, dueDate));
    }
}
