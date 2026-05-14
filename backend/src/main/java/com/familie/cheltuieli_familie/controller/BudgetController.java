package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Budget;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.BudgetRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/budgets")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    public BudgetController(BudgetRepository budgetRepository,
                            ExpenseRepository expenseRepository,
                            FamilyMemberRepository familyMemberRepository,
                            UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
    }

    public record ChildBudgetSummary(BigDecimal totalBudget, BigDecimal totalSpent, BigDecimal balance) {}
    public record SetBudgetRequest(@NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal amount) {}

    /** Copilul autentificat îsi vede propriul sold */
    @GetMapping("/child-summary")
    public ChildBudgetSummary getChildSummary(Authentication auth) {
        User user = (User) auth.getPrincipal();
        LocalDate today = LocalDate.now();

        BigDecimal totalBudget = budgetRepository.findChildBudget(user.getId(), today)
                .map(Budget::getAmount)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalSpent = expenseRepository.sumByUserCurrentMonth(
                user.getId(), today.getYear(), today.getMonthValue());

        BigDecimal balance = totalBudget.subtract(totalSpent);
        return new ChildBudgetSummary(totalBudget, totalSpent, balance.max(BigDecimal.ZERO));
    }

    /** Părintele citește bugetul unui copil specific */
    @GetMapping("/child/{childUserId}")
    public BigDecimal getChildBudget(@PathVariable Long childUserId, Authentication auth) {
        verifyFamilyAccess(auth, childUserId);
        LocalDate today = LocalDate.now();
        return budgetRepository.findChildBudget(childUserId, today)
                .map(Budget::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    /** Părintele setează bugetul pentru un copil specific */
    @PutMapping("/child/{childUserId}")
    @ResponseStatus(HttpStatus.OK)
    public BigDecimal setChildBudget(@PathVariable Long childUserId,
                                     @Valid @RequestBody SetBudgetRequest req,
                                     Authentication auth) {
        User parent = (User) auth.getPrincipal();
        verifyFamilyAccess(auth, childUserId);

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth   = today.withDayOfMonth(today.lengthOfMonth());

        User child = userRepository.findById(childUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizatorul nu există."));

        var familyOpt = familyMemberRepository.findByUserId(parent.getId()).stream().findFirst();
        if (familyOpt.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu ești asociat unei familii.");

        Budget budget = budgetRepository.findChildBudget(childUserId, today)
                .orElseGet(() -> {
                    Budget b = new Budget();
                    b.setUser(child);
                    b.setFamily(familyOpt.get().getFamily());
                    b.setStartDate(startOfMonth);
                    b.setEndDate(endOfMonth);
                    return b;
                });

        budget.setAmount(req.amount());
        budgetRepository.save(budget);
        return budget.getAmount();
    }

    private void verifyFamilyAccess(Authentication auth, Long targetUserId) {
        User requester = (User) auth.getPrincipal();
        var requesterFamily = familyMemberRepository.findByUserId(requester.getId())
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ești asociat unei familii."));
        var targetFamily = familyMemberRepository.findByUserId(targetUserId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilizatorul nu este în nicio familie."));
        if (!requesterFamily.getFamily().getId().equals(targetFamily.getFamily().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la acest utilizator.");
        }
    }
}
