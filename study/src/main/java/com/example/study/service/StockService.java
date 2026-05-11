package com.example.study.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
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

    // 한국 시장 상승률 랭킹용 — KOSPI/KOSDAQ 시총 상위 + 거래 활발한 종목 큐레이션
    private static final List<Symbol> KR_MARKET = List.of(
            // KOSPI 대형주
            new Symbol("005930.KS", "삼성전자"),
            new Symbol("000660.KS", "SK하이닉스"),
            new Symbol("373220.KS", "LG에너지솔루션"),
            new Symbol("207940.KS", "삼성바이오로직스"),
            new Symbol("005380.KS", "현대차"),
            new Symbol("000270.KS", "기아"),
            new Symbol("105560.KS", "KB금융"),
            new Symbol("035420.KS", "NAVER"),
            new Symbol("055550.KS", "신한지주"),
            new Symbol("035720.KS", "카카오"),
            new Symbol("005490.KS", "POSCO홀딩스"),
            new Symbol("006400.KS", "삼성SDI"),
            new Symbol("028260.KS", "삼성물산"),
            new Symbol("068270.KS", "셀트리온"),
            new Symbol("012330.KS", "현대모비스"),
            new Symbol("003550.KS", "LG"),
            new Symbol("066570.KS", "LG전자"),
            new Symbol("015760.KS", "한국전력"),
            new Symbol("017670.KS", "SK텔레콤"),
            new Symbol("032830.KS", "삼성생명"),
            new Symbol("086790.KS", "하나금융지주"),
            new Symbol("009150.KS", "삼성전기"),
            new Symbol("018260.KS", "삼성에스디에스"),
            new Symbol("259960.KS", "크래프톤"),
            new Symbol("033780.KS", "KT&G"),
            new Symbol("030200.KS", "KT"),
            new Symbol("010130.KS", "고려아연"),
            new Symbol("010950.KS", "S-Oil"),
            new Symbol("051910.KS", "LG화학"),
            new Symbol("096770.KS", "SK이노베이션"),
            new Symbol("011200.KS", "HMM"),
            new Symbol("009830.KS", "한화솔루션"),
            new Symbol("078930.KS", "GS"),
            new Symbol("004020.KS", "현대제철"),
            // KOSDAQ
            new Symbol("247540.KQ", "에코프로비엠"),
            new Symbol("086520.KQ", "에코프로"),
            new Symbol("196170.KQ", "알테오젠"),
            new Symbol("277810.KQ", "레인보우로보틱스"),
            new Symbol("443250.KQ", "리노공업"),
            new Symbol("145020.KQ", "휴젤")
    );

    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (study-notice/1.0)")
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record Quote(String name, Double price, Double change, Double changePercent) {}

    private record Symbol(String code, String name) {}

    public List<Quote> fetchWatchlist() {
        return WATCHLIST.stream().map(this::fetchOne).toList();
    }

    /** 한국 시장 큐레이션 종목 중 당일 상승률 상위 N개 (지수 제외) */
    public List<Quote> fetchTopGainers(int limit) {
        int n = Math.max(1, Math.min(50, limit));
        return KR_MARKET.parallelStream()
                .map(this::fetchOne)
                .filter(q -> q.changePercent() != null)
                .sorted(Comparator.comparingDouble(Quote::changePercent).reversed())
                .limit(n)
                .toList();
    }

    /** 한국 시장 큐레이션 종목 중 당일 하락률 상위 N개 */
    public List<Quote> fetchTopLosers(int limit) {
        int n = Math.max(1, Math.min(50, limit));
        return KR_MARKET.parallelStream()
                .map(this::fetchOne)
                .filter(q -> q.changePercent() != null)
                .sorted(Comparator.comparingDouble(Quote::changePercent))
                .limit(n)
                .toList();
    }

    private Quote fetchOne(Symbol s) {
        try {
            String raw = restClient.get()
                    .uri(CHART_URL + s.code())
                    .retrieve()
                    .body(String.class);
            JsonNode meta = objectMapper.readTree(raw)
                    .path("chart").path("result").path(0).path("meta");
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
