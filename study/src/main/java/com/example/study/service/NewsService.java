package com.example.study.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 주식 관련 뉴스 검색 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 네이버 검색 API 등을 이용하여 특정 종목 관련 뉴스를 가져옵니다.
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    // 한국어 주식·증시 관련 헤드라인 (Google News RSS, 인증 불필요)
    private static final String FEED_URL =
            "https://news.google.com/rss/search"
                    + "?q=%EC%A3%BC%EC%8B%9D+OR+%EC%A6%9D%EC%8B%9C+OR+%EC%BD%94%EC%8A%A4%ED%94%BC"
                    + "&hl=ko&gl=KR&ceid=KR:ko";

    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (study-notice/1.0)")
            .build();

    public record Headline(String title, String link, String source) {}

    /** 종목명으로 Google News RSS 검색 */
    public List<Headline> fetchByStock(String stockName, int max) {
        try {
            String encoded = java.net.URLEncoder.encode(stockName + " 주식", StandardCharsets.UTF_8);
            String url = "https://news.google.com/rss/search?q=" + encoded + "&hl=ko&gl=KR&ceid=KR:ko";
            String xml = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            return parseHeadlines(xml, max);
        } catch (Exception e) {
            log.warn("[News] 종목 뉴스 가져오기 실패 ({}): {}", stockName, e.getMessage());
            return List.of();
        }
    }

    public List<Headline> fetchTop(int max) {
        try {
            String xml = restClient.get()
                    .uri(FEED_URL)
                    .retrieve()
                    .body(String.class);
            return parseHeadlines(xml, max);
        } catch (Exception e) {
            log.warn("[News] 뉴스 RSS 가져오기 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Headline> parseHeadlines(String xml, int max) {
        if (xml == null || xml.isBlank()) return List.of();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            int n = Math.min(max, items.getLength());
            List<Headline> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                Element item = (Element) items.item(i);
                String rawTitle = textOf(item, "title");
                String link = textOf(item, "link");
                String source = textOf(item, "source");
                // Google News title 끝에 붙는 " - 소스명" 제거
                String title = rawTitle;
                if (!source.isBlank() && rawTitle.endsWith(" - " + source)) {
                    title = rawTitle.substring(0, rawTitle.length() - 3 - source.length()).trim();
                }
                if (!title.isBlank() && !link.isBlank()) {
                    out.add(new Headline(title, link, source));
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("[News] XML 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private static String textOf(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) return "";
        String t = list.item(0).getTextContent();
        return t == null ? "" : t.trim();
    }
}