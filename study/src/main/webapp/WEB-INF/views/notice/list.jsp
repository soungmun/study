<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>공지사항</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; color: #333; }
        .container { max-width: 900px; margin: 40px auto; padding: 0 16px; }
        h1 { font-size: 1.8rem; margin-bottom: 24px; color: #1a1a2e; }

        /* 검색 바 */
        .search-form { display: flex; gap: 8px; margin-bottom: 20px; }
        .search-form select,
        .search-form input[type=text] {
            padding: 8px 12px; border: 1px solid #ccc; border-radius: 6px; font-size: 0.95rem;
        }
        .search-form input[type=text] { flex: 1; }
        .search-form button {
            padding: 8px 20px; background: #4f46e5; color: #fff;
            border: none; border-radius: 6px; cursor: pointer; font-size: 0.95rem;
        }
        .search-form button:hover { background: #4338ca; }

        /* 테이블 */
        .notice-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
        .notice-table th { background: #4f46e5; color: #fff; padding: 12px 16px; text-align: left; font-size: 0.9rem; }
        .notice-table td { padding: 12px 16px; border-bottom: 1px solid #eee; font-size: 0.9rem; }
        .notice-table tr:last-child td { border-bottom: none; }
        .notice-table tr:hover td { background: #f0f0ff; }
        .notice-table td.title a { color: #1a1a2e; text-decoration: none; font-weight: 500; }
        .notice-table td.title a:hover { color: #4f46e5; text-decoration: underline; }
        .notice-table td.center { text-align: center; color: #666; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 0.78rem; }
        .badge-view { background: #e0e7ff; color: #4f46e5; }
        .badge-comment { background: #dcfce7; color: #16a34a; }
        .badge-like { background: #fce7f3; color: #db2777; }

        /* 페이지네이션 */
        .pagination { display: flex; justify-content: center; align-items: center; gap: 6px; margin-top: 24px; }
        .pagination a, .pagination span {
            display: inline-block; padding: 6px 12px; border-radius: 6px; font-size: 0.9rem;
            text-decoration: none; border: 1px solid #ddd; color: #555; background: #fff;
        }
        .pagination a:hover { background: #e0e7ff; border-color: #4f46e5; color: #4f46e5; }
        .pagination .active { background: #4f46e5; color: #fff; border-color: #4f46e5; }
        .pagination .disabled { color: #bbb; cursor: default; }

        /* 버튼 */
        .btn-write { display: inline-block; margin-bottom: 16px; padding: 9px 20px;
            background: #4f46e5; color: #fff; border-radius: 6px; text-decoration: none; font-size: 0.9rem; }
        .btn-write:hover { background: #4338ca; }
        .empty { text-align: center; padding: 40px; color: #999; }
    </style>
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
