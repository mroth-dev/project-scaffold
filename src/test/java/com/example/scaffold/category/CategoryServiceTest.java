package com.example.scaffold.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.product.Product;
import com.example.scaffold.product.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryService categoryService;

    private Category root;
    private Category child;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, productRepository);

        root = new Category();
        root.setId(1L);
        root.setName("Clothing");
        root.setSlug("clothing");
        root.setSortOrder(0);
        root.setCreatedAt(LocalDateTime.now());
        root.setUpdatedAt(LocalDateTime.now());

        child = new Category();
        child.setId(2L);
        child.setName("Shirts");
        child.setSlug("shirts");
        child.setParent(root);
        child.setSortOrder(0);
        child.setCreatedAt(LocalDateTime.now());
        child.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getCategoryReturnsMappedDto() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(child));

        CategoryDto result = categoryService.getCategory(2L);

        assertEquals("shirts", result.slug());
        assertEquals(1L, result.parentId());
    }

    @Test
    void getCategoryThrowsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getCategory(99L));
    }

    @Test
    void getCategoryTreeNestsChildrenUnderRoots() {
        when(categoryRepository.findAll()).thenReturn(List.of(root, child));

        List<CategoryTreeDto> tree = categoryService.getCategoryTree();

        assertEquals(1, tree.size());
        assertEquals("clothing", tree.get(0).slug());
        assertEquals(1, tree.get(0).children().size());
        assertEquals("shirts", tree.get(0).children().get(0).slug());
    }

    @Test
    void createCategoryWithParentResolvesParent() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryRequest request = new CategoryRequest("Shirts", "shirts", null, 1L, 0);
        CategoryDto created = categoryService.createCategory(request);

        assertEquals("shirts", created.slug());
        assertEquals(1L, created.parentId());
    }

    @Test
    void updateCategoryRejectsSelfAsParent() {
        CategoryRequest request = new CategoryRequest("Shirts", "shirts", null, 2L, 0);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(child));

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(2L, request));
    }

    @Test
    void updateCategoryRejectsDescendantAsParent() {
        // Attempting to set root's parent to child, but child's parent is root -> would create a cycle
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root));
        when(categoryRepository.findParentId(2L)).thenReturn(Optional.of(1L));

        CategoryRequest request = new CategoryRequest("Clothing", "clothing", null, 2L, 0);

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(1L, request));
    }

    @Test
    void deleteCategoryThrowsWhenMissing() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(99L));
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void addProductToCategoryAssociatesProduct() {
        Product product = new Product();
        product.setId(5L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.addProductToCategory(1L, 5L);

        assertTrue(product.getCategories().contains(root));
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void removeProductFromCategoryRemovesAssociation() {
        Product product = new Product();
        product.setId(5L);
        product.getCategories().add(root);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.removeProductFromCategory(1L, 5L);

        assertTrue(product.getCategories().isEmpty());
    }
}
