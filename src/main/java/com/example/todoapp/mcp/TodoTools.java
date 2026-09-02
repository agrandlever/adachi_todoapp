package com.example.todoapp.mcp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;
import com.example.todoapp.api.HolidayClient;
import com.example.todoapp.api.TodoDto;

@Component
public class TodoTools {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "デザイン", "マーケティング", "プログラミング", "資格", "就職活動");

    private final TodoService todoService;
    private final HolidayClient holidayClient;

    public TodoTools(TodoService todoService, HolidayClient holidayClient) {
        this.todoService = todoService;
        this.holidayClient = holidayClient;
    }

    @McpTool(name = "list_todos", description = "やることの一覧を返す（期間・ジャンルで絞れる）")
    public List<TodoDto> listTodos(
            @McpToolParam(required = false) String keyword,
            @McpToolParam(required = false) String category,
            @McpToolParam(required = false) String from,
            @McpToolParam(required = false) String to) {
        // 日付が省略された場合はnullを渡し、期間による絞り込みを行いません。
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);

        return todoService.search(keyword, category, "asc", fromDate, toDate).stream()
                .map(TodoDto::from)
                .toList();
    }

    @McpTool(name = "summarize_week", description = "期間内のやることを数えて、件数・ジャンルごとの内訳・期限切れ件数を要約して返す（一覧は返さない）")
    public TodoSummary summarizeWeek(
            @McpToolParam(description = "期間の始まり（yyyy-MM-dd）", required = false) String from,
            @McpToolParam(description = "期間の終わり（yyyy-MM-dd）", required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);
        List<Todo> todos = todoService.search(null, null, "asc", fromDate, toDate);

        // 同じジャンルをまとめて数え、ジャンル名順に並べることで結果を読みやすくします。
        Map<String, Long> categoryCounts = new TreeMap<>();
        for (Todo todo : todos) {
            categoryCounts.merge(todo.getCategory(), 1L, Long::sum);
        }

        LocalDate today = LocalDate.now();
        long overdueCount = todos.stream()
                // 画面と同じく「今日より前の期限で、まだ完了していないもの」を期限切れとします。
                .filter(todo -> todo.getDueDate() != null
                        && todo.getDueDate().isBefore(today)
                        && Boolean.FALSE.equals(todo.getCompleted()))
                .count();

        return new TodoSummary(todos.size(), categoryCounts, overdueCount);
    }

    @McpTool(name = "get_todo", description = "やることを1件返す")
    public TodoDto getTodo(@McpToolParam(required = true) Long id) {
        Todo todo = todoService.findById(id);
        return todo == null ? null : TodoDto.from(todo);
    }

    @McpTool(name = "add_todo", description = "やることを1件足す")
    public TodoDto addTodo(
            @McpToolParam(required = true) String title,
            @McpToolParam(required = false) String detail,
            @McpToolParam(required = true) String category,
            @McpToolParam(required = true) Integer priority,
            @McpToolParam(required = false) String dueDate) {
        validateCategory(category);

        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setDetail(detail);
        todo.setCategory(category);
        todo.setPriority(priority);
        todo.setDueDate(dueDate == null ? null : LocalDate.parse(dueDate));
        todo.setCompleted(false);

        todoService.create(todo);
        return TodoDto.from(todo);
    }

    @McpTool(name = "update_todo", description = "やることを1件直す（期限を変えるのもこれ）")
    public TodoDto updateTodo(
            @McpToolParam(required = true) Long id,
            @McpToolParam(required = false) String title,
            @McpToolParam(required = false) String detail,
            @McpToolParam(required = false) String category,
            @McpToolParam(required = false) Integer priority,
            @McpToolParam(required = false) String dueDate) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return null;
        }

        // nullは「その項目は渡されなかった」という意味にして、元の値を残します。
        if (title != null) {
            todo.setTitle(title);
        }
        if (detail != null) {
            todo.setDetail(detail);
        }
        if (category != null) {
            validateCategory(category);
            todo.setCategory(category);
        }
        if (priority != null) {
            todo.setPriority(priority);
        }
        if (dueDate != null) {
            todo.setDueDate(LocalDate.parse(dueDate));
        }

        todoService.update(todo);
        return TodoDto.from(todo);
    }

    @McpTool(name = "complete_todo", description = "やることを完了にする")
    public TodoDto completeTodo(@McpToolParam(required = true) Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return null;
        }

        todo.setCompleted(true);
        todoService.update(todo);
        return TodoDto.from(todo);
    }

    @McpTool(name = "delete_todo", description = "やることを1件消す")
    public void deleteTodo(@McpToolParam(required = true) Long id) {
        todoService.delete(id);
    }

    @McpTool(name = "find_free_days", description = "期間の中で、期限のやることが無く、土日でも祝日でもない「空いている日」を返す。やることの期限を動かす先を決めるのに使う")
    public List<String> findFreeDays(
            @McpToolParam(required = true) String from,
            @McpToolParam(required = true) String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("開始日は終了日以前にしてください");
        }

        Set<LocalDate> scheduledDates = new HashSet<>();
        for (Todo todo : todoService.search(null, null, "asc", fromDate, toDate)) {
            if (todo.getDueDate() != null) {
                scheduledDates.add(todo.getDueDate());
            }
        }

        HolidayClient.HolidayFetchResult holidayResult = holidayClient.getHolidays();
        if (holidayResult.unavailable()) {
            throw new IllegalStateException("祝日を取得できませんでした");
        }

        return fromDate.datesUntil(toDate.plusDays(1))
                // 期限のあるやることが置かれている日を除きます。
                .filter(date -> !scheduledDates.contains(date))
                // 土曜日と日曜日を除きます。
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY
                        && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                // 祝日一覧は日付文字列を鍵にしているため、同じ形式で照合します。
                .filter(date -> !holidayResult.holidays().containsKey(date.toString()))
                .map(LocalDate::toString)
                .toList();
    }

    private void validateCategory(String category) {
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException(
                    "ジャンルはデザイン・マーケティング・プログラミング・資格・就職活動のいずれかにしてください");
        }
    }

    public record TodoSummary(long totalCount, Map<String, Long> categoryCounts, long overdueCount) {
    }
}
