package com.example.todoapp;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoMapper {

    List<Todo> findAll();

    Todo findById(Long id);

    void insert(Todo todo);
}
