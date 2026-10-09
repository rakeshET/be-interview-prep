package com.edstem.interviewprep.task;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Body for create and update. {@code status} is optional: it defaults to TODO on create
 * and keeps the current value on update.
 */
public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        TaskStatus status,

        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate) {
}
