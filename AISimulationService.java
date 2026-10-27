package com.uip.backend.service;

import com.uip.backend.model.CashFlow;
import com.uip.backend.model.Goal;
import com.uip.backend.model.Investment;
import com.uip.backend.model.Loan;
import com.uip.backend.model.Portfolio;
import com.uip.backend.model.SavingsPlan;
import com.uip.backend.model.User;
import com.uip.backend.repository.CashFlowRepository;
import com.uip.backend.repository.GoalRepository;
import com.uip.backend.repository.InvestmentRepository;
import com.uip.backend.repository.LoanRepository;
import com.uip.backend.repository.PortfolioRepository;
import com.uip.backend.repository.SavingsPlanRepository;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AISimulationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @Autowired
    private SavingsPlanRepository savingsPlanRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private com.uip.backend.repository.InsuranceRepository insuranceRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateAIResponse(Long userId, String userMessage, String geminiApiKey) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if (user == null) {
            return "Hello! I am your Unified Wealth AI Advisor. Please log in to your account so I can analyze your portfolio, cash flows, and investments.";
        }

        // 1. Portfolios & Holdings context
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        double totalPortfolioValue = 0.0;
        StringBuilder holdingsCtx = new StringBuilder();
        for (Portfolio p : portfolios) {
            totalPortfolioValue += p.getTotalValue() != null ? p.getTotalValue() : 0.0;
            List<Investment> holdings = investmentRepository.findByPortfolioId(p.getId());
            for (Investment h : holdings) {
                if (h.getQuantity() > 0) {
                    holdingsCtx.append("- ").append(h.getAssetName())
                            .append(" (").append(h.getAssetType()).append("): ")
                            .append(h.getQuantity()).append(" units @ ₹")
                            .append(String.format("%.2f", h.getCurrentPrice()))
                            .append(" (Value: ₹").append(String.format("%.2f", h.getQuantity() * h.getCurrentPrice())).append(")\n");
                }
            }
        }

        // 2. Cash Flow (Income vs Expenses)
        List<CashFlow> flows = cashFlowRepository.findByUserIdOrderByFlowDateDesc(userId);
        double totalIncome = 0.0;
        double totalExpense = 0.0;
        for (CashFlow cf : flows) {
            if ("INCOME".equalsIgnoreCase(cf.getType())) {
                totalIncome += cf.getAmount();
            } else if ("EXPENSE".equalsIgnoreCase(cf.getType())) {
                totalExpense += cf.getAmount();
            }
        }
        double netSavings = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100.0 : 0.0;

        // 3. Savings Plans
        List<SavingsPlan> savingsPlans = savingsPlanRepository.findByUserId(userId);
        StringBuilder savingsCtx = new StringBuilder();
        double totalSavingsDeposits = 0.0;
        for (SavingsPlan sp : savingsPlans) {
            double deposit = sp.getInitialDeposit() != null ? sp.getInitialDeposit() : 0.0;
            double monthly = sp.getMonthlyAddition() != null ? sp.getMonthlyAddition() : 0.0;
            totalSavingsDeposits += deposit;
            savingsCtx.append("- Plan '").append(sp.getAccountName()).append("': Initial Deposit ₹")
                    .append(String.format("%.2f", deposit)).append(", Monthly Addition ₹")
                    .append(String.format("%.2f", monthly)).append(" @ ")
                    .append(sp.getInterestRate()).append("% p.a.\n");
        }

        // 4. Loans & EMIs
        List<Loan> loans = loanRepository.findByUserId(userId);
        StringBuilder loansCtx = new StringBuilder();
        double totalLoanDebt = 0.0;
        double totalMonthlyEmi = 0.0;
        for (Loan l : loans) {
            if ("ACTIVE".equalsIgnoreCase(l.getStatus())) {
                double principal = l.getPrincipalAmount() != null ? l.getPrincipalAmount() : 0.0;
                double paid = l.getAmountPaid() != null ? l.getAmountPaid() : 0.0;
                double remaining = Math.max(0.0, principal - paid);
                double emi = l.getEmiAmount() != null ? l.getEmiAmount() : 0.0;
                totalLoanDebt += remaining;
                totalMonthlyEmi += emi;
                loansCtx.append("- Loan '").append(l.getLoanName()).append("': Outstanding ₹")
                        .append(String.format("%.2f", remaining)).append(", EMI: ₹")
                        .append(String.format("%.2f", emi)).append("/mo @ ")
                        .append(l.getInterestRate()).append("%\n");
            }
        }

        // 5. Goals
        StringBuilder goalsCtx = new StringBuilder();
        for (Portfolio p : portfolios) {
            List<Goal> goals = goalRepository.findByPortfolioId(p.getId());
            for (Goal g : goals) {
                goalsCtx.append("- Goal '").append(g.getGoalName()).append("': Target ₹")
                        .append(String.format("%.2f", g.getTargetAmount())).append(" by ").append(g.getTargetDate())
                        .append(" (Status: ").append(g.getStatus()).append(")\n");
            }
        }

        String riskProfile = user.getRiskProfile() != null ? user.getRiskProfile() : "MODERATE";

        // If live Gemini API Key is provided, call Google Generative AI
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            try {
                return callGeminiAPI(user.getName(), riskProfile, totalPortfolioValue, totalIncome, totalExpense,
                        savingsRate, totalLoanDebt, totalMonthlyEmi, holdingsCtx.toString(), savingsCtx.toString(),
                        loansCtx.toString(), goalsCtx.toString(), userMessage, geminiApiKey);
            } catch (Exception e) {
                System.err.println("Gemini API call failed, using intelligent local simulation: " + e.getMessage());
            }
        }

        // Intelligent Built-In Financial Engine
        return getLocalSimulationResponse(user.getName(), riskProfile, totalPortfolioValue, totalIncome, totalExpense,
                savingsRate, totalLoanDebt, totalMonthlyEmi, totalSavingsDeposits, holdingsCtx.toString(),
                savingsCtx.toString(), loansCtx.toString(), goalsCtx.toString(), userMessage);
    }

    private String callGeminiAPI(String userName, String riskProfile, double portfolioVal, double income, double expense,
                                 double savingsRate, double debt, double monthlyEmi, String holdings, String savings,
                                 String loans, String goals, String message, String apiKey) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "You are an expert AI Financial Advisor for Unified Investment Platform (an Indian Personal Wealth & Investment Management Platform). " +
                "You are counseling " + userName + ". " +
                "Primary Currency: Indian Rupee (₹ INR).\n\n" +
                "CLIENT FINANCIAL DOSSIER:\n" +
                "- Risk Profile: " + riskProfile + "\n" +
                "- Total Portfolio Investment: ₹" + String.format("%.2f", portfolioVal) + "\n" +
                "- Monthly Inflow: ₹" + String.format("%.2f", income) + " | Outflow: ₹" + String.format("%.2f", expense) + " (Savings Rate: " + String.format("%.1f", savingsRate) + "%)\n" +
                "- Total Debt: ₹" + String.format("%.2f", debt) + " | Monthly EMI Commitment: ₹" + String.format("%.2f", monthlyEmi) + "\n" +
                "- Holdings:\n" + (holdings.isEmpty() ? "No current holdings." : holdings) + "\n" +
                "- Savings & Deposits:\n" + (savings.isEmpty() ? "No active plans." : savings) + "\n" +
                "- Active Loans:\n" + (loans.isEmpty() ? "Debt free." : loans) + "\n" +
                "- Financial Goals:\n" + (goals.isEmpty() ? "None set." : goals) + "\n\n" +
                "Instructions:\n" +
                "1. Answer in a professional, encouraging, advisory tone using ₹ INR formatting.\n" +
                "2. Reference Indian financial instruments where relevant (e.g. Nifty 50, Sensex, SIP, Mutual Funds, Sovereign Gold Bonds, PPF, FDs).\n" +
                "3. Use structured formatting with bold headings and bullet points.\n" +
                "4. Keep recommendations actionable, practical, and safe.";

        Map<String, Object> contents = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", systemPrompt + "\n\nUser Prompt: " + message);
        contents.put("parts", new Object[]{parts});

        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", new Object[]{contents});

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                List candidates = (List) response.getBody().get("candidates");
                Map candidate = (Map) candidates.get(0);
                Map content = (Map) candidate.get("content");
                List partsList = (List) content.get("parts");
                Map part = (Map) partsList.get(0);
                return (String) part.get("text");
            } catch (Exception e) {
                return "Error parsing Gemini response: " + e.getMessage();
            }
        }
        return "Failed to contact Gemini service (Status: " + response.getStatusCode() + ").";
    }

    private String getLocalSimulationResponse(String userName, String riskProfile, double portfolioVal, double income,
                                              double expense, double savingsRate, double debt, double monthlyEmi,
                                              double savingsDeposits, String holdings, String savings, String loans,
                                              String goals, String message) {
        String msg = message.toLowerCase().trim();

        // 1. Greetings
        if (msg.contains("hello") || msg.contains("hi") || msg.contains("hey") || msg.contains("who are you")) {
            return "### Welcome, " + userName + "! 👋\n\n" +
                    "I am your **Unified Wealth AI Advisor**, actively monitoring your financial health across all 7 core modules:\n\n" +
                    "- **Portfolio Value:** ₹" + String.format("%,.2f", portfolioVal) + "\n" +
                    "- **Monthly Cash Flow:** ₹" + String.format("%,.2f", income) + " Inflow / ₹" + String.format("%,.2f", expense) + " Outflow (" + String.format("%.1f", savingsRate) + "% Savings Rate)\n" +
                    "- **Active Debt / EMI:** ₹" + String.format("%,.2f", debt) + " (EMI: ₹" + String.format("%,.2f", monthlyEmi) + "/mo)\n" +
                    "- **Assessed Risk Profile:** " + riskProfile + "\n\n" +
                    "**How can I assist you today?**\n" +
                    "- 📊 *\"Analyze my portfolio risk & asset weights\"*\n" +
                    "- 💵 *\"Review my cash flow & monthly savings\"*\n" +
                    "- 🏦 *\"What is my loan & EMI health?\"*\n" +
                    "- 🚀 *\"Recommend investments for my profile\"*\n" +
                    "- 🎯 *\"How are my financial goals progressing?\"*\n" +
                    "- 💡 *\"Explain SIP compounding or Sovereign Gold Bonds\"*";
        }

        // 2. Cash Flow & Budgeting
        if (msg.contains("cash flow") || msg.contains("income") || msg.contains("expense") || msg.contains("budget") || msg.contains("saving rate") || msg.contains("savings")) {
            double surplus = income - expense;
            return "### 💳 Monthly Cash Flow & Budget Analysis for " + userName + "\n\n" +
                    "- **Monthly Inflow (Income):** ₹" + String.format("%,.2f", income) + "\n" +
                    "- **Monthly Outflow (Expenses):** ₹" + String.format("%,.2f", expense) + "\n" +
                    "- **Net Surplus:** ₹" + String.format("%,.2f", surplus) + "\n" +
                    "- **Current Savings Rate:** **" + String.format("%.1f", savingsRate) + "%** " + (savingsRate >= 40.0 ? "🌟 (Excellent!)" : savingsRate >= 20.0 ? "✅ (Healthy)" : "⚠️ (Needs Optimization)") + "\n\n" +
                    "**AI Strategic Insights:**\n" +
                    "- **The 50/30/20 Benchmark:** Ideally, 50% goes to Needs (rent, EMI, food), 30% to Wants (dining, shopping), and 20%+ to Wealth Building (SIPs, Stocks, FDs).\n" +
                    "- With your surplus of **₹" + String.format("%,.2f", surplus) + "/month**, you can comfortably deploy ₹" + String.format("%,.2f", surplus * 0.7) + " into systematic monthly SIPs and keep ₹" + String.format("%,.2f", surplus * 0.3) + " for emergency liquidity.";
        }

        // 3. Loans & EMI
        if (msg.contains("loan") || msg.contains("emi") || msg.contains("debt") || msg.contains("afford") || msg.contains("borrow")) {
            double dti = income > 0 ? (monthlyEmi / income) * 100.0 : 0.0;
            return "### 🏦 Loan & EMI Debt Profile for " + userName + "\n\n" +
                    (loans.isEmpty() ? 
                        "🎉 **Congratulations!** You currently have **zero active loans**. You are 100% debt-free, giving you maximum leverage to channel your surplus into high-growth wealth building!" :
                        "- **Total Outstanding Principal:** ₹" + String.format("%,.2f", debt) + "\n" +
                        "- **Total Monthly EMI Outflow:** ₹" + String.format("%,.2f", monthlyEmi) + "\n" +
                        "- **Debt-to-Income (DTI) Ratio:** **" + String.format("%.1f", dti) + "%** " + (dti <= 35.0 ? "✅ (Safe & manageable)" : "⚠️ (High EMI burden)") + "\n\n" +
                        "**Active Loan Facilities:**\n" + loans + "\n" +
                        "**AI Advisory Strategy:**\n" +
                        "- Aim to keep your total EMI commitment below **40%** of net income.\n" +
                        "- Consider prepaying 1 extra EMI per year or making partial prepayments toward higher-interest facilities to dramatically reduce your total interest payoff.");
        }

        // 4. Portfolio & Risk
        if (msg.contains("portfolio") || msg.contains("risk") || msg.contains("holding") || msg.contains("invest") || msg.contains("allocation") || msg.contains("rebalance")) {
            if (portfolioVal <= 0 && holdings.isEmpty()) {
                return "### Portfolio Status\n\nYour portfolio is currently empty. Head over to the **Investment Manager** page and log your first transaction (such as Nifty 50 ETF, HDFC Bank, or Digital Gold). Once logged, I will provide real-time sector diversification and asset allocation rebalancing insights!";
            }

            return "### 📈 Portfolio Allocation & Risk Assessment for " + userName + "\n\n" +
                    "- **Total Portfolio Valuation:** **₹" + String.format("%,.2f", portfolioVal) + "**\n" +
                    "- **Client Risk Profile:** **" + riskProfile + "**\n\n" +
                    "**Active Holdings:**\n" + (holdings.isEmpty() ? "No active positions." : holdings) + "\n" +
                    "**Asset Allocation Recommendations:**\n" +
                    (riskProfile.equalsIgnoreCase("CONSERVATIVE") ? 
                        "- **Target Mix:** 20% Large-Cap Equities, 60% Debt/Fixed Deposits, 20% Sovereign Gold Bonds / Digital Gold.\n- Focus on steady capital preservation with predictable annual yields." :
                        riskProfile.equalsIgnoreCase("AGGRESSIVE") ? 
                        "- **Target Mix:** 75% Equities (Nifty 50 + Midcaps), 15% Digital Gold, 10% Liquid Buffer.\n- Capitalize on India's GDP expansion with high long-term compounding tolerance." :
                        "- **Target Mix:** 55% Equities (Index Funds & Bluechips), 30% Debt/FDs, 15% Digital Gold.\n- Optimal balanced strategy providing steady growth with downside cushioning.") +
                    "\n\nNavigate to the **Risk Analysis** tab to see your visual allocation radar and direct rebalance trade suggestions!";
        }

        // 5. Goals
        if (msg.contains("goal") || msg.contains("target") || msg.contains("retirement") || msg.contains("house") || msg.contains("car") || msg.contains("wedding")) {
            if (goals.isEmpty()) {
                return "### 🎯 Financial Goals Planner\n\nYou haven't created any financial milestones yet! Go to the **Financial Goals** tab and set targets (e.g. *Retirement Corpus*, *Emergency Fund*, *New Apartment*, or *Higher Education*). I will calculate your required monthly SIP to achieve them right on schedule!";
            }

            return "### 🎯 Active Financial Goals for " + userName + "\n\n" +
                    goals + "\n" +
                    "**AI Strategic Guidance:**\n" +
                    "- **Short-term Goals (< 3 years):** Protect capital in Fixed Deposits or Liquid Funds yielding 6.5% - 7.5%.\n" +
                    "- **Long-term Goals (5+ years):** Fuel with disciplined monthly SIPs in diversified Equity Index Funds (Nifty 50, Sensex) historically generating 12-14% CAGR.\n" +
                    "- Always step up your SIP contributions by 10% each year with your annual salary appraisals to hit targets 2 to 3 years early!";
        }

        // 6. Asset Recommendations
        if (msg.contains("recommend") || msg.contains("what should i buy") || msg.contains("suggest") || msg.contains("advice") || msg.contains("best stock") || msg.contains("best fund")) {
            if (riskProfile.equalsIgnoreCase("CONSERVATIVE")) {
                return "### 🛡️ Recommended Assets: Conservative Investor\n\n" +
                        "Tailored for capital security and inflation-beating steady yields:\n\n" +
                        "1. **Sovereign Gold Bonds (SGB)**: 2.5% annual assured payout + gold appreciation + 100% tax-free capital gains at maturity.\n" +
                        "2. **HDFC Bank / ICICI Bank**: High-quality Indian banking anchors with solid balance sheets and consistent compounding.\n" +
                        "3. **High-Yield Fixed Deposits (7.25% - 7.5%)**: Guaranteed capital protection.\n" +
                        "4. **Bharat Bond / Corporate Bond Funds**: Predictable AAA-rated bond yields with minimal volatility.";
            } else if (riskProfile.equalsIgnoreCase("AGGRESSIVE")) {
                return "### 🚀 Recommended Assets: Aggressive Investor\n\n" +
                        "Tailored for maximum long-term capital compounding:\n\n" +
                        "1. **Parag Parikh Flexi Cap Fund**: Top-performing multi-cap fund investing in Indian and global market leaders.\n" +
                        "2. **Tata Motors / Reliance Industries**: Leaders in EV transition, telecom, retail, and renewable energy.\n" +
                        "3. **Nifty Midcap 150 ETF**: Instant exposure to India's fastest expanding mid-tier enterprises.\n" +
                        "4. **Digital Gold (Augmont/MMTC-PAMP)**: 24K 99.9% pure gold hedge with 100% liquidity.";
            } else {
                return "### ⚖️ Recommended Assets: Moderate Balanced Investor\n\n" +
                        "Tailored for balanced capital appreciation with calculated risk:\n\n" +
                        "1. **Nippon India Nifty 50 BeES ETF**: Cost-effective diversification across India's top 50 corporate giants.\n" +
                        "2. **Reliance Industries & Infosys**: Core bluechip foundation with steady dividend and capital returns.\n" +
                        "3. **Digital Gold (5-10% allocation)**: Strong hedge against currency depreciation and geopolitical volatility.\n" +
                        "4. **Recurring Compounding Deposit (7.0% p.a.)**: Steady fixed income bedrock for liquidity.";
            }
        }

        // 7. Educational Concepts
        if (msg.contains("sip") || msg.contains("compounding") || msg.contains("rule of 72")) {
            return "### 📈 The Power of Systematic Investment Plans (SIP) & Compounding\n\n" +
                    "- **What is a SIP?** Rather than timing market peaks and troughs, you invest a fixed sum (e.g. ₹5,000/month) at set intervals. During market dips, you acquire more units; during rallies, your accumulated units swell in value (**Rupee Cost Averaging**).\n\n" +
                    "- **The Rule of 72:** Divide 72 by your expected annual interest rate to find out how many years it takes to double your capital!\n" +
                    "  - At 7.2% FD rate: 72 / 7.2 = **10 Years to double**.\n" +
                    "  - At 12% Equity Index rate: 72 / 12 = **6 Years to double**.\n\n" +
                    "- **Example:** Investing ₹10,000/month for 15 years at 12% yields **₹50,45,760** on an invested principal of just ₹18,00,000!";
        }

        // 6. Insurance & Protection
        if (msg.contains("insurance") || msg.contains("policy") || msg.contains("term") || msg.contains("mediclaim") || msg.contains("health cover") || msg.contains("life cover") || msg.contains("protect")) {
            return "### 🛡️ Insurance & Protection Portfolio Analysis for " + userName + "\n\n" +
                    "- **Rule of Thumb (Human Life Value):** Your Life Cover should be at least **10x to 15x your annual income** plus any active loan debt.\n" +
                    "- **Health Insurance Benchmark:** A family floater of at least **₹15,00,000** to shield your wealth from modern hospitalization inflation.\n\n" +
                    "**Key Recommendations:**\n" +
                    "1. **Pure Term Life:** Choose pure term insurance over endowment/ULIPs for maximum sum assured at minimal annual premium.\n" +
                    "2. **Separate Health Cover:** Never rely solely on corporate employer insurance; keep a personal health policy to ensure continuity during job transitions.\n" +
                    "3. **Tax Deductions:** Term Life premiums qualify for deductions under **Section 80C** (up to ₹1.5L), while Health Insurance premiums qualify under **Section 80D** (up to ₹25,000 for self/family and ₹50,000 for senior parents).";
        }

        // 7. Income Tax & 80C/80D Optimization
        if (msg.contains("tax") || msg.contains("80c") || msg.contains("80d") || msg.contains("regime") || msg.contains("deduction") || msg.contains("save tax")) {
            return "### 🧾 Indian Income Tax Planning & 80C/80D Optimizer\n\n" +
                    "**New vs Old Tax Regime Quick Guide:**\n" +
                    "- **New Tax Regime (Budget 2024-2025):**\n" +
                    "  - Standard Deduction: **₹75,000**.\n" +
                    "  - Full tax rebate under Section 87A for net taxable income up to **₹7,00,000** (Zero Tax!).\n" +
                    "  - Lower slab rates, but no 80C, 80D, or HRA deductions.\n\n" +
                    "- **Old Tax Regime:**\n" +
                    "  - Beneficial if you claim significant deductions: **Section 80C (up to ₹1.5L)**, **Section 80D (Health Insurance up to ₹50k)**, **Home Loan Interest (Section 24b up to ₹2L)**, and HRA.\n\n" +
                    "💡 *Pro-Tip: Visit our **Tax Planner & 80C/80D** tab to see your exact rupee savings calculated side-by-side!*";
        }

        if (msg.contains("sgb") || msg.contains("gold") || msg.contains("digital gold")) {
            return "### 🏆 Sovereign Gold Bonds (SGB) vs Digital Gold\n\n" +
                    "- **Sovereign Gold Bonds (SGB):**\n" +
                    "  - Issued by the Reserve Bank of India (RBI) on behalf of the Government.\n" +
                    "  - Pays **2.50% annual interest** credited directly into your bank account.\n" +
                    "  - **Zero Making Charges & Zero Storage Fees**.\n" +
                    "  - **100% Tax-Exempt** capital gains if held until maturity (8 years).\n\n" +
                    "- **Digital Gold:**\n" +
                    "  - Backed by 24K 99.9% pure gold stored in insured vaults.\n" +
                    "  - Buy or sell instantly 24/7 starting from just ₹100.\n" +
                    "  - Great for short-to-medium term flexibility.";
        }

        if (msg.contains("etf") || msg.contains("mutual fund") || msg.contains("stock")) {
            return "### 📚 Asset Classes Explained: Stocks vs ETFs vs Mutual Funds\n\n" +
                    "- **Direct Equity (Stocks):** Direct fractional ownership in a specific company (e.g. Reliance, TCS). Highest potential upside, but concentrated company-specific risk.\n" +
                    "- **ETFs (Exchange Traded Funds):** A diversified basket tracking an entire index (like Nifty 50 BeES). Trades in real-time on NSE/BSE just like a regular share with rock-bottom expense ratios (< 0.1%).\n" +
                    "- **Mutual Funds:** Professionally managed pooled vehicles where fund managers select securities according to an investment mandate (Large-cap, Flexi-cap, Liquid, etc.).";
        }

        // Generic helpful response
        return "### Unified Wealth AI Financial Advisor\n\n" +
                "I am here to guide your personal finances and investments. What specific area would you like to explore?\n\n" +
                "1. **Portfolio Risk & Holdings:** Asset weights, rebalancing, and sector analysis.\n" +
                "2. **Cash Flow & Budgeting:** Income vs expenses and optimal savings rate.\n" +
                "3. **Loans & EMIs:** Debt health, amortization schedule, and prepayment advice.\n" +
                "4. **Insurance & Protection:** Coverage adequacy, term life, and health policies.\n" +
                "5. **Income Tax & 80C/80D:** Old vs New regime comparison and deductions.\n" +
                "6. **Goal Tracking:** Milestone projections and monthly SIP requirements.";
    }
}
