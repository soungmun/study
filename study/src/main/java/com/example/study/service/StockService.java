package com.example.study.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private static final String CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";

    private static final List<Symbol> WATCHLIST = List.of(
            new Symbol("^KS11", "코스피"),
            new Symbol("^KQ11", "코스닥"),
            new Symbol("005930.KS", "삼성전자"),
            new Symbol("000660.KS", "SK하이닉스"),
            new Symbol("035420.KS", "NAVER"),
            new Symbol("035720.KS", "카카오")
    );

    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (study-notice/1.0)")
            .build();

    public record Quote(String name, Double price, Double change, Double changePercent) {}

    private record Symbol(String code, String name) {}

    public List<Quote> fetchWatchlist() {
        return WATCHLIST.stream().map(this::fetchOne).toList();
    }

    private Quote fetchOne(Symbol s) {
        try {
            JsonNode body = restClient.get()
                    .uri(CHART_URL + s.code())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode meta = body.path("chart").path("result").path(0).path("meta");
            if (meta.isMissingNode() || meta.path("regularMarketPrice").isMissingNode()) {
                log.warn("[Stock] {} meta 비어있음", s.code());
                return new Quote(s.name(), null, null, null);
            }
            double price = meta.path("regularMarketPrice").asDouble();
            double prev = meta.path("chartPreviousClose").asDouble(price);
            double change = price - prev;
            double pct = prev == 0 ? 0 : (change / prev) * 100;
            return new Quote(s.name(), price, change, pct);
        } catch (Exception e) {
            log.warn("[Stock] {} 조회 실패: {}", s.code(), e.getMessage());
            return new Quote(s.name(), null, null, null);
        }
    }
}