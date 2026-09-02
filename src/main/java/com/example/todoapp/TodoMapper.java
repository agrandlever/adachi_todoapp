package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {

    List<Todo> search(@Param("keyword") String keyword, @Param("category") String category,
            @Param("order") String order, @Param("completed") Boolean completed,
            @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("limit") Integer limit, @Param("offset") Integer offset,
            @Param("trash") boolean trash);

    int count(@Param("keyword") String keyword, @Param("category") String category,
            @Param("completed") Boolean completed, @Param("trash") boolean trash);

    Todo findById(Long id);

    void insert(Todo todo);

    void update(Todo todo);

    void softDeleteById(Long id);

    void restoreById(Long id);

    void togglePinnedById(Long id);
}
