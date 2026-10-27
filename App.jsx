import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import CashFlowPage from './pages/CashFlowPage';
import PortfolioPage from './pages/PortfolioPage';
import SavingsCalculatorsPage from './pages/SavingsCalculatorsPage';
import GoalsPage from './pages/GoalsPage';
import LoansPage from './pages/LoansPage';
import ReportsPage from './pages/ReportsPage';
import RiskAnalysisPage from './pages/RiskAnalysisPage';
import MarketNewsPage from './pages/MarketNewsPage';
import Chatbot from './components/Chatbot';
import AdminPortalPage from './pages/AdminPortalPage';
import InsurancePage from './pages/InsurancePage';
import TaxPlannerPage from './pages/TaxPlannerPage';

const MainApp = () => {
  const { user, loading, isAdmin } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: 'var(--bg-primary)', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ width: '48px', height: '48px', border: '3px solid rgba(99, 102, 241, 0.2)', borderTop: '3px solid var(--color-primary)', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
        <h2>Unified Wealth Management Loading...</h2>
      </div>
    );
  }

  if (!user) {
    return <Login />;
  }

  const renderActivePage = () => {
    switch (activeTab) {
      case 'dashboard':
        return <Dashboard setActiveTab={setActiveTab} />;
      case 'cashflow':
        return <CashFlowPage />;
      case 'portfolio':
        return <PortfolioPage />;
      case 'savings':
        return <SavingsCalculatorsPage />;
      case 'goals':
        return <GoalsPage />;
      case 'loans':
        return <LoansPage />;
      case 'reports':
        return <ReportsPage />;
      case 'insurance':
        return <InsurancePage />;
      case 'tax':
        return <TaxPlannerPage />;
      case 'risk':
        return <RiskAnalysisPage />;
      case 'news':
        return <MarketNewsPage />;
      case 'ai-chat':
        return <Chatbot />;
      case 'admin':
        return isAdmin ? <AdminPortalPage /> : <Dashboard setActiveTab={setActiveTab} />;
      default:
        return <Dashboard setActiveTab={setActiveTab} />;
    }
  };

  return (
    <div className="app-container">
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />
      <main className="main-content">
        {renderActivePage()}
      </main>
    </div>
  );
};

const App = () => {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
};

export default App;
