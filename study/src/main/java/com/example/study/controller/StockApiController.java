package com.example.study.controller;

import com.example.study.service.NewsService;
import com.example.study.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주식 정보 조회 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 주식 시세 데이터, 차트 정보, 관련 뉴스 등을 제공하는 API를 포함합니다.
 */
@RestController
@RequestMapping("/api/stocks")
public class StockApiController {

    private final StockService stockService;
    private final NewsService newsService;

    public StockApiController(StockService stockService, NewsService newsService) {
        this.stockService = stockService;
        this.newsService = newsService;
    }

    /** 코스피·코스닥 큐레이션 종목 중 당일 상승률 TOP N */
    @GetMapping("/top-gainers")
    public List<StockService.Quote> topGainers(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit
    ) {
        return stockService.fetchTopGainers(limit);
    }

    /** 코스피·코스닥 큐레이션 종목 중 당일 하락률 TOP N */
    @GetMapping("/top-losers")
    public List<StockService.Quote> topLosers(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit
    ) {
        return stockService.fetchTopLosers(limit);
    }

    /** 종목명으로 관련 뉴스 검색 */
    @GetMapping("/news")
    public List<NewsService.Headline> stockNews(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit
    ) {
        return newsService.fetchByStock(name, limit);
    }
}
