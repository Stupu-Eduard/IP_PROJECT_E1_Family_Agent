package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseAnalyticsService {

    private final ExpenseJpaRepository repository;

    public BigDecimal calculateTotal(LocalDate from, LocalDate to) {
        log.info("Calculating total expenses from {} to {}", from, to);
        return repository.findByDateBetween(from, to)
                .stream()
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> byCategory(LocalDate from, LocalDate to) {
        log.info("Calculating expenses by category from {} to {}", from, to);
        return repository.findByDateBetween(from, to)
                .stream()
                .collect(Collectors.groupingBy(
                        ExpenseEntity::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseEntity::getAmount, BigDecimal::add)
                ));
    }

    public Map<String, BigDecimal> compareMembers(LocalDate from, LocalDate to) {
        log.info("Comparing expenses between members from {} to {}", from, to);
        return repository.findByDateBetween(from, to)
                .stream()
                .filter(e -> e.getPerson() != null)
                .collect(Collectors.groupingBy(
                        ExpenseEntity::getPerson,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseEntity::getAmount, BigDecimal::add)
                ));
    }

    public List<ExpenseEntity> detectAnomalies(BigDecimal threshold) {
        log.info("Detecting expense anomalies above threshold: {}", threshold);
        return repository.findAll()
                .stream()
                .filter(e -> e.getAmount().compareTo(threshold) > 0)
                .toList();
    }

    public List<ExpenseEntity> findByPerson(String person, LocalDate from, LocalDate to) {
        log.info("Fetching expenses for person: {} from {} to {}", person, from, to);
        return repository.findByDateBetween(from, to)
                .stream()
                .filter(e -> person.equalsIgnoreCase(e.getPerson()))
                .collect(Collectors.toList());
    }

    public List<ExpenseEntity> getTopExpenses(int limit) {
        log.info("Fetching top {} expenses", limit);
        return repository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "amount"))).getContent();
    }

    public BigDecimal calculateMonthlyAverage(int months) {
        log.info("Calculating monthly average for last {} months", months);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusMonths(months).withDayOfMonth(1);
        
        BigDecimal total = calculateTotal(from, to);
        if (months <= 0) return BigDecimal.ZERO;
        
        return total.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
    }

    public String calculateTrend(String category, LocalDate from, LocalDate to) {
        log.info("Calculating trend for category: {} from {} to {}", category, from, to);
        BigDecimal currentTotal = repository.findByDateBetween(from, to).stream()
                .filter(e -> category.equalsIgnoreCase(e.getCategory()))
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);

        BigDecimal prevTotal = repository.findByDateBetween(prevFrom, prevTo).stream()
                .filter(e -> category.equalsIgnoreCase(e.getCategory()))
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (prevTotal.compareTo(BigDecimal.ZERO) == 0) {
            return String.format("Spending on %s is %s RON. No data for the previous period to compare.", category, currentTotal);
        }

        BigDecimal difference = currentTotal.subtract(prevTotal);
        BigDecimal percentage = difference.multiply(new BigDecimal("100")).divide(prevTotal, 2, RoundingMode.HALF_UP);

        String direction = difference.compareTo(BigDecimal.ZERO) >= 0 ? "increased" : "decreased";
        return String.format("Spending on %s has %s by %s%% (%s RON) compared to the previous period (Current: %s RON, Previous: %s RON).",
                category, direction, percentage.abs(), difference.abs(), currentTotal, prevTotal);
    }
}
