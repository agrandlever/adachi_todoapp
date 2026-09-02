package com.example.todoapp.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;

class TodoApiControllerTest {

    @Test
    void csvKeepsConditionsAndProtectsSpreadsheetCells() {
        TodoService todoService = mock(TodoService.class);
        Todo todo = new Todo();
        todo.setTitle("=1+1");
        todo.setDetail("+SUM(1,1)");
        todo.setCategory("資格");
        todo.setPriority(1);
        todo.setDueDate(LocalDate.of(2026, 9, 30));
        todo.setCompleted(false);
        todo.setPinned(false);

        when(todoService.search("式", "資格", "desc", false, false)).thenReturn(List.of(todo));
        TodoApiController controller = new TodoApiController(todoService);

        ResponseEntity<byte[]> response = controller.downloadCsv("式", "資格", "desc", false, 0);
        byte[] body = response.getBody();
        String csv = new String(body, StandardCharsets.UTF_8);

        assertEquals("attachment; filename=\"todos.csv\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                new byte[] {body[0], body[1], body[2]});
        assertTrue(csv.contains("\"'=1+1\""));
        assertTrue(csv.contains("\"'+SUM(1,1)\""));
        verify(todoService).search("式", "資格", "desc", false, false);
    }
}
