package com.example.study.controller;

import com.example.study.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockApiController {

    private final StockService stockService;

    public StockApiController(StockService stockService) {
        this.stockService = stockService;
    }

    /** 코스피·코스닥 큐레이션 종목 중 당일 상승률 TOP N */
    @GetMapping("/top-gainers")
    public List<StockService.Quote> topGainers(
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        return stockService.fetchTopGainers(limit);
    }

    /** 코스피·코스닥 큐레이션 종목 중 당일 하락률 TOP N */
    @GetMapping("/top-losers")
    public List<StockService.Quote> topLosers(
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        return stockService.fetchTopLosers(limit);
    }
}
