<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><c:choose><c:when test="${empty notice.id}">공지 등록</c:when><c:otherwise>공지 수정</c:otherwise></c:choose></title>
    <style>
        body { font-family: sans-serif; max-width: 720px; margin: 30px auto; padding: 0 16px; }
        h1 { margin-bottom: 20px; }
        .field { margin-bottom: 16px; }
        label { display: block; font-weight: bold; margin-bottom: 6px; }
        input[type=text], textarea { width: 100%; padding: 8px; box-sizing: border-box; font-size: 14px; }
        textarea { min-height: 240px; resize: vertical; }
        .error { color: #c00; font-size: 13px; margin-top: 4px; }
        .actions { display: flex; gap: 8px; }
        .btn { padding: 8px 16px; border-radius: 4px; text-decoration: none; border: none; cursor: pointer; font-size: 14px; }
        .btn-primary { background: #2c7be5; color: #fff; }
        .btn-default { background: #eee; color: #333; }
    </style>
</head>
<body>
<h1>
    <c:choose>
        <c:when test="${empty notice.id}">공지 등록</c:when>
        <c:otherwise>공지 수정</c:otherwise>
    </c:choose>
</h1>

<c:set var="action" value="${pageContext.request.contextPath}/notices"/>
<c:if test="${not empty notice.id}">
    <c:set var="action" value="${pageContext.request.contextPath}/notices/${notice.id}/edit"/>
</c:if>

<form:form modelAttribute="notice" action="${action}" method="post">
    <div class="field">
        <label for="author">작성자</label>
        <form:input path="author" id="author" maxlength="100"/>
        <form:errors path="author" cssClass="error" element="div"/>
    </div>

    <div class="field">
        <label for="title">제목</label>
        <form:input path="title" id="title" maxlength="200"/>
        <form:errors path="title" cssClass="error" element="div"/>
    </div>

    <div class="field">
        <label for="content">내용</label>
        <form:textarea path="content" id="content"/>
        <form:errors path="content" cssClass="error" element="div"/>
    </div>

    <div class="actions">
        <button type="submit" class="btn btn-primary">저장</button>
        <a class="btn btn-default" href="${pageContext.request.contextPath}/notices">취소</a>
    </div>
</form:form>
</body>
</html>