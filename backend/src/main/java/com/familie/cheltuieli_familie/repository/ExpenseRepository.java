package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Decuplează cheltuielile unui user șters — user_id devine NULL.
     * Apelat în UserController.deleteOwnAccount() înainte de userRepository.delete().
     */
    @Modifying
    @Query("UPDATE Expense e SET e.user = null WHERE e.user.id = :userId")
    void clearUserFromExpenses(@Param("userId") Long userId);


    interface ExpenseWithLocationProjection {
        Long getId();
        BigDecimal getAmount();
        String getCurrency();
        String getDescription();
        LocalDateTime getExpenseDate();
        String getCategory();
        String getPerson();
        String getSourceType();
        Long getLocationId();
        String getStore();
        String getAddress();
        String getCity();
        String getCountry();
        Double getLat();
        Double getLng();
    }

    @Query(value = """
            SELECT
                e.id AS id,
                e.amount AS amount,
                e.currency AS currency,
                e.description AS description,
                e.expense_date AS expenseDate,
                c.name AS category,
                u.name AS person,
                e.source_type AS sourceType,
                l.id AS locationId,
                l.store AS store,
                l.address AS address,
                l.city AS city,
                l.country AS country,
                COALESCE(ST_Y(CAST(l.location AS geometry)), l.latitude) AS lat,
                COALESCE(ST_X(CAST(l.location AS geometry)), l.longitude) AS lng
            FROM expenses e
            LEFT JOIN categories c ON c.id = e.category_id
            LEFT JOIN users u ON u.id = e.user_id
            LEFT JOIN locations l ON l.id = e.location_id
            ORDER BY e.expense_date DESC
            """, nativeQuery = true)
    List<ExpenseWithLocationProjection> findAllWithLocation();

    @Query(value = """
            SELECT
                e.id AS id,
                e.amount AS amount,
                e.currency AS currency,
                e.description AS description,
                e.expense_date AS expenseDate,
                c.name AS category,
                u.name AS person,
                e.source_type AS sourceType,
                l.id AS locationId,
                l.store AS store,
                l.address AS address,
                l.city AS city,
                l.country AS country,
                COALESCE(ST_Y(CAST(l.location AS geometry)), l.latitude) AS lat,
                COALESCE(ST_X(CAST(l.location AS geometry)), l.longitude) AS lng
            FROM expenses e
            LEFT JOIN categories c ON c.id = e.category_id
            LEFT JOIN users u ON u.id = e.user_id
            LEFT JOIN locations l ON l.id = e.location_id
            WHERE (:expenseDate IS NULL OR CAST(e.expense_date AS date) = :expenseDate)
              AND (:category IS NULL OR c.name = :category)
              AND (:person IS NULL OR u.name = :person)
            ORDER BY e.expense_date DESC
            """, nativeQuery = true)
    List<ExpenseWithLocationProjection> findAllWithLocationFiltered(
            @Param("expenseDate") LocalDate expenseDate,
            @Param("category") String category,
            @Param("person") String person
    );

    @Query(value = """
            SELECT
                e.id AS id,
                e.amount AS amount,
                e.currency AS currency,
                e.description AS description,
                e.expense_date AS expenseDate,
                c.name AS category,
                u.name AS person,
                e.source_type AS sourceType,
                l.id AS locationId,
                l.store AS store,
                l.address AS address,
                l.city AS city,
                l.country AS country,
                COALESCE(ST_Y(CAST(l.location AS geometry)), l.latitude) AS lat,
                COALESCE(ST_X(CAST(l.location AS geometry)), l.longitude) AS lng
            FROM expenses e
            LEFT JOIN categories c ON c.id = e.category_id
            LEFT JOIN users u ON u.id = e.user_id
            LEFT JOIN locations l ON l.id = e.location_id
            WHERE e.family_id = :familyId
              AND (:expenseDate IS NULL OR CAST(e.expense_date AS date) = :expenseDate)
              AND (:category IS NULL OR c.name = :category)
              AND (:person IS NULL OR u.name = :person)
            ORDER BY e.expense_date DESC
            """, nativeQuery = true)
    List<ExpenseWithLocationProjection> findAllByFamilyFiltered(
            @Param("familyId") Long familyId,
            @Param("expenseDate") LocalDate expenseDate,
            @Param("category") String category,
            @Param("person") String person
    );

    @Query(value = """
            SELECT
                e.id AS id,
                e.amount AS amount,
                e.currency AS currency,
                e.description AS description,
                e.expense_date AS expenseDate,
                c.name AS category,
                u.name AS person,
                e.source_type AS sourceType,
                l.id AS locationId,
                l.store AS store,
                l.address AS address,
                l.city AS city,
                l.country AS country,
                COALESCE(ST_Y(CAST(l.location AS geometry)), l.latitude) AS lat,
                COALESCE(ST_X(CAST(l.location AS geometry)), l.longitude) AS lng
            FROM expenses e
            LEFT JOIN categories c ON c.id = e.category_id
            LEFT JOIN users u ON u.id = e.user_id
            LEFT JOIN locations l ON l.id = e.location_id
            WHERE e.user_id = :userId
              AND (:expenseDate IS NULL OR CAST(e.expense_date AS date) = :expenseDate)
              AND (:category IS NULL OR c.name = :category)
            ORDER BY e.expense_date DESC
            """, nativeQuery = true)
    List<ExpenseWithLocationProjection> findAllByUserFiltered(
            @Param("userId") Long userId,
            @Param("expenseDate") LocalDate expenseDate,
            @Param("category") String category
    );

    @Query(value = """
            SELECT
                e.id AS id,
                e.amount AS amount,
                e.currency AS currency,
                e.description AS description,
                e.expense_date AS expenseDate,
                c.name AS category,
                u.name AS person,
                e.source_type AS sourceType,
                l.id AS locationId,
                l.store AS store,
                l.address AS address,
                l.city AS city,
                l.country AS country,
                COALESCE(ST_Y(CAST(l.location AS geometry)), l.latitude) AS lat,
                COALESCE(ST_X(CAST(l.location AS geometry)), l.longitude) AS lng
            FROM expenses e
            LEFT JOIN categories c ON c.id = e.category_id
            LEFT JOIN users u ON u.id = e.user_id
            LEFT JOIN locations l ON l.id = e.location_id
            WHERE e.id = ?1
            """, nativeQuery = true)
    ExpenseWithLocationProjection findOneWithLocation(Long id);

    @Query(value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e WHERE e.user_id = :userId AND EXTRACT(YEAR FROM e.expense_date) = :year AND EXTRACT(MONTH FROM e.expense_date) = :month", nativeQuery = true)
    BigDecimal sumByUserCurrentMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
}