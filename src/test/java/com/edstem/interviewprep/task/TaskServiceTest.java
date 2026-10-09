package com.edstem.interviewprep.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.common.error.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createDefaultsStatusToTodo() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(new TaskRequest("Title", null, null, null));

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void updateKeepsCurrentStatusWhenNoneGiven() {
        Task existing = new Task("Old", null, TaskStatus.IN_PROGRESS, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        TaskResponse response = taskService.update(1L, new TaskRequest("New", "desc", null, null));

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void deleteOfUnknownTaskThrowsNotFound() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(42L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task with id 42 not found");
        verify(taskRepository, never()).delete(any());
    }
}
