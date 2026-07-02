package com.example.study.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

/**
 * 메일 발송 전, 수신 메일함이 실제로 존재하는지 확인한다.
 * DNS MX 조회 → 가장 우선순위 높은 메일 서버에 SMTP RCPT TO 로 질의.
 *
 * 결과:
 *  - UNDELIVERABLE: 도메인에 메일서버 없음 / 서버가 "없는 사용자(550)"로 거절 → 발송 차단
 *  - DELIVERABLE  : 서버가 수신 가능(250)으로 응답
 *  - UNKNOWN      : 포트25 차단·타임아웃·그레이리스팅 등 판단 불가 → 그냥 발송 진행(fail-open)
 *
 * 주의: 발신 서버에서 아웃바운드 25번 포트가 막혀 있으면(국내 ISP/클라우드 다수)
 *      항상 UNKNOWN 이 되어 검증이 동작하지 않는다. 이 경우 기존처럼 발송만 시도한다.
 */
/**
 * 이메일 수신 가능 여부를 검증하는 서비스 클래스입니다.
 * SMTP 프로토콜을 이용해 실제 메일 서버의 수신함 존재 여부를 확인합니다.
 */
@Service
public class MailboxVerifier {

    private static final Logger log = LoggerFactory.getLogger(MailboxVerifier.class);

    public enum Result { DELIVERABLE, UNDELIVERABLE, UNKNOWN }

    private static final int TIMEOUT_MS = 4000;

    private final String probeFrom;   // SMTP MAIL FROM 으로 쓸 주소 (우리 발신 주소)
    private final String heloDomain;  // HELO 도메인

    public MailboxVerifier(@Value("${app.mail.from}") String probeFrom) {
        this.probeFrom = probeFrom;
        int at = probeFrom.indexOf('@');
        this.heloDomain = at > 0 ? probeFrom.substring(at + 1) : "localhost";
    }

    public Result verify(String email) {
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) return Result.UNDELIVERABLE;
        String domain = email.substring(at + 1);

        List<String> mxHosts = lookupMx(domain);
        if (mxHosts.isEmpty()) {
            log.info("[MailboxVerifier] MX 없음 → 없는 도메인 판정: {}", domain);
            return Result.UNDELIVERABLE;
        }
        // 우선순위 1순위 메일서버에만 질의(지연 최소화). 막혀 있으면 UNKNOWN.
        return probe(mxHosts.get(0), email);
    }

    /** MX 레코드를 우선순위(낮은 값이 우선) 순으로 정렬해 호스트명만 반환. */
    private List<String> lookupMx(String domain) {
        List<String> hosts = new ArrayList<>();
        InitialDirContext ctx = null;
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            Attribute mx = attrs.get("MX");
            if (mx != null) {
                List<String[]> entries = new ArrayList<>();
                for (int i = 0; i < mx.size(); i++) {
                    String[] parts = ((String) mx.get(i)).trim().split("\\s+");  // "10 mail.example.com."
                    if (parts.length == 2) entries.add(parts);
                }
                entries.sort(Comparator.comparingInt(p -> Integer.parseInt(p[0])));
                for (String[] e : entries) {
                    String host = e[1];
                    if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
                    hosts.add(host);
                }
            }
        } catch (NamingException | NumberFormatException e) {
            log.debug("[MailboxVerifier] MX 조회 실패 domain={}: {}", domain, e.getMessage());
        } finally {
            if (ctx != null) try { ctx.close(); } catch (NamingException ignored) {}
        }
        return hosts;
    }

    private Result probe(String host, String email) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, 25), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            if (!readReply(in).startsWith("220")) return Result.UNKNOWN;
            sendCmd(out, "HELO " + heloDomain);
            if (!readReply(in).startsWith("250")) return Result.UNKNOWN;
            sendCmd(out, "MAIL FROM:<" + probeFrom + ">");
            if (!readReply(in).startsWith("250")) return Result.UNKNOWN;
            sendCmd(out, "RCPT TO:<" + email + ">");
            String rcpt = readReply(in);
            sendCmd(out, "QUIT");

            if (rcpt.startsWith("250") || rcpt.startsWith("251")) return Result.DELIVERABLE;
            if (rcpt.startsWith("550") || rcpt.startsWith("553")) {
                log.info("[MailboxVerifier] 없는 메일함 판정 {}: {}", email, rcpt);
                return Result.UNDELIVERABLE;
            }
            return Result.UNKNOWN;  // 4xx 그레이리스팅 등
        } catch (IOException e) {
            log.debug("[MailboxVerifier] probe 실패(차단/타임아웃) host={}: {}", host, e.getMessage());
            return Result.UNKNOWN;
        }
    }

    /** SMTP 멀티라인 응답에서 마지막(최종 코드) 라인을 반환. */
    private String readReply(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) return "";
        while (line.length() >= 4 && line.charAt(3) == '-') {
            String next = in.readLine();
            if (next == null) break;
            line = next;
        }
        return line;
    }

    private void sendCmd(BufferedWriter out, String cmd) throws IOException {
        out.write(cmd + "\r\n");
        out.flush();
    }
}