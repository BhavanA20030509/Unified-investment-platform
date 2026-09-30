Unified Investment Platform (UIP)
A full‑stack personal finance & investment manager that lets clients view, plan, and act on all aspects of their financial life in one place.
Built with Spring Boot (Java 17) + SQLite on the back‑end and React + Vite + Chart.js on the front‑end, the application demonstrates a real‑world end‑to‑end workflow covering:

Domain	Core Features
Accounts & Profiles	Secure user registration, role‑based UI (admin vs client), JWT‑based authentication.
Cash Flow & Budgeting	Income/expense tracking, automated categorisation, monthly & yearly cash‑flow statements.
Goal & Portfolio Management	Create/track financial goals (retirement, travel, education), simulate portfolio growth, edit target dates, and visualise progress.
Investment Manager	Browse & select equities (e.g., TCS, Reliance), place simulated buy‑orders, view pending orders and a transaction history (future‑ready for real‑broker integration).
Protection & Tax Optimizer	Add life/health insurance policies, automatically compute 80C/80D deductions, surface renewal alerts, and show total cover vs. required protection.
AI Advisor	Conversational chatbot (placeholder) that can answer finance‑related questions, explain tax benefits, and suggest goal‑optimisation strategies.
Admin Console	Dashboard for monitoring overall platform health, user metrics, and audit logs (admin UI hidden from clients).
Insurance & Nominee Support	Full CRUD for insurance policies with a dedicated nominee field, summed coverage, and quick‑action buttons from the main dashboard.
Savings & Interest Calculator	Visualise projected savings, interest earned, and runway for achieving each goal.
Extensible Architecture	Plug‑in‑style front‑end sections, clean service‑layer separation on the back‑end, and a lightweight SQLite database that can be swapped for PostgreSQL/MySQL with minimal changes.
Why This Project Matters
Holistic View – Clients no longer need multiple spreadsheets or disparate apps; everything from daily cash‑flow to long‑term tax planning lives under one roof.
Educational – The UI explains financial concepts (e.g., “What does 80C mean?”) in plain language, making finance accessible to non‑experts.
Scalable Design – Although the current prototype runs on a single Spring Boot process, the codebase is structured for micro‑service extraction (e.g., separate investment or AI services).
Open‑Source Learning – Demonstrates best practices for role‑based React components, JPA entity modelling, DTO handling, and CI/CD pipelines.
Technical Stack
Layer	Technology	Reason
Back‑end	Spring Boot 3.x, Spring Data JPA, SQLite (via JDBC)	Rapid development, strong typing, easy DB swap.
Front‑end	React 18, Vite, Tailwind CSS, lucide‑react icons, Chart.js	Modern, fast hot‑reload, small bundle size, responsive charts.
API	RESTful JSON endpoints under /api/*	Simple consumption from any client (mobile, web).
Authentication	JWT stored in localStorage, validated via AuthFilter.	
Testing	JUnit 5 (back‑end), React Testing Library (front‑end).	
Build / Deploy	Maven Wrapper (./mvnw), npm scripts (npm run dev, npm run build).	
Deployment	Dockerfile provided; can be deployed on any container platform (GitHub Actions, Render, Fly.io, Azure App Service, etc.).	
Repository Layout


/backend
 ├─ src/main/java/com/uip/backend/
 │    ├─ model/            ← JPA entities (User, Portfolio, InsurancePolicy, …)
 │    ├─ repository/       ← Spring Data repositories
 │    ├─ controller/       ← REST controllers (Auth, Investment, Insurance, …)
 │    └─ service/          ← Business logic (InvestmentService, AISimulationService)
 └─ pom.xml                ← Maven build, UTF‑8 encoding, dependencies
/frontend
 ├─ src/
 │    ├─ components/       ← Re‑usable UI pieces (Sidebar, GlassCard, ChartWrapper)
 │    ├─ pages/            ← Main route components (Dashboard, InvestmentPage, …)
 │    ├─ services/api.js   ← Wrapper around fetch calls for each backend module
 │    └─ App.jsx            ← Root component controlling navigation
 ├─ vite.config.ts
 └─ package.json          ← npm scripts, React, Vite, Tailwind, lucide‑react
/.github
 └─ workflows/             ← GitHub Actions CI (build + test)
Dockerfile                 ← Multi‑stage build (frontend → static files, backend Java)
README.md                  ← (this file)
.gitignore
How to Get Started (Developer Walk‑through)
Clone the repo

bash


git clone https://github.com/<your‑org>/unified-investment-platform.git
cd unified-investment-platform
Run the back‑end

bash


./mvnw spring-boot:run   # runs on http://localhost:8085
Run the front‑end

bash


cd frontend
npm install
npm run dev               # runs on http://localhost:5173
Explore the UI – Log in with the demo user (email: demo@example.com, password password) and try out each module (Dashboard → Investment → Execute Order, Insurance → Add Policy, etc.).

Run Tests

Back‑end: ./mvnw test
Front‑end: npm run test
Docker Deploy (one‑liner)

bash


docker build -t uip .
docker run -p 8085:8085 uip
Future Roadmap (Ideas for Contributors)
Real‑world brokerage integration (e.g., Zerodha, Alpaca) with OAuth and order routing.
Payment gateway (Stripe / Razorpay) to actually debit/credit user balances.
AI Advisor v2 – fine‑tuned LLM that can generate personalised investment suggestions.
Multi‑currency support & FX rate feed.
Role‑based permissions refinement (e.g., financial advisor role).
Unit / integration test coverage > 80 % for both back‑end and front‑end.
CI/CD pipelines that automatically push to a staging environment on every PR merge.
License
MIT – Free to use, modify, and redistribute. See LICENSE for details.
