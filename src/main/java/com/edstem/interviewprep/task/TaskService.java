package com.edstem.interviewprep.task;

import com.edstem.interviewprep.common.error.NotFoundException;
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
