package com.example.todoapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
public class HomeController {

    private final TodoMapper todoMapper;

    // コンストラクタで受け取ることで、HomeControllerがTodoMapperを利用できるようにします。
    public HomeController(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "やること管理");
        return "index";
    }

    @GetMapping("/todos")
    public String todos(@RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order, Model model) {
        if (!"desc".equals(order)) {
            order = "asc";
        }

        model.addAttribute("todos", todoMapper.search(keyword, category, order));
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("order", order);
        return "todos";
    }

    @GetMapping("/todos/new")
    public String create(Model model) {
        model.addAttribute("todo", new Todo());
        return "create";
    }

    @PostMapping("/todos/confirm")
    public String confirm(@Valid @ModelAttribute("todo") Todo todo, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "create";
        }

        return "create-confirm";
    }

    @PostMapping("/todos/new")
    public String rewrite(@ModelAttribute("todo") Todo todo) {
        return "create";
    }

    @PostMapping("/todos")
    public String insert(@ModelAttribute("todo") Todo todo, RedirectAttributes redirectAttributes) {
        todoMapper.insert(todo);
        redirectAttributes.addFlashAttribute("message", "登録しました");
        return "redirect:/todos";
    }

    @GetMapping("/todos/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoMapper.findById(id);

        if (todo == null) {
            redirectAttributes.addFlashAttribute("message", "見つかりませんでした");
            return "redirect:/todos";
        }

        model.addAttribute("todo", todo);
        return "edit";
    }

    @PostMapping("/todos/{id}/confirm")
    public String editConfirm(@PathVariable Long id, @Valid @ModelAttribute("todo") Todo todo,
            BindingResult bindingResult) {
        todo.setId(id);

        if (bindingResult.hasErrors()) {
            return "edit";
        }

        return "edit-confirm";
    }

    @PostMapping("/todos/{id}/edit")
    public String editRewrite(@PathVariable Long id, @ModelAttribute("todo") Todo todo) {
        todo.setId(id);
        return "edit";
    }

    @PostMapping("/todos/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("todo") Todo todo,
            RedirectAttributes redirectAttributes) {
        todo.setId(id);
        todoMapper.update(todo);
        redirectAttributes.addFlashAttribute("message", "保存しました");
        return "redirect:/todos";
    }

    @GetMapping("/todos/{id}/delete")
    public String deleteConfirm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoMapper.findById(id);

        if (todo == null) {
            redirectAttributes.addFlashAttribute("message", "見つかりませんでした");
            return "redirect:/todos";
        }

        model.addAttribute("todo", todo);
        return "delete";
    }

    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        todoMapper.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "削除しました");
        return "redirect:/todos";
    }
}
