package com.uip.backend.config;

import com.uip.backend.model.*;
import com.uip.backend.repository.*;
import com.uip.backend.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @Autowired
    private SavingsPlanRepository savingsPlanRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private PortfolioService portfolioService;

    @Override
    public void run(String... args) throws Exception {
        // Seed Admin user if not exists
        if (userRepository.findByEmail("admin@uip.com").isEmpty()) {
            User admin = User.builder()
                    .name("System Administrator")
                    .email("admin@uip.com")
                    .password("admin123")
                    .phone("+91 98111 22334")
                    .riskProfile("MODERATE")
                    .role("ADMIN")
                    .currency("INR")
                    .cibilScore(820)
                    .createdAt(LocalDateTime.now().minusMonths(12))
                    .build();
            userRepository.save(admin);
        }

        // Seed Demo User (demo@example.com / password)
        if (userRepository.findByEmail("demo@example.com").isEmpty()) {
            User demo = User.builder()
                    .name("Demo User")
                    .email("demo@example.com")
                    .password("password")
                    .phone("+91 98000 11122")
                    .riskProfile("MODERATE")
                    .role("CLIENT")
                    .currency("INR")
                    .cibilScore(765)
                    .createdAt(LocalDateTime.now().minusMonths(3))
                    .build();
            userRepository.save(demo);
        }

        // Seed Client 1: Rahul Sharma (Complete Diverse FinTech Portfolio)
        User rahul = userRepository.findByEmail("rahul@uip.com").orElse(null);
        if (rahul == null) {
            rahul = User.builder()
                    .name("Rahul Sharma")
                    .email("rahul@uip.com")
                    .password("client123")
                    .phone("+91 98765 43210")
                    .riskProfile("MODERATE")
                    .role("CLIENT")
                    .currency("INR")
                    .createdAt(LocalDateTime.now().minusMonths(6))
                    .build();
            rahul = userRepository.save(rahul);

            // Portfolio 1: Core Growth Portfolio
            Portfolio p1 = Portfolio.builder()
                    .user(rahul)
                    .name("Wealth Builder Portfolio")
                    .totalValue(0.0)
                    .build();
            p1 = portfolioRepository.save(p1);

            // Add Holdings
            addInvestment(p1, "RELIANCE", "STOCK", 25.0, 2850.0, 2985.40, null, null, null, "Bluechip Oil-to-Telecom giant");
            addInvestment(p1, "TCS", "STOCK", 15.0, 3900.0, 4210.80, null, null, null, "IT Services Leader");
            addInvestment(p1, "TATAMOTORS", "STOCK", 80.0, 880.0, 1045.75, null, null, null, "Automotive & EV pioneer");
            addInvestment(p1, "HDFCBANK", "STOCK", 40.0, 1520.0, 1645.25, null, null, null, "Banking Leader");
            addInvestment(p1, "Parag Parikh Flexi Cap Fund", "MUTUAL_FUND", 1200.0, 72.50, 84.62, null, null, 10000.0, "Monthly SIP active on 5th");
            addInvestment(p1, "GOLD BEES ETF", "GOLD", 1000.0, 58.20, 64.80, null, null, null, "Gold ETF hedge");
            addInvestment(p1, "SGB (Sovereign Gold Bond)", "BOND", 35.0, 6100.0, 7180.0, 2.5, LocalDate.now().plusYears(4), null, "RBI Sovereign Gold Bond");

            portfolioService.recalculatePortfolioValue(p1.getId());

            // Portfolio 2: Safe Fixed Income & FD
            Portfolio p2 = Portfolio.builder()
                    .user(rahul)
                    .name("Fixed Income & Deposits")
                    .totalValue(0.0)
                    .build();
            p2 = portfolioRepository.save(p2);
            addInvestment(p2, "HDFC 3-Year Fixed Deposit", "FD", 1.0, 300000.0, 321300.0, 7.1, LocalDate.now().plusYears(2), null, "Quarterly compounding FD");
            portfolioService.recalculatePortfolioValue(p2.getId());

            // Incomes & Expenses (Cash Flow)
            addCashFlow(rahul, "INCOME", "Salary", 145000.0, LocalDate.now().minusDays(3), "Tech Lead Monthly Salary", "Net Banking");
            addCashFlow(rahul, "INCOME", "Freelance", 35000.0, LocalDate.now().minusDays(12), "System Consulting Honorarium", "UPI");
            addCashFlow(rahul, "INCOME", "Dividend", 4800.0, LocalDate.now().minusDays(18), "TCS Interim Dividend", "Net Banking");

            addCashFlow(rahul, "EXPENSE", "Rent & Housing", 28000.0, LocalDate.now().minusDays(4), "Apartment monthly rent", "Net Banking");
            addCashFlow(rahul, "EXPENSE", "Food & Dining", 14200.0, LocalDate.now().minusDays(6), "Groceries & weekend dining", "UPI");
            addCashFlow(rahul, "EXPENSE", "Bills & Utilities", 6400.0, LocalDate.now().minusDays(8), "Electricity, Water, Broadband", "UPI");
            addCashFlow(rahul, "EXPENSE", "Shopping", 8900.0, LocalDate.now().minusDays(11), "Clothing & tech accessories", "Credit Card");
            addCashFlow(rahul, "EXPENSE", "Travel & Fuel", 5300.0, LocalDate.now().minusDays(14), "Petrol and Toll payments", "UPI");
            addCashFlow(rahul, "EXPENSE", "Healthcare", 3200.0, LocalDate.now().minusDays(16), "Annual checkup & medicines", "Credit Card");

            // Savings Plan (Exact requirement: ₹50,000 initial, ₹5,000 monthly addition, 7% interest, 2 years)
            SavingsPlan sp = SavingsPlan.builder()
                    .user(rahul)
                    .accountName("High-Yield Wealth Savings")
                    .initialDeposit(50000.0)
                    .monthlyAddition(5000.0)
                    .interestRate(7.0)
                    .periodYears(2)
                    .compoundingFrequency("MONTHLY")
                    .startDate(LocalDate.now().minusMonths(2))
                    .notes("Primary compounding liquid wealth reserve")
                    .build();
            savingsPlanRepository.save(sp);

            // Active Loan: Car Loan
            double carEmi = 15420.0;
            Loan carLoan = Loan.builder()
                    .user(rahul)
                    .loanName("HDFC Car Loan - Creta")
                    .loanType("CAR")
                    .principalAmount(750000.0)
                    .interestRate(8.6)
                    .tenureMonths(60)
                    .emiAmount(carEmi)
                    .amountPaid(185040.0)
                    .startDate(LocalDate.now().minusMonths(12))
                    .nextEmiDate(LocalDate.now().plusDays(10))
                    .status("ACTIVE")
                    .build();
            loanRepository.save(carLoan);

            // Goals
            Goal g1 = Goal.builder()
                    .portfolio(p1)
                    .goalName("Emergency Reserve Fund")
                    .targetAmount(200000.0)
                    .targetDate(LocalDate.now().plusMonths(8))
                    .status("ACTIVE")
                    .build();
            goalRepository.save(g1);

            Goal g2 = Goal.builder()
                    .portfolio(p1)
                    .goalName("Europe Family Vacation")
                    .targetAmount(300000.0)
                    .targetDate(LocalDate.now().plusMonths(14))
                    .status("ACTIVE")
                    .build();
            goalRepository.save(g2);

            Goal g3 = Goal.builder()
                    .portfolio(p1)
                    .goalName("Apartment Down Payment")
                    .targetAmount(1000000.0)
                    .targetDate(LocalDate.now().plusMonths(36))
                    .status("ACTIVE")
                    .build();
            goalRepository.save(g3);
        }

        // Seed Client 2: Priya Patel (Demonstrates clean Client A vs Client B isolation)
        User priya = userRepository.findByEmail("priya@uip.com").orElse(null);
        if (priya == null) {
            priya = User.builder()
                    .name("Priya Patel")
                    .email("priya@uip.com")
                    .password("client123")
                    .phone("+91 97654 32109")
                    .riskProfile("AGGRESSIVE")
                    .role("CLIENT")
                    .currency("INR")
                    .createdAt(LocalDateTime.now().minusMonths(3))
                    .build();
            priya = userRepository.save(priya);

            Portfolio pPriya = Portfolio.builder()
                    .user(priya)
                    .name("Priya Alpha Tech Portfolio")
                    .totalValue(0.0)
                    .build();
            pPriya = portfolioRepository.save(pPriya);

            addInvestment(pPriya, "INFY", "STOCK", 50.0, 1680.0, 1825.60, null, null, null, "Tech equity");
            addInvestment(pPriya, "Mirae Asset Large Cap Fund", "MUTUAL_FUND", 800.0, 98.0, 112.35, null, null, 5000.0, "Large cap equity fund");
            portfolioService.recalculatePortfolioValue(pPriya.getId());

            addCashFlow(priya, "INCOME", "Salary", 95000.0, LocalDate.now().minusDays(2), "Product Designer Salary", "Net Banking");
            addCashFlow(priya, "EXPENSE", "Rent & Housing", 22000.0, LocalDate.now().minusDays(3), "Studio apartment", "UPI");
            addCashFlow(priya, "EXPENSE", "Food & Dining", 9500.0, LocalDate.now().minusDays(5), "Dining & Swiggy", "UPI");
            addCashFlow(priya, "EXPENSE", "Shopping", 7000.0, LocalDate.now().minusDays(7), "Fashion & books", "Credit Card");

            SavingsPlan spPriya = SavingsPlan.builder()
                    .user(priya)
                    .accountName("Priya Tech Growth Savings")
                    .initialDeposit(80000.0)
                    .monthlyAddition(10000.0)
                    .interestRate(7.2)
                    .periodYears(3)
                    .compoundingFrequency("MONTHLY")
                    .startDate(LocalDate.now().minusMonths(1))
                    .notes("Higher yield liquid fund")
                    .build();
            savingsPlanRepository.save(spPriya);

            Goal gPriya = Goal.builder()
                    .portfolio(pPriya)
                    .goalName("Higher Education MS Degree")
                    .targetAmount(500000.0)
                    .targetDate(LocalDate.now().plusMonths(18))
                    .status("ACTIVE")
                    .build();
            goalRepository.save(gPriya);
        }

        // Seed sample insurance policies for Rahul Sharma if not present
        User rahulUser = userRepository.findByEmail("rahul@uip.com").orElse(null);
        if (rahulUser != null && insuranceRepository.findByUserId(rahulUser.getId()).isEmpty()) {
            InsurancePolicy termLife = InsurancePolicy.builder()
                    .user(rahulUser)
                    .policyName("HDFC Life Click 2 Protect 3D Plus")
                    .policyType("LIFE_TERM")
                    .provider("HDFC Life")
                    .policyNumber("HDFC-TERM-884910")
                    .sumAssured(15000000.0) // 1.5 Crore Term Cover
                    .premiumAmount(16500.0)
                    .premiumFrequency("ANNUAL")
                    .startDate(LocalDate.now().minusMonths(11))
                    .renewalDate(LocalDate.now().plusDays(24)) // Due in 24 days!
                    .expiryDate(LocalDate.now().plusYears(29))
                    .nominee("Ananya Sharma (Spouse)")
                    .status("DUE_SOON")
                    .taxBenefitSection("80C")
                    .notes("Comprehensive pure term life coverage for family security")
                    .build();
            insuranceRepository.save(termLife);

            InsurancePolicy health = InsurancePolicy.builder()
                    .user(rahulUser)
                    .policyName("Star Health Comprehensive Family Floater")
                    .policyType("HEALTH")
                    .provider("Star Health")
                    .policyNumber("STAR-MED-339102")
                    .sumAssured(1500000.0) // 15 Lakh Health Cover
                    .premiumAmount(21400.0)
                    .premiumFrequency("ANNUAL")
                    .startDate(LocalDate.now().minusMonths(6))
                    .renewalDate(LocalDate.now().plusMonths(6))
                    .expiryDate(LocalDate.now().plusYears(1))
                    .nominee("Self & Spouse & Dependents")
                    .status("ACTIVE")
                    .taxBenefitSection("80D")
                    .notes("Covers cashless hospitalization across 14,000+ network hospitals")
                    .build();
            insuranceRepository.save(health);

            InsurancePolicy car = InsurancePolicy.builder()
                    .user(rahulUser)
                    .policyName("ICICI Lombard Zero-Dep Car Comprehensive")
                    .policyType("MOTOR")
                    .provider("ICICI Lombard")
                    .policyNumber("ICICI-MOT-551920")
                    .sumAssured(850000.0) // Creta IDV
                    .premiumAmount(12800.0)
                    .premiumFrequency("ANNUAL")
                    .startDate(LocalDate.now().minusMonths(4))
                    .renewalDate(LocalDate.now().plusMonths(8))
                    .expiryDate(LocalDate.now().plusMonths(8))
                    .nominee("Rahul Sharma")
                    .status("ACTIVE")
                    .taxBenefitSection("NONE")
                    .notes("Bumper to Bumper comprehensive with engine protection & RSA")
                    .build();
            insuranceRepository.save(car);
        }
    }

    private void addInvestment(Portfolio portfolio, String assetName, String assetType, Double qty, Double buyPrice, Double curPrice,
                               Double interestRate, LocalDate maturityDate, Double sipAmount, String notes) {
        Investment inv = Investment.builder()
                .portfolio(portfolio)
                .assetName(assetName)
                .assetType(assetType)
                .quantity(qty)
                .buyPrice(buyPrice)
                .investedAmount(qty * buyPrice)
                .currentPrice(curPrice)
                .interestRate(interestRate)
                .maturityDate(maturityDate)
                .sipMonthlyAmount(sipAmount)
                .notes(notes)
                .build();
        inv = investmentRepository.save(inv);

        Transaction tx = Transaction.builder()
                .portfolio(portfolio)
                .investment(inv)
                .buySell("BUY")
                .quantity(qty)
                .price(buyPrice)
                .transactionDate(LocalDateTime.now().minusMonths(1))
                .build();
        transactionRepository.save(tx);
    }

    private void addCashFlow(User user, String type, String category, Double amount, LocalDate date, String desc, String mode) {
        CashFlow cf = CashFlow.builder()
                .user(user)
                .type(type)
                .category(category)
                .amount(amount)
                .flowDate(date)
                .description(desc)
                .paymentMode(mode)
                .build();
        cashFlowRepository.save(cf);
    }
}
