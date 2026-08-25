package com.example.todoapp;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TodoService {

    private final TodoMapper todoMapper;

    // コンストラクタで受け取ることで、TodoServiceがTodoMapperを利用できるようにします。
    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public List<Todo> search(String keyword, String category, String order) {
        return todoMapper.search(keyword, category, order);
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public void create(Todo todo) {
        todoMapper.insert(todo);
    }

    public void update(Todo todo) {
        todoMapper.update(todo);
    }

    public void delete(Long id) {
        todoMapper.deleteById(id);
    }
}
