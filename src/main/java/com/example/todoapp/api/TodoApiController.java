package com.example.todoapp.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.todoapp.TodoService;

/**
 * やること一覧をJSONで返すための入り口です。
 */
@RestController
@RequestMapping("/api/todos")
public class TodoApiController {

    private final TodoService todoService;

    public TodoApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<TodoDto> getTodos(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order) {
        // 既存の一覧画面と同じく、desc以外は期限の昇順として扱います。
        if (!"desc".equals(order)) {
            order = "asc";
        }

        // 検索と並び替えは既存サービスへ任せ、返す形だけDTOへ変換します。
        return todoService.search(keyword, category, order).stream()
                .map(TodoDto::from)
                .toList();
    }
}
