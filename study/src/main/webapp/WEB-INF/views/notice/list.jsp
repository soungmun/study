<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>공지사항</title>
    <link rel="stylesheet" href="<c:url value="/resources/css/list.css"/>">
</head>
<body>
<div class="container">
    <h1>📋 공지사항</h1>

    <%-- 검색 폼 --%>
    <form class="search-form" method="get" action="/notices">
        <select name="type">
            <option value="title"  <c:if test="${type == 'title'}">selected</c:if>>제목</option>
            <option value="content" <c:if test="${type == 'content'}">selected</c:if>>내용</option>
        </select>
        <input type="text" name="keyword" value="${keyword}" placeholder="검색어를 입력하세요">
        <button type="submit">검색</button>
    </form>

    <%-- 작성 버튼 (로그인 상태는 JS로 확인 — 서버 세션 기반이므로 노출 후 서버에서 권한 체크) --%>
    <a href="/notices/new" class="btn-write">+ 글쓰기</a>

    <%-- 공지사항 테이블 --%>
    <table class="notice-table">
        <thead>
        <tr>
            <th style="width:60px">번호</th>
            <th>제목</th>
            <th style="width:110px">작성자</th>
            <th style="width:140px">작성일</th>
            <th style="width:80px">조회</th>
            <th style="width:60px">댓글</th>
            <th style="width:60px">좋아요</th>
        </tr>
        </thead>
        <tbody>
        <c:choose>
            <c:when test="${noticePage.totalElements == 0}">
                <tr><td colspan="7" class="empty">등록된 공지사항이 없습니다.</td></tr>
            </c:when>
            <c:otherwise>
                <c:forEach var="n" items="${noticePage.content}">
                    <tr>
                        <td class="center">${n.id}</td>
                        <td class="title"><a href="/notices/${n.id}">${n.title}</a></td>
                        <td class="center">${n.author}</td>
                        <td class="center">
                            <c:if test="${n.createdAt != null}">${n.createdAt.toString().substring(0,10)}</c:if>
                        </td>
                        <td class="center"><span class="badge badge-view">${n.viewCountText}</span></td>
                        <td class="center"><span class="badge badge-comment">${n.commentCount}</span></td>
                        <td class="center"><span class="badge badge-like">${n.likeCount}</span></td>
                    </tr>
                </c:forEach>
            </c:otherwise>
        </c:choose>
        </tbody>
    </table>

    <%-- 페이지네이션 --%>
    <c:if test="${noticePage.totalPages > 1}">
        <div class="pagination">
            <%-- 이전 --%>
            <c:choose>
                <c:when test="${currentPage == 0}">
                    <span class="disabled">&laquo;</span>
                </c:when>
                <c:otherwise>
                    <a href="/notices?page=${currentPage - 1}&size=${noticePage.size}&type=${type}&keyword=${keyword}">&laquo;</a>
                </c:otherwise>
            </c:choose>

            <%-- 페이지 번호 (최대 10개) --%>
            <c:set var="startPage" value="${currentPage - 4 < 0 ? 0 : currentPage - 4}" />
            <c:set var="endPage"   value="${startPage + 9 >= noticePage.totalPages ? noticePage.totalPages - 1 : startPage + 9}" />
            <c:forEach var="i" begin="${startPage}" end="${endPage}">
                <c:choose>
                    <c:when test="${i == currentPage}">
                        <span class="active">${i + 1}</span>
                    </c:when>
                    <c:otherwise>
                        <a href="/notices?page=${i}&size=${noticePage.size}&type=${type}&keyword=${keyword}">${i + 1}</a>
                    </c:otherwise>
                </c:choose>
            </c:forEach>

            <%-- 다음 --%>
            <c:choose>
                <c:when test="${currentPage >= noticePage.totalPages - 1}">
                    <span class="disabled">&raquo;</span>
                </c:when>
                <c:otherwise>
                    <a href="/notices?page=${currentPage + 1}&size=${noticePage.size}&type=${type}&keyword=${keyword}">&raquo;</a>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>
</div>
</body>
</html>
