package com.example.scaffold.category;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Server-rendered category management for the admin section. Delegates to
 * {@link CategoryService} for create/update/delete; hierarchy validation
 * (no cycles, no self-parenting) stays in the service layer.
 */
@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryViewController {

    private final CategoryService categoryService;

    public AdminCategoryViewController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("tree", categoryService.getCategoryTree());
        return "admin/categories/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryOptions", categoryService.getFlattenedOptions());
        return "admin/categories/form";
    }

    @GetMapping("/{categoryId}/edit")
    public String editForm(@PathVariable Long categoryId, Model model) {
        model.addAttribute("category", categoryService.getCategory(categoryId));
        model.addAttribute("categoryOptions", categoryService.getFlattenedOptions());
        return "admin/categories/form";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam String slug,
            @RequestParam(required = false) String description, @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String sortOrder) {
        categoryService.createCategory(toRequest(name, slug, description, parentId, sortOrder));
        return "redirect:/admin/categories";
    }

    @PostMapping("/{categoryId}")
    public String update(@PathVariable Long categoryId, @RequestParam String name, @RequestParam String slug,
            @RequestParam(required = false) String description, @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String sortOrder) {
        categoryService.updateCategory(categoryId, toRequest(name, slug, description, parentId, sortOrder));
        return "redirect:/admin/categories";
    }

    @PostMapping("/{categoryId}/delete")
    public String delete(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{categoryId}/thumbnail")
    public String uploadThumbnail(@PathVariable Long categoryId, @RequestParam("file") MultipartFile file) {
        categoryService.uploadThumbnail(categoryId, file);
        return "redirect:/admin/categories/" + categoryId + "/edit";
    }

    @PostMapping("/{categoryId}/panel-image")
    public String uploadPanelImage(@PathVariable Long categoryId, @RequestParam("file") MultipartFile file) {
        categoryService.uploadPanelImage(categoryId, file);
        return "redirect:/admin/categories/" + categoryId + "/edit";
    }

    private CategoryRequest toRequest(String name, String slug, String description, String parentId,
            String sortOrder) {
        Long parsedParentId = parentId == null || parentId.isBlank() ? null : Long.valueOf(parentId);
        Integer parsedSortOrder = sortOrder == null || sortOrder.isBlank() ? null : Integer.valueOf(sortOrder);
        return new CategoryRequest(name, slug, description, parsedParentId, parsedSortOrder);
    }
}
