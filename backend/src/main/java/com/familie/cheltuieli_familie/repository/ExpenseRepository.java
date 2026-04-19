package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	interface ExpenseWithLocationProjection {
		Long getId();

		BigDecimal getAmount();

		String getCurrency();

		String getDescription();

		LocalDateTime getExpenseDate();

		String getCategory();

		String getPerson();

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
				l.id AS locationId,
				l.store AS store,
				l.adress AS address,
				l.city AS city,
				l.country AS country,
				ST_Y(l.location::geometry) AS lat,
				ST_X(l.location::geometry) AS lng
			FROM expenses e
			LEFT JOIN categories c ON c.id = e.category_id
			LEFT JOIN users u ON u.id = e.user_id
			LEFT JOIN locations l ON l.id = e.location_id
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
				l.id AS locationId,
				l.store AS store,
				l.adress AS address,
				l.city AS city,
				l.country AS country,
				ST_Y(l.location::geometry) AS lat,
				ST_X(l.location::geometry) AS lng
			FROM expenses e
			LEFT JOIN categories c ON c.id = e.category_id
			LEFT JOIN users u ON u.id = e.user_id
			LEFT JOIN locations l ON l.id = e.location_id
			WHERE e.id = ?1
			""", nativeQuery = true)
	ExpenseWithLocationProjection findOneWithLocation(Long id);
}