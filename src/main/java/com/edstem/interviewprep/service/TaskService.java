package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.request.TaskRequest;
import com.edstem.interviewprep.dto.response.TaskResponse;
import com.edstem.interviewprep.entity.Task;
import com.edstem.interviewprep.entity.TaskStatus;
import com.edstem.interviewprep.exception.NotFoundException;
import com.edstem.interviewprep.repository.TaskRepository;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private static final Sort DEFAULT_SORT = Sort.by("id");

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        TaskStatus status = request.status() != null ? request.status() : TaskStatus.TODO;
        Task task = new Task(request.title(), request.description(), status, request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null
                ? taskRepository.findAll(DEFAULT_SORT)
                : taskRepository.findByStatus(status, DEFAULT_SORT);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    public TaskResponse get(Long id) {
        return TaskResponse.from(findOrThrow(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findOrThrow(id);
        TaskStatus status = request.status() != null ? request.status() : task.getStatus();
        task.update(request.title(), request.description(), status, request.dueDate());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(findOrThrow(id));
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
    }
}
