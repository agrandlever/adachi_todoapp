package com.example.todoapp.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.todoapp.Todo;
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getTodo(@PathVariable Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createNotFoundProblem(id));
        }

        return ResponseEntity.ok(TodoDto.from(todo));
    }

    @PostMapping
    public ResponseEntity<TodoDto> createTodo(@RequestBody Todo todo) {
        todoService.create(todo);

        // データベースで採番されたIDを使い、作成日時も入った状態を取り直します。
        Todo createdTodo = todoService.findById(todo.getId());
        URI location = URI.create("/api/todos/" + todo.getId());
        return ResponseEntity.created(location).body(TodoDto.from(createdTodo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTodo(@PathVariable Long id, @RequestBody Todo todo) {
        Todo existingTodo = todoService.findById(id);
        if (existingTodo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createNotFoundProblem(id));
        }

        // アドレスのIDを更新対象として使い、受け取った6項目で1件を入れ替えます。
        todo.setId(id);
        todoService.update(todo);
        Todo updatedTodo = todoService.findById(id);
        return ResponseEntity.ok(TodoDto.from(updatedTodo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createNotFoundProblem(id));
        }

        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ProblemDetail createNotFoundProblem(Long id) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "ID " + id + " のやることは見つかりませんでした。");
        problem.setType(URI.create("about:blank"));
        problem.setTitle("やることが見つかりません");
        problem.setInstance(URI.create("/api/todos/" + id));
        return problem;
    }
}
