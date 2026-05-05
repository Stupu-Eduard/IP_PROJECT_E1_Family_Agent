package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Family;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FamilyRepositoryTest {

    @Autowired
    private FamilyRepository familyRepository;


    private Family buildFamily(String name) {
        Family f = new Family();
        f.setName(name);
        f.setCreatedAt(LocalDate.now());
        return f;
    }



    @Test
    @DisplayName("Create family – should persist entity and generate ID")
    void shouldSaveFamilyAndGenerateId() {
        Family saved = familyRepository.save(buildFamily("Familia Ionescu"));

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
    }

    @Test
    @DisplayName("Create family – should persist createdAt date")
    void shouldPersistCreatedAtDate() {
        LocalDate today = LocalDate.now();
        Family saved = familyRepository.save(buildFamily("Familia Pop"));

        assertEquals(today, saved.getCreatedAt());
    }

    @Test
    @DisplayName("Create family – should persist without createdAt (nullable)")
    void shouldAllowNullCreatedAt() {
        Family f = new Family();
        f.setName("Familia Fara Data");

        Family saved = familyRepository.save(f);

        assertNotNull(saved.getId());
        assertNull(saved.getCreatedAt());
    }



    @Test
    @DisplayName("Read family – should retrieve entity by ID")
    void shouldFindFamilyById() {
        Family saved = familyRepository.save(buildFamily("Familia Popa"));

        Optional<Family> found = familyRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Familia Popa", found.get().getName());
    }

    @Test
    @DisplayName("Read family – should return empty Optional for non-existent ID")
    void shouldReturnEmptyOptionalForNonExistentId() {
        Optional<Family> result = familyRepository.findById(-999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Read family – findAll should include newly saved families")
    void shouldIncludeNewFamilyInFindAll() {
        Family saved = familyRepository.save(buildFamily("Familia Mihail"));

        List<Family> all = familyRepository.findAll();

        assertTrue(all.stream().anyMatch(f -> f.getId().equals(saved.getId())));
    }



    @Test
    @DisplayName("Update family – should persist name change")
    void shouldUpdateFamilyName() {
        Family saved = familyRepository.save(buildFamily("Familia Veche"));

        saved.setName("Familia Noua");
        Family updated = familyRepository.save(saved);

        assertEquals("Familia Noua", updated.getName());
        assertEquals(saved.getId(), updated.getId());
    }

    @Test
    @DisplayName("Update family – should persist createdAt change")
    void shouldUpdateFamilyCreatedAt() {
        Family saved = familyRepository.save(buildFamily("Familia Update"));
        LocalDate newDate = LocalDate.of(2023, 1, 15);

        saved.setCreatedAt(newDate);
        Family updated = familyRepository.save(saved);

        assertEquals(newDate, updated.getCreatedAt());
    }



    @Test
    @DisplayName("Delete family – should remove entity from DB")
    void shouldDeleteFamily() {
        Family saved = familyRepository.save(buildFamily("Familia DeSteras"));
        Long id = saved.getId();

        familyRepository.deleteById(id);

        assertFalse(familyRepository.findById(id).isPresent());
    }

    @Test
    @DisplayName("Delete family – existsById should return false after deletion")
    void shouldReturnFalseExistsByIdAfterDelete() {
        Family saved = familyRepository.save(buildFamily("Familia DeSteras2"));
        Long id = saved.getId();

        familyRepository.deleteById(id);

        assertFalse(familyRepository.existsById(id));
    }



    @Test
    @DisplayName("Count – should increase after saving a new family")
    void shouldIncrementCountAfterSave() {
        long before = familyRepository.count();

        familyRepository.save(buildFamily("Familia Count"));

        assertEquals(before + 1, familyRepository.count());
    }
}