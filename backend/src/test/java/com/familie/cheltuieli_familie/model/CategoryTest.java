package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    @DisplayName("Model Unit Test: Should have null id before persistence")
    void testIdIsNullByDefault() {
        Category c = new Category();

        assertNull(c.getId());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get name")
    void testNameGetterSetter() {
        Category c = new Category();
        c.setName("Alimente");

        assertEquals("Alimente", c.getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null name by default")
    void testNameIsNullByDefault() {
        Category c = new Category();

        assertNull(c.getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get description")
    void testDescriptionGetterSetter() {
        Category c = new Category();
        c.setDescription("Produse alimentare si bauturi");

        assertEquals("Produse alimentare si bauturi", c.getDescription());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null description by default")
    void testDescriptionIsNullByDefault() {
        Category c = new Category();

        assertNull(c.getDescription());
    }

    @Test
    @DisplayName("Model Unit Test: Should initialize isActive to false")
    void testIsActiveDefaultIsFalse() {
        Category c = new Category();

        assertFalse(c.getIsActive());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set isActive to true")
    void testSetIsActiveTrue() {
        Category c = new Category();
        c.setIsActive(true);

        assertTrue(c.getIsActive());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly reset isActive to false")
    void testSetIsActiveFalse() {
        Category c = new Category();
        c.setIsActive(true);
        c.setIsActive(false);

        assertFalse(c.getIsActive());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null parent by default (root category)")
    void testParentIsNullByDefault() {
        Category c = new Category();

        assertNull(c.getParent());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly link to a parent Category")
    void testParentLink() {
        Category parent = new Category();
        parent.setName("Cheltuieli");

        Category child = new Category();
        child.setParent(parent);

        assertEquals("Cheltuieli", child.getParent().getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should access parent description through child")
    void testParentDescriptionThroughChild() {
        Category parent = new Category();
        parent.setName("Transport");
        parent.setDescription("Cheltuieli de transport");

        Category child = new Category();
        child.setName("Combustibil");
        child.setParent(parent);

        assertEquals("Cheltuieli de transport", child.getParent().getDescription());
    }

    @Test
    @DisplayName("Model Unit Test: Should support three-level hierarchy (grandparent -> parent -> child)")
    void testThreeLevelHierarchy() {
        Category grandparent = new Category();
        grandparent.setName("Nivel1");

        Category parent = new Category();
        parent.setName("Nivel2");
        parent.setParent(grandparent);

        Category child = new Category();
        child.setName("Nivel3");
        child.setParent(parent);

        assertEquals("Nivel2", child.getParent().getName());
        assertEquals("Nivel1", child.getParent().getParent().getName());
        assertNull(child.getParent().getParent().getParent());
    }

    @Test
    @DisplayName("Model Unit Test: Should allow replacing parent reference")
    void testReplaceParent() {
        Category parent1 = new Category();
        parent1.setName("ParinteVechi");

        Category parent2 = new Category();
        parent2.setName("ParinteNou");

        Category child = new Category();
        child.setParent(parent1);
        child.setParent(parent2);

        assertEquals("ParinteNou", child.getParent().getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should allow removing parent reference (set to null)")
    void testRemoveParentReference() {
        Category parent = new Category();
        parent.setName("Parinte");

        Category child = new Category();
        child.setParent(parent);
        child.setParent(null);

        assertNull(child.getParent());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly hold all fields simultaneously")
    void testAllFieldsTogether() {
        Category parent = new Category();
        parent.setName("Utilitati");

        Category child = new Category();
        child.setName("Curent Electric");
        child.setDescription("Factura electricitate lunara");
        child.setIsActive(true);
        child.setParent(parent);

        assertEquals("Curent Electric", child.getName());
        assertEquals("Factura electricitate lunara", child.getDescription());
        assertTrue(child.getIsActive());
        assertEquals("Utilitati", child.getParent().getName());
    }
}