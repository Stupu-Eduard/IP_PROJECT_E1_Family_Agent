package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Category;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;



    private Category buildCategory(String name) {
        Category c = new Category();
        c.setName(name);
        c.setDescription("Descriere pentru " + name);
        c.setIsActive(false);
        return c;
    }

    private Category savedCategory(String name) {
        return categoryRepository.save(buildCategory(name));
    }



    @Test
    @DisplayName("Create Category – should persist and generate ID")
    void shouldSaveCategoryAndGenerateId() {
        Category saved = savedCategory("Alimente");

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
    }

    @Test
    @DisplayName("Create Category – should persist name field")
    void shouldPersistName() {
        Category saved = savedCategory("Transport");

        assertEquals("Transport", saved.getName());
    }

    @Test
    @DisplayName("Create Category – should persist description field")
    void shouldPersistDescription() {
        Category c = new Category();
        c.setName("Utilitati");
        c.setDescription("Curent, apa, gaz");
        Category saved = categoryRepository.save(c);

        assertEquals("Curent, apa, gaz", saved.getDescription());
    }

    @Test
    @DisplayName("Create Category – isActive should default to false")
    void shouldDefaultIsActiveToFalse() {
        Category c = new Category();
        c.setName("CategorieNoua");
        // isActive nu este setat explicit
        Category saved = categoryRepository.save(c);

        assertFalse(saved.getIsActive());
    }

    @Test
    @DisplayName("Create Category – isActive can be set to true")
    void shouldPersistIsActiveTrue() {
        Category c = buildCategory("CategorieActiva");
        c.setIsActive(true);
        Category saved = categoryRepository.save(c);

        assertTrue(saved.getIsActive());
    }

    @Test
    @DisplayName("Create Category – root category without parent should have null parent")
    void shouldAllowRootCategoryWithNullParent() {
        Category root = new Category();
        root.setName("Root");
        Category saved = categoryRepository.save(root);

        assertNull(saved.getParent());
    }



    @Test
    @DisplayName("Read Category – should retrieve entity by ID")
    void shouldFindCategoryById() {
        Category saved = savedCategory("Sanatate");

        Optional<Category> found = categoryRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Sanatate", found.get().getName());
    }

    @Test
    @DisplayName("Read Category – should return empty Optional for non-existent ID")
    void shouldReturnEmptyOptionalForNonExistentId() {
        Optional<Category> result = categoryRepository.findById(-999L);

        assertFalse(result.isPresent());
    }



    @Test
    @DisplayName("Update Category – should persist name change")
    void shouldUpdateCategoryName() {
        Category saved = savedCategory("NumeVechi");

        saved.setName("NumeNou");
        Category updated = categoryRepository.save(saved);

        assertEquals("NumeNou", updated.getName());
        assertEquals(saved.getId(), updated.getId());
    }

    @Test
    @DisplayName("Update Category – should persist isActive change")
    void shouldUpdateIsActive() {
        Category saved = savedCategory("CategorieInactiva");
        assertFalse(saved.getIsActive());

        saved.setIsActive(true);
        Category updated = categoryRepository.save(saved);

        assertTrue(updated.getIsActive());
    }



    @Test
    @DisplayName("Delete Category – should remove entity from DB")
    void shouldDeleteCategory() {
        Category saved = savedCategory("DeStears");
        Long id = saved.getId();

        categoryRepository.deleteById(id);

        assertFalse(categoryRepository.findById(id).isPresent());
    }



    @Test
    @DisplayName("Ierarhie Category – child ar trebui să aibă parent setat")
    void shouldSaveCategoryWithParent() {
        Category parent = savedCategory("Parinte");
        Category child = buildCategory("Copil");
        child.setParent(parent);
        Category savedChild = categoryRepository.save(child);

        assertNotNull(savedChild.getParent());
        assertEquals(parent.getId(), savedChild.getParent().getId());
    }

    @Test
    @DisplayName("Ierarhie Category – parent.name trebuie să fie corect după fetch")
    void shouldLoadCorrectParentNameAfterFetch() {
        Category parent = savedCategory("Parinte_Fetch");
        Category child = buildCategory("Copil_Fetch");
        child.setParent(parent);
        Category savedChild = categoryRepository.save(child);

        Category found = categoryRepository.findById(savedChild.getId()).orElseThrow();

        assertEquals("Parinte_Fetch", found.getParent().getName());
    }

    @Test
    @DisplayName("Ierarhie Category – ierarhie pe 3 nivele (grandparent -> parent -> child)")
    void shouldSupportThreeLevelHierarchy() {
        Category grandparent = savedCategory("Bunic");
        Category parent = buildCategory("Parinte3N");
        parent.setParent(grandparent);
        Category savedParent = categoryRepository.save(parent);

        Category child = buildCategory("Copil3N");
        child.setParent(savedParent);
        Category savedChild = categoryRepository.save(child);

        Category foundChild = categoryRepository.findById(savedChild.getId()).orElseThrow();
        assertNotNull(foundChild.getParent());
        assertEquals(savedParent.getId(), foundChild.getParent().getId());

        Category foundParent = categoryRepository.findById(savedParent.getId()).orElseThrow();
        assertNotNull(foundParent.getParent());
        assertEquals(grandparent.getId(), foundParent.getParent().getId());
    }

    @Test
    @DisplayName("Ierarhie Category – mai mulți copii sub același parent sunt permiși")
    void shouldAllowMultipleChildrenUnderSameParent() {
        Category parent = savedCategory("ParinteMulti");

        Category child1 = buildCategory("Copil1");
        child1.setParent(parent);
        Category child2 = buildCategory("Copil2");
        child2.setParent(parent);

        Category savedChild1 = categoryRepository.save(child1);
        Category savedChild2 = categoryRepository.save(child2);

        assertEquals(parent.getId(), savedChild1.getParent().getId());
        assertEquals(parent.getId(), savedChild2.getParent().getId());
    }

    @Test
    @DisplayName("Ierarhie Category – schimbarea parent-ului trebuie persisată")
    void shouldUpdateParentReference() {
        Category parent1 = savedCategory("ParinteInitial");
        Category parent2 = savedCategory("ParinteNou");
        Category child = buildCategory("CopilMutat");
        child.setParent(parent1);
        Category savedChild = categoryRepository.save(child);

        savedChild.setParent(parent2);
        Category updated = categoryRepository.save(savedChild);

        assertEquals(parent2.getId(), updated.getParent().getId());
    }

    @Test
    @DisplayName("Ierarhie Category – eliminarea parent-ului (setare null) trebuie persisată")
    void shouldAllowRemovingParentReference() {
        Category parent = savedCategory("ParinteDezlipit");
        Category child = buildCategory("CopilDezlipit");
        child.setParent(parent);
        Category savedChild = categoryRepository.save(child);
        assertNotNull(savedChild.getParent());

        savedChild.setParent(null);
        Category updated = categoryRepository.save(savedChild);

        assertNull(updated.getParent());
    }
}