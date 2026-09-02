package com.example.todoapp.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.BindingResult;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * やること一覧をJSONで返すための入り口です。
 */
@RestController
@RequestMapping("/api")
public class TodoApiController {

    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TodoService todoService;

    public TodoApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/todos")
    public List<TodoDto> getTodos(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // 既存の一覧画面と同じく、desc以外は期限の昇順として扱います。
        if (!"desc".equals(order)) {
            order = "asc";
        }

        // 検索と並び替えは既存サービスへ任せ、返す形だけDTOへ変換します。
        return todoService.search(keyword, category, order, from, to).stream()
                .map(TodoDto::from)
                .toList();
    }

    @GetMapping(value = "/todos.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> downloadCsv(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "showCompleted", defaultValue = "false") boolean showCompleted,
            @RequestParam(name = "trash", defaultValue = "0") int trash) {
        if (!"desc".equals(order)) {
            order = "asc";
        }

        boolean trashMode = trash == 1;
        Boolean completed = showCompleted || trashMode ? null : false;
        List<Todo> todos = todoService.search(keyword, category, order, completed, trashMode);

        // 先頭のBOMは、ExcelなどがUTF-8の日本語だと判断するための目印です。
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, "やること", "メモ", "ジャンル", "優先度", "期限", "状態", "印");

        for (Todo todo : todos) {
            appendCsvRow(csv,
                    todo.getTitle(),
                    todo.getDetail(),
                    todo.getCategory(),
                    priorityLabel(todo.getPriority()),
                    todo.getDueDate() == null ? "" : todo.getDueDate().format(CSV_DATE_FORMAT),
                    completedLabel(todo),
                    Boolean.TRUE.equals(todo.getPinned()) ? "★" : "－");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("todos.csv").build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void appendCsvRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvCell(values[i]));
        }
        csv.append("\r\n");
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;

        // 表計算ソフトが計算式として実行し得る先頭記号には、文字扱いにする印を付けます。
        if (!safeValue.isEmpty() && "=+-@".indexOf(safeValue.charAt(0)) >= 0) {
            safeValue = "'" + safeValue;
        }

        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private String priorityLabel(Integer priority) {
        if (Integer.valueOf(1).equals(priority)) {
            return "高";
        }
        if (Integer.valueOf(2).equals(priority)) {
            return "中";
        }
        if (Integer.valueOf(3).equals(priority)) {
            return "低";
        }
        return "";
    }

    private String completedLabel(Todo todo) {
        if (!Boolean.TRUE.equals(todo.getCompleted())) {
            return "未完了";
        }
        if (todo.getCompletedAt() == null) {
            return "完了";
        }
        return "完了（" + todo.getCompletedAt().format(CSV_DATE_FORMAT) + "）";
    }

    @GetMapping("/todos/{id}")
    public ResponseEntity<?> getTodo(@PathVariable Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createNotFoundProblem(id));
        }

        return ResponseEntity.ok(TodoDto.from(todo));
    }

    @PostMapping("/todos")
    public ResponseEntity<?> createTodo(
            @Valid @RequestBody TodoRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(createBadRequestProblem(bindingResult, httpRequest));
        }

        Todo todo = request.toTodo();
        todoService.create(todo);

        // データベースで採番されたIDを使い、作成日時も入った状態を取り直します。
        Todo createdTodo = todoService.findById(todo.getId());
        URI location = URI.create("/api/todos/" + todo.getId());
        return ResponseEntity.created(location).body(TodoDto.from(createdTodo));
    }

    @PutMapping("/todos/{id}")
    public ResponseEntity<?> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody TodoRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(createBadRequestProblem(bindingResult, httpRequest));
        }

        Todo existingTodo = todoService.findById(id);
        if (existingTodo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createNotFoundProblem(id));
        }

        Todo todo = request.toTodo();
        // アドレスのIDを更新対象として使い、受け取った6項目で1件を入れ替えます。
        todo.setId(id);
        todoService.update(todo);
        Todo updatedTodo = todoService.findById(id);
        return ResponseEntity.ok(TodoDto.from(updatedTodo));
    }

    @DeleteMapping("/todos/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createNotFoundProblem(id));
        }

        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ProblemDetail createBadRequestProblem(
            BindingResult bindingResult,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "入力に誤りがあります");
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", bindingResult.getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()))
                .toList());
        return problem;
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
