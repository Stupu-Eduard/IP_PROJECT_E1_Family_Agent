package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FamilyMemberRepositoryTest {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    // ------------------------------------------------------------------ helpers

    /** Creează și salvează un User cu email garantat unic. */
    private User savedUser(String nameSuffix) {
        User u = new User();
        u.setName("User_" + nameSuffix);
        // UUID previne coliziunile de email între teste
        u.setEmail("fm_" + UUID.randomUUID() + "@test.com");
        u.setPasswordH("pass");
        u.setCreatedAt(LocalDate.now());
        return userRepository.save(u);
    }

    /** Creează și salvează o Family. */
    private Family savedFamily(String name) {
        Family f = new Family();
        f.setName(name);
        f.setCreatedAt(LocalDate.now());
        return familyRepository.save(f);
    }

    /** Construiește un FamilyMember fără a-l salva. */
    private FamilyMember buildMember(User user, Family family, String role) {
        FamilyMember fm = new FamilyMember();
        fm.setUser(user);
        fm.setFamily(family);
        fm.setRole(role);
        return fm;
    }

    // ------------------------------------------------------------------ CREATE

    @Test
    @DisplayName("Create FamilyMember – should persist and generate ID")
    void shouldSaveFamilyMemberAndGenerateId() {
        User user = savedUser("create1");
        Family family = savedFamily("Familie_C1");

        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "ADULT"));

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
    }

    @Test
    @DisplayName("Create FamilyMember – should persist the role field")
    void shouldPersistRole() {
        User user = savedUser("roleTest");
        Family family = savedFamily("Familie_Role");

        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "CHILD"));

        assertEquals("CHILD", saved.getRole());
    }

    // ------------------------------------------------------------------ READ

    @Test
    @DisplayName("Read FamilyMember – should retrieve entity by ID")
    void shouldFindFamilyMemberById() {
        User user = savedUser("read1");
        Family family = savedFamily("Familie_R1");
        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "ADULT"));

        Optional<FamilyMember> found = familyMemberRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Read FamilyMember – should return empty Optional for non-existent ID")
    void shouldReturnEmptyForNonExistentId() {
        Optional<FamilyMember> result = familyMemberRepository.findById(-999L);

        assertFalse(result.isPresent());
    }

    // ------------------------------------------------------------------ RELATIONS: FamilyMember -> User

    @Test
    @DisplayName("Relație FamilyMember->User – user asociat trebuie să fie cel salvat")
    void shouldLoadCorrectAssociatedUser() {
        User user = savedUser("assocUser");
        Family family = savedFamily("Familie_AssocU");
        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "ADULT"));

        FamilyMember found = familyMemberRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(found.getUser());
        assertEquals(user.getId(), found.getUser().getId());
        assertEquals(user.getEmail(), found.getUser().getEmail());
    }

    @Test
    @DisplayName("Relație FamilyMember->User – datele User-ului sunt corecte după fetch")
    void shouldHaveCorrectUserNameAfterFetch() {
        User user = savedUser("nameCheck");
        Family family = savedFamily("Familie_NameCheck");
        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "ADULT"));

        FamilyMember found = familyMemberRepository.findById(saved.getId()).orElseThrow();

        assertEquals(user.getName(), found.getUser().getName());
    }

    // ------------------------------------------------------------------ RELATIONS: FamilyMember -> Family

    @Test
    @DisplayName("Relație FamilyMember->Family – family asociată trebuie să fie cea salvată")
    void shouldLoadCorrectAssociatedFamily() {
        User user = savedUser("assocFam");
        Family family = savedFamily("Familie_AssocF");
        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "ADULT"));

        FamilyMember found = familyMemberRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(found.getFamily());
        assertEquals(family.getId(), found.getFamily().getId());
        assertEquals(family.getName(), found.getFamily().getName());
    }

    @Test
    @DisplayName("Relație FamilyMember->Family – FamilyMember poate fi creat fără User (null FK)")
    void shouldAllowNullUserReference() {
        Family family = savedFamily("Familie_NullUser");
        FamilyMember fm = buildMember(null, family, "GUEST");

        // JPA nu impune NOT NULL pe user_id în entitate, deci salvarea trebuie să reușească
        FamilyMember saved = familyMemberRepository.save(fm);

        assertNotNull(saved.getId());
        assertNull(saved.getUser());
    }

    // ------------------------------------------------------------------ UPDATE

    @Test
    @DisplayName("Update FamilyMember – should persist role change")
    void shouldUpdateRole() {
        User user = savedUser("updRole");
        Family family = savedFamily("Familie_UpdRole");
        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "CHILD"));

        saved.setRole("ADMIN");
        FamilyMember updated = familyMemberRepository.save(saved);

        assertEquals("ADMIN", updated.getRole());
    }

    // ------------------------------------------------------------------ DELETE

    @Test
    @DisplayName("Delete FamilyMember – should remove entity from DB")
    void shouldDeleteFamilyMember() {
        User user = savedUser("del1");
        Family family = savedFamily("Familie_Del1");
        FamilyMember saved = familyMemberRepository.save(buildMember(user, family, "ADULT"));
        Long id = saved.getId();

        familyMemberRepository.deleteById(id);

        assertFalse(familyMemberRepository.findById(id).isPresent());
    }

    // ------------------------------------------------------------------ CONSTRAINTS

    @Test
    @DisplayName("Constrângere NOT NULL role – ar trebui să arunce excepție")
    void shouldFailWhenRoleIsNull() {
        User user = savedUser("roleNull");
        Family family = savedFamily("Familie_RoleNull");
        FamilyMember fm = buildMember(user, family, null);

        assertThrows(Exception.class, () -> familyMemberRepository.saveAndFlush(fm));
    }

    @Test
    @DisplayName("UNIQUE(family_id, user_id) – duplicate trebuie să arunce excepție")
    void shouldEnforceUniqueUserFamilyCombination() {
        User user = savedUser("uniq1");
        Family family = savedFamily("Familie_Uniq1");

        FamilyMember first = buildMember(user, family, "ADULT");
        familyMemberRepository.save(first);

        FamilyMember duplicate = buildMember(user, family, "CHILD");

        assertThrows(Exception.class, () -> familyMemberRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("UNIQUE(family_id, user_id) – același user în familii diferite este permis")
    void shouldAllowSameUserInDifferentFamilies() {
        User user = savedUser("sameUser2Fam");
        Family family1 = savedFamily("Familie_Dif1");
        Family family2 = savedFamily("Familie_Dif2");

        familyMemberRepository.save(buildMember(user, family1, "ADULT"));
        FamilyMember member2 = familyMemberRepository.save(buildMember(user, family2, "ADULT"));

        assertNotNull(member2.getId());
    }

    @Test
    @DisplayName("UNIQUE(family_id, user_id) – utilizatori diferiți în aceeași familie sunt permiși")
    void shouldAllowDifferentUsersInSameFamily() {
        User user1 = savedUser("diffUser1");
        User user2 = savedUser("diffUser2");
        Family family = savedFamily("Familie_Shared");

        familyMemberRepository.save(buildMember(user1, family, "ADULT"));
        FamilyMember member2 = familyMemberRepository.save(buildMember(user2, family, "CHILD"));

        assertNotNull(member2.getId());
    }
}