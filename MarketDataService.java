package com.uip.backend.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketDataService {

    // Preset list of Indian & Global market tickers with realistic simulated data (Groww & Angel One inspired)
    public List<Map<String, Object>> getLiveMarketQuotes() {
        List<Map<String, Object>> quotes = new ArrayList<>();

        quotes.add(createQuote("NIFTY 50", "INDEX", 24852.50, 142.30, 0.58, "NSE Benchmark Index"));
        quotes.add(createQuote("SENSEX", "INDEX", 81465.20, 480.15, 0.59, "BSE Benchmark Index"));
        quotes.add(createQuote("GOLD 24K (10g)", "COMMODITY", 72450.00, 280.00, 0.39, "Domestic Spot Gold 999 Purity"));
        quotes.add(createQuote("SILVER (1kg)", "COMMODITY", 84300.00, -320.00, -0.38, "Domestic Spot Silver"));

        // Indian Equities (NSE/BSE)
        quotes.add(createQuote("RELIANCE", "STOCK", 2985.40, 32.10, 1.09, "Reliance Industries Ltd"));
        quotes.add(createQuote("TCS", "STOCK", 4210.80, -18.50, -0.44, "Tata Consultancy Services Ltd"));
        quotes.add(createQuote("HDFCBANK", "STOCK", 1645.25, 14.80, 0.91, "HDFC Bank Ltd"));
        quotes.add(createQuote("INFY", "STOCK", 1825.60, 22.40, 1.24, "Infosys Ltd"));
        quotes.add(createQuote("TATAMOTORS", "STOCK", 1045.75, 28.60, 2.81, "Tata Motors Ltd"));
        quotes.add(createQuote("ICICIBANK", "STOCK", 1215.30, 8.90, 0.74, "ICICI Bank Ltd"));
        quotes.add(createQuote("ITC", "STOCK", 512.40, 3.20, 0.63, "ITC Limited"));
        quotes.add(createQuote("BHARTIARTL", "STOCK", 1540.90, 19.30, 1.27, "Bharti Airtel Ltd"));

        // Mutual Funds & ETFs
        quotes.add(createQuote("Parag Parikh Flexi Cap Fund", "MUTUAL_FUND", 84.62, 0.45, 0.53, "Equity - Flexi Cap Direct Growth"));
        quotes.add(createQuote("Mirae Asset Large Cap Fund", "MUTUAL_FUND", 112.35, 0.82, 0.73, "Equity - Large Cap Direct Growth"));
        quotes.add(createQuote("SBI Small Cap Fund", "MUTUAL_FUND", 168.90, 1.85, 1.11, "Equity - Small Cap Direct Growth"));
        quotes.add(createQuote("NIFTY BEES ETF", "ETF", 274.20, 1.65, 0.61, "Nippon India ETF Nifty 50 BeES"));
        quotes.add(createQuote("GOLD BEES ETF", "GOLD", 64.80, 0.35, 0.54, "Nippon India ETF Gold BeES"));
        quotes.add(createQuote("SGB (Sovereign Gold Bond)", "BOND", 7180.00, 25.00, 0.35, "RBI Govt Sovereign Gold Bond 2.5% p.a."));

        return quotes;
    }

    public Map<String, Object> getQuote(String symbol) {
        String query = symbol.toUpperCase().trim();
        for (Map<String, Object> quote : getLiveMarketQuotes()) {
            if (quote.get("symbol").toString().equalsIgnoreCase(query) || 
                quote.get("name").toString().toUpperCase().contains(query)) {
                return quote;
            }
        }
        // Fallback simulated price
        return createQuote(symbol, "STOCK", 1000.00, 12.50, 1.25, symbol + " Asset");
    }

    private Map<String, Object> createQuote(String symbol, String type, double price, double change, double changePct, String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", symbol);
        map.put("name", name);
        map.put("assetType", type);
        map.put("currentPrice", price);
        map.put("dayChange", change);
        map.put("dayChangePct", changePct);
        map.put("high52W", Math.round(price * 1.22 * 100.0) / 100.0);
        map.put("low52W", Math.round(price * 0.78 * 100.0) / 100.0);
        map.put("currency", "INR");
        return map;
    }
}
