<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${notice.title}</title>
    <style>
        body { font-family: sans-serif; max-width: 800px; margin: 30px auto; padding: 0 16px; }
        h1 { margin-bottom: 8px; }
        .meta { color: #888; font-size: 14px; margin-bottom: 20px; }
        .content { white-space: pre-wrap; line-height: 1.6; padding: 20px 0; border-top: 1px solid #ddd; border-bottom: 1px solid #ddd; }
        .actions { margin-top: 20px; display: flex; gap: 8px; }
        .btn { padding: 8px 14px; border-radius: 4px; text-decoration: none; border: none; cursor: pointer; font-size: 14px; }
        .btn-primary { background: #2c7be5; color: #fff; }
        .btn-default { background: #eee; color: #333; }
        .btn-danger { background: #dc3545; color: #fff; }
        form { display: inline; }
    </style>
</head>
<body>
<h1><c:out value="${notice.title}"/></h1>

<div class="meta">
    작성자: <c:out value="${notice.author}"/> ·
    <fmt:parseDate value="${notice.createdAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="parsed" type="both"/>
    <fmt:formatDate value="${parsed}" pattern="yyyy-MM-dd HH:mm"/> ·
    조회수
    <c:choose>
        <c:when test="${notice.viewCount >= 1000}">
            <fmt:formatNumber value="${notice.viewCount / 1000}" maxFractionDigits="1"/>k
        </c:when>
        <c:otherwise>${notice.viewCount}</c:otherwise>
    </c:choose>
</div>

<div class="content"><c:out value="${notice.content}"/></div>

<div class="actions">
    <a class="btn btn-default" href="${pageContext.request.contextPath}/notices">목록</a>
    <a class="btn btn-primary" href="${pageContext.request.contextPath}/notices/${notice.id}/edit">수정</a>
    <form method="post" action="${pageContext.request.contextPath}/notices/${notice.id}/delete"
          onsubmit="return confirm('정말 삭제하시겠습니까?');">
        <button type="submit" class="btn btn-danger">삭제</button>
    </form>
</div>
</body>
</html>