package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FamilyMemberTest {

    @Test
    @DisplayName("Model Unit Test: Should have null id before persistence")
    void testIdIsNullByDefault() {
        FamilyMember fm = new FamilyMember();

        assertNull(fm.getId());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get role")
    void testRoleGetterSetter() {
        FamilyMember fm = new FamilyMember();
        fm.setRole("ADULT");

        assertEquals("ADULT", fm.getRole());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null role by default")
    void testRoleIsNullByDefault() {
        FamilyMember fm = new FamilyMember();

        assertNull(fm.getRole());
    }

    @Test
    @DisplayName("Model Unit Test: Should override role with new value")
    void testOverwriteRole() {
        FamilyMember fm = new FamilyMember();
        fm.setRole("CHILD");
        fm.setRole("ADMIN");

        assertEquals("ADMIN", fm.getRole());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly link to a User")
    void testUserLink() {
        FamilyMember fm = new FamilyMember();
        User user = new User();
        user.setName("Maria");
        user.setEmail("maria@test.com");

        fm.setUser(user);

        assertEquals("Maria", fm.getUser().getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should access User email through FamilyMember")
    void testUserEmailThroughFamilyMember() {
        FamilyMember fm = new FamilyMember();
        User user = new User();
        user.setEmail("ion@test.com");

        fm.setUser(user);

        assertEquals("ion@test.com", fm.getUser().getEmail());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null user by default")
    void testUserIsNullByDefault() {
        FamilyMember fm = new FamilyMember();

        assertNull(fm.getUser());
    }

    @Test
    @DisplayName("Model Unit Test: Should allow replacing the linked User")
    void testReplaceUser() {
        FamilyMember fm = new FamilyMember();

        User u1 = new User();
        u1.setName("Primul");

        User u2 = new User();
        u2.setName("AlDoilea");

        fm.setUser(u1);
        fm.setUser(u2);

        assertEquals("AlDoilea", fm.getUser().getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly link to a Family")
    void testFamilyLink() {
        FamilyMember fm = new FamilyMember();
        Family family = new Family();
        family.setName("Familia Ionescu");

        fm.setFamily(family);

        assertEquals("Familia Ionescu", fm.getFamily().getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null family by default")
    void testFamilyIsNullByDefault() {
        FamilyMember fm = new FamilyMember();

        assertNull(fm.getFamily());
    }

    @Test
    @DisplayName("Model Unit Test: Should allow replacing the linked Family")
    void testReplaceFamily() {
        FamilyMember fm = new FamilyMember();

        Family f1 = new Family();
        f1.setName("Familia A");

        Family f2 = new Family();
        f2.setName("Familia B");

        fm.setFamily(f1);
        fm.setFamily(f2);

        assertEquals("Familia B", fm.getFamily().getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly hold User, Family and role simultaneously")
    void testAllFieldsTogether() {
        FamilyMember fm = new FamilyMember();

        User user = new User();
        user.setName("Ana");
        user.setCreatedAt(LocalDate.of(2024, 1, 1));

        Family family = new Family();
        family.setName("Familia Popa");

        fm.setUser(user);
        fm.setFamily(family);
        fm.setRole("PARENT");

        assertEquals("Ana", fm.getUser().getName());
        assertEquals("Familia Popa", fm.getFamily().getName());
        assertEquals("PARENT", fm.getRole());
    }
}