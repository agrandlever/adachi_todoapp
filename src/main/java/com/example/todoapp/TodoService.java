package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TodoService {

    private final TodoMapper todoMapper;

    // コンストラクタで受け取ることで、TodoServiceがTodoMapperを利用できるようにします。
    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public List<Todo> search(String keyword, String category, String order) {
        return search(keyword, category, order, null, null);
    }

    public List<Todo> search(String keyword, String category, String order,
            Boolean completed) {
        return todoMapper.search(keyword, category, order, completed, null, null, null, null, false);
    }

    public List<Todo> search(String keyword, String category, String order,
            Boolean completed, boolean trash) {
        return todoMapper.search(keyword, category, order, completed, null, null, null, null, trash);
    }

    public List<Todo> search(String keyword, String category, String order,
            Boolean completed, int limit, int offset, boolean trash) {
        return todoMapper.search(keyword, category, order, completed, null, null, limit, offset, trash);
    }

    public List<Todo> search(String keyword, String category, String order,
            LocalDate from, LocalDate to) {
        return todoMapper.search(keyword, category, order, null, from, to, null, null, false);
    }

    public int count(String keyword, String category, Boolean completed, boolean trash) {
        return todoMapper.count(keyword, category, completed, trash);
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public void create(Todo todo) {
        todoMapper.insert(todo);
        log.info("operation=登録 id={}", todo.getId());
    }

    public void update(Todo todo) {
        todoMapper.update(todo);
        log.info("operation=編集 id={}", todo.getId());
    }

    public void delete(Long id) {
        todoMapper.softDeleteById(id);
        log.info("operation=削除 id={}", id);
    }

    public void restore(Long id) {
        todoMapper.restoreById(id);
        log.info("operation=復元 id={}", id);
    }

    public void togglePinned(Long id) {
        todoMapper.togglePinnedById(id);
        log.info("operation=印切替 id={}", id);
    }
}
