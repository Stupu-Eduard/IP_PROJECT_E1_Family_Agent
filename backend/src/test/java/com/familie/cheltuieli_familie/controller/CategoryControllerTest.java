package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CategoryControllerTest {

    @Test
    void list_filtersOutNullAndBlankNames() {
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        CategoryController controller = new CategoryController(categoryRepository);

        Category c1 = new Category();
        c1.setName("Food");

        Category c2 = new Category();
        c2.setName(" ");

        Category c3 = new Category();
        c3.setName(null);

        when(categoryRepository.findAll(eq(Sort.by(Sort.Direction.ASC, "name"))))
                .thenReturn(List.of(c1, c2, c3));

        List<String> result = controller.list();

        assertEquals(List.of("Food"), result);
        verify(categoryRepository).findAll(eq(Sort.by(Sort.Direction.ASC, "name")));
    }
}
