package com.uip.backend.controller;

import com.uip.backend.model.MarketNews;
import com.uip.backend.repository.MarketNewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private MarketNewsRepository marketNewsRepository;

    @GetMapping
    public ResponseEntity<List<MarketNews>> getNews() {
        List<MarketNews> news = marketNewsRepository.findAllByOrderByPublishedDateDesc();
        if (news.isEmpty()) {
            // Seed News Data
            seedNewsData();
            news = marketNewsRepository.findAllByOrderByPublishedDateDesc();
        }
        return ResponseEntity.ok(news);
    }

    @PostMapping("/summarize/{newsId}")
    public ResponseEntity<?> getNewsSummary(@PathVariable Long newsId) {
        Optional<MarketNews> newsOpt = marketNewsRepository.findById(newsId);
        if (newsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MarketNews news = newsOpt.get();
        // Generate a detailed simulated AI summary based on the title & category
        String summary = news.getSummary();
        String aiSummary = "### AI Market Digest: " + news.getTitle() + "\n\n" +
                "**Key Highlights:**\n" +
                "- This article highlights a major shift in the " + news.getCategory() + " sector.\n" +
                "- The underlying driver is shifting interest rates and corporate earnings metrics.\n" +
                "- Market analysts expect short-term volatility but a positive medium-term outlook.\n\n" +
                "**Investment Takeaway:**\n" +
                "If you are exposed to this sector, consider maintaining your dollar-cost-averaging (DCA) strategy and avoiding emotional panic selling. Conservative profiles should hold steady, while aggressive profiles can use the dip as a buying opportunity.\n\n" +
                "**Sentiment Indicator:** 🟢 Bullish (Short-Term accumulation)";

        return ResponseEntity.ok(Map.of(
            "title", news.getTitle(),
            "originalSummary", summary,
            "aiSummary", aiSummary,
            "category", news.getCategory(),
            "publishedDate", news.getPublishedDate()
        ));
    }

    private void seedNewsData() {
        List<MarketNews> seeds = new ArrayList<>();
        seeds.add(MarketNews.builder()
                .title("US Federal Reserve Hints at Rate Cuts Amid Easing Inflation")
                .category("MACRO")
                .summary("The Federal Reserve Chairman signaled that interest rate cuts could begin as early as next quarter, as inflation measures move closer to the central bank's 2% target. Economic growth remains moderate while the labor market shows signs of cooling down.")
                .publishedDate(LocalDateTime.now().minusHours(2))
                .build());

        seeds.add(MarketNews.builder()
                .title("Tech Stocks Surge led by NVIDIA and Microsoft Earnings Blowout")
                .category("STOCKS")
                .summary("Major technology shares index rose by 2.4% today following quarterly earnings updates from NVIDIA and Microsoft, both beating analyst projections. Demand for Enterprise AI chips and cloud infrastructure services continues to outpace expectations globally.")
                .publishedDate(LocalDateTime.now().minusHours(5))
                .build());

        seeds.add(MarketNews.builder()
                .title("Treasury Yields Stabilize as Investors Seek Bond Safe Havens")
                .category("BONDS")
                .summary("Ten-year US treasury yields fell to 3.8% as institutional capital shifted into fixed income assets. Economists suggest the bond market is pricing in rate cuts, making lock-in yields highly attractive for conservative portfolios.")
                .publishedDate(LocalDateTime.now().minusDays(1))
                .build());

        seeds.add(MarketNews.builder()
                .title("Bitcoin Crosses $68k Threshold Ahead of Halving Event")
                .category("CRYPTO")
                .summary("The world's largest cryptocurrency, Bitcoin, broke through the $68,000 mark today. Retail and institutional interest has surged following approvals of Bitcoin spot ETFs, drawing capital inflows and increasing overall market liquidity.")
                .publishedDate(LocalDateTime.now().minusDays(2))
                .build());

        seeds.add(MarketNews.builder()
                .title("Global Supply Chains Recover But Oil Prices Edge Upward")
                .category("MACRO")
                .summary("Shipping lane congestions have largely cleared, returning shipping rates to historical averages. However, Crude oil prices rose 1.8% to $82 per barrel due to geopolitical tensions in Eastern Europe and OPEC production quotas.")
                .publishedDate(LocalDateTime.now().minusDays(3))
                .build());

        marketNewsRepository.saveAll(seeds);
    }
}
