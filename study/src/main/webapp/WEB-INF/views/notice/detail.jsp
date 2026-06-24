<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${notice.title} - 공지사항</title>
    <link rel="stylesheet" href="<c:url value="/resources/css/detail.css"/>">
</head>
<body>
<div class="container">
    <a href="/notices" class="back-link">← 목록으로</a>

    <div class="card">
        <div class="card-header">
            <h2>${notice.title}</h2>
            <div class="meta">
                <span>✍️ ${notice.author}</span>
                <span>📅 ${notice.createdAt.toString().replace('T', ' ').substring(0, 16)}</span>
                <span>👁 ${notice.viewCountText}</span>
                <span>💬 ${notice.commentCount}</span>
                <span>❤️ <span id="likeCount">${notice.likeCount}</span></span>
            </div>
        </div>

        <div class="card-body">
            <div class="content">${notice.content}</div>

            <%-- 이미지 목록 --%>
            <c:if test="${not empty notice.imageUrls}">
                <div class="images">
                    <c:forEach var="url" items="${notice.imageUrls}">
                        <img src="${url}" alt="공지 이미지" onclick="openModal('${url}')">
                    </c:forEach>
                </div>
            </c:if>
        </div>

        <div class="actions">
            <div class="actions-left">
                <%-- 좋아요 버튼 --%>
                <button class="btn-like ${notice.iLiked ? 'liked' : ''}" id="likeBtn" onclick="toggleLike(${notice.id})">
                    ❤️ 좋아요 <span id="likeBtnCount">${notice.likeCount}</span>
                </button>
            </div>
            <div style="display:flex; gap:8px;">
                <c:if test="${notice.canEdit}">
                    <a href="/notices/${notice.id}/edit" class="btn btn-secondary">수정</a>
                    <button class="btn btn-danger" onclick="deleteNotice(${notice.id})">삭제</button>
                </c:if>
            </div>
        </div>
    </div>

    <%-- 댓글 섹션 --%>
    <div class="comments-section">
        <h3>💬 댓글 <span id="commentCount">${notice.commentCount}</span>개</h3>

        <c:if test="${isLoggedIn}">
            <div class="comment-form">
                <textarea id="commentInput" placeholder="댓글을 입력하세요..."></textarea>
                <button onclick="submitComment()">등록</button>
            </div>
        </c:if>
        <c:if test="${not isLoggedIn}">
            <p style="color:#888; font-size:0.9rem; margin-bottom:16px;">댓글을 작성하려면 <a href="#" style="color:#4f46e5;">로그인</a>이 필요합니다.</p>
        </c:if>

        <div class="comment-list" id="commentList">
            <div class="no-comments" id="noComment">댓글을 불러오는 중...</div>
        </div>
    </div>
</div>

<%-- 이미지 모달 --%>
<div class="modal-overlay" id="imgModal" onclick="closeModal()">
    <span class="modal-close" onclick="closeModal()">✕</span>
    <img id="modalImg" src="" alt="확대 이미지">
</div>

<script>
    const NOTICE_ID = ${notice.id};
    const IS_LOGGED_IN = ${isLoggedIn};
    const IS_ADMIN = ${isAdmin};

    /* ── 좋아요 토글 ── */
    async function toggleLike(id) {
        if (!IS_LOGGED_IN) { alert('로그인이 필요합니다.'); return; }
        try {
            const res = await fetch("/api/notices/" + id + "/like", { method: "POST", credentials: "include" });
            if (res.status === 401) { alert('로그인이 필요합니다.'); return; }
            const data = await res.json();
            const count = data.likeCount ?? data.count ?? 0;
            document.getElementById('likeCount').textContent = count;
            document.getElementById('likeBtnCount').textContent = count;
            const btn = document.getElementById('likeBtn');
            btn.classList.toggle('liked', data.liked);
        } catch (e) { console.error(e); }
    }

    /* ── 게시글 삭제 ── */
    async function deleteNotice(id) {
        if (!confirm('정말 삭제하시겠습니까?')) return;
        try {
            const res = await fetch("/api/notices/" + id, { method: "DELETE", credentials: "include" });
            if (res.ok) { alert('삭제되었습니다.'); location.href = '/notices'; }
            else { const d = await res.json(); alert(d.message || '삭제에 실패했습니다.'); }
        } catch (e) { console.error(e); alert('오류가 발생했습니다.'); }
    }

    /* ── 댓글 목록 로드 ── */
    async function loadComments() {
        try {
            const res = await fetch("/api/comments/notices/" + NOTICE_ID, { credentials: "include" });
            if (!res.ok) return;
            const comments = await res.json();
            renderComments(comments);
        } catch (e) { console.error(e); }
    }

    function renderComments(comments) {
        const list = document.getElementById('commentList');
        const noComment = document.getElementById('noComment');
        if (!comments || comments.length === 0) {
            noComment.style.display = 'block';
            noComment.textContent = '등록된 댓글이 없습니다.';
            return;
        }
        noComment.style.display = 'none';
        list.innerHTML = '';
        comments.forEach(c => {
            const div = document.createElement('div');
            div.className = 'comment-item';
            div.innerHTML = `
                <div class="c-meta">
                    <span>✍️ <strong>\${c.author || c.username}</strong> &nbsp; \${(c.createdAt || '').substring(0,16)}</span>
                    \${(IS_ADMIN || c.canDelete) ? '<button class="c-del" onclick="deleteComment(' + c.id + ')">삭제</button>' : ''}
                </div>
                <div class="c-body">\${escapeHtml(c.content)}</div>
            `;
            list.appendChild(div);
        });
        document.getElementById('commentCount').textContent = comments.length;
    }

    /* ── 댓글 등록 ── */
    async function submitComment() {
        const input = document.getElementById('commentInput');
        const content = input.value.trim();
        if (!content) { alert('댓글 내용을 입력하세요.'); return; }
        try {
            const res = await fetch("/api/comments/notices/" + NOTICE_ID, {
                method: "POST",
                headers: { 'Content-Type': 'application/json' },
                credentials: "include",
                body: JSON.stringify({ content })
            });
            if (res.status === 401) { alert('로그인이 필요합니다.'); return; }
            if (!res.ok) { const d = await res.json(); alert(d.message || '등록 실패'); return; }
            input.value = '';
            loadComments();
        } catch (e) { console.error(e); alert('오류가 발생했습니다.'); }
    }

    /* ── 댓글 삭제 ── */
    async function deleteComment(id) {
        if (!confirm('댓글을 삭제하시겠습니까?')) return;
        try {
            const res = await fetch("/api/comments/" + id, { method: "DELETE", credentials: "include" });
            if (res.ok || res.status === 204) loadComments();
            else { const d = await res.json(); alert(d.message || '삭제 실패'); }
        } catch (e) { console.error(e); }
    }

    /* ── 이미지 모달 ── */
    function openModal(url) {
        document.getElementById('modalImg').src = url;
        document.getElementById('imgModal').classList.add('active');
    }
    function closeModal() {
        document.getElementById('imgModal').classList.remove('active');
    }
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

    /* ── XSS 방지용 이스케이프 ── */
    function escapeHtml(str) {
        const d = document.createElement('div');
        d.appendChild(document.createTextNode(str || ''));
        return d.innerHTML;
    }

    /* 페이지 로드 시 댓글 불러오기 */
    loadComments();
</script>
</body>
</html>
