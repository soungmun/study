<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>공지사항 목록</title>
    <style>
        body { font-family: sans-serif; max-width: 960px; margin: 30px auto; padding: 0 16px; }
        h1 { margin-bottom: 20px; }
        .toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border-bottom: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background: #f7f7f7; }
        .pager { margin-top: 16px; text-align: center; }
        .pager a, .pager span { padding: 4px 8px; margin: 0 2px; text-decoration: none; }
        .pager .current { font-weight: bold; color: #c00; }
        .btn { display: inline-block; padding: 6px 12px; background: #2c7be5; color: #fff; text-decoration: none; border-radius: 4px; }
        form.search { display: flex; gap: 6px; }
        select, input[type=text] { padding: 6px; }
    </style>
</head>
<body>
<h1>공지사항</h1>

<div class="toolbar">
    <form class="search" method="get" action="${pageContext.request.contextPath}/notices">
        <select name="type">
            <option value="title"   <c:if test="${type eq 'title'}">selected</c:if>>제목</option>
            <option value="content" <c:if test="${type eq 'content'}">selected</c:if>>내용</option>
        </select>
        <input type="text" name="keyword" value="${keyword}" placeholder="검색어">
        <button type="submit">검색</button>
    </form>
    <a class="btn" href="${pageContext.request.contextPath}/notices/new">글쓰기</a>
</div>

<table>
    <thead>
    <tr>
        <th style="width:60px">번호</th>
        <th>제목</th>
        <th style="width:120px">작성자</th>
        <th style="width:160px">작성일</th>
        <th style="width:80px">조회수</th>
    </tr>
    </thead>
    <tbody>
    <c:choose>
        <c:when test="${empty notices.content}">
            <tr><td colspan="5" style="text-align:center;">등록된 공지사항이 없습니다.</td></tr>
        </c:when>
        <c:otherwise>
            <c:forEach var="n" items="${notices.content}">
                <tr>
                    <td>${n.id}</td>
                    <td>
                        <a href="${pageContext.request.contextPath}/notices/${n.id}">
                            <c:out value="${n.title}"/>
                        </a>
                    </td>
                    <td><c:out value="${n.author}"/></td>
                    <td><fmt:parseDate value="${n.createdAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="parsed" type="both"/>
                        <fmt:formatDate value="${parsed}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${n.viewCount >= 1000}">
                                <fmt:formatNumber value="${n.viewCount / 1000}" maxFractionDigits="1"/>k
                            </c:when>
                            <c:otherwise>${n.viewCount}</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
        </c:otherwise>
    </c:choose>
    </tbody>
</table>

<c:if test="${notices.totalPages > 0}">
    <div class="pager">
        <c:if test="${!notices.first}">
            <a href="?type=${type}&keyword=${keyword}&page=${notices.number - 1}&size=${notices.size}">&laquo; 이전</a>
        </c:if>
        <c:forEach var="i" begin="0" end="${notices.totalPages - 1}">
            <c:choose>
                <c:when test="${i == notices.number}">
                    <span class="current">${i + 1}</span>
                </c:when>
                <c:otherwise>
                    <a href="?type=${type}&keyword=${keyword}&page=${i}&size=${notices.size}">${i + 1}</a>
                </c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${!notices.last}">
            <a href="?type=${type}&keyword=${keyword}&page=${notices.number + 1}&size=${notices.size}">다음 &raquo;</a>
        </c:if>
    </div>
</c:if>
</body>
</html>