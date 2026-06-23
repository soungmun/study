<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${notice.title} - 공지사항</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; color: #333; }
        .container { max-width: 860px; margin: 40px auto; padding: 0 16px; }

        /* 뒤로가기 */
        .back-link { display: inline-flex; align-items: center; gap: 6px; color: #4f46e5;
            text-decoration: none; font-size: 0.9rem; margin-bottom: 20px; }
        .back-link:hover { text-decoration: underline; }

        /* 카드 */
        .card { background: #fff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,.08); overflow: hidden; }
        .card-header { padding: 28px 32px 20px; border-bottom: 1px solid #eee; }
        .card-header h2 { font-size: 1.5rem; color: #1a1a2e; margin-bottom: 12px; line-height: 1.4; }
        .meta { display: flex; gap: 20px; flex-wrap: wrap; font-size: 0.85rem; color: #777; }
        .meta span { display: flex; align-items: center; gap: 4px; }
        .card-body { padding: 28px 32px; }
        .content { font-size: 0.95rem; line-height: 1.8; white-space: pre-wrap; word-break: break-word; min-height: 120px; }

        /* 이미지 */
        .images { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 24px; }
        .images img { max-width: 100%; max-height: 400px; border-radius: 8px;
            object-fit: contain; border: 1px solid #e5e7eb; cursor: pointer; }

        /* 액션 버튼 */
        .actions { display: flex; justify-content: space-between; align-items: center;
            padding: 20px 32px; border-top: 1px solid #eee; background: #fafafa; }
        .actions-left { display: flex; gap: 10px; }
        .btn { display: inline-block; padding: 8px 20px; border-radius: 6px;
            font-size: 0.9rem; text-decoration: none; cursor: pointer; border: none; }
        .btn-primary   { background: #4f46e5; color: #fff; }
        .btn-primary:hover { background: #4338ca; }
        .btn-secondary { background: #f3f4f6; color: #374151; border: 1px solid #d1d5db; }
        .btn-secondary:hover { background: #e5e7eb; }
        .btn-danger    { background: #ef4444; color: #fff; }
        .btn-danger:hover { background: #dc2626; }
        .btn-like { display: inline-flex; align-items: center; gap: 6px;
            padding: 8px 18px; border-radius: 20px; border: 1px solid #db2777;
            color: #db2777; background: #fff; font-size: 0.9rem; cursor: pointer; }
        .btn-like:hover, .btn-like.liked { background: #fce7f3; }
        .btn-like.liked { font-weight: 600; }

        /* 댓글 섹션 */
        .comments-section { margin-top: 32px; }
        .comments-section h3 { font-size: 1.1rem; margin-bottom: 16px; color: #1a1a2e; }
        .comment-form { display: flex; gap: 8px; margin-bottom: 20px; }
        .comment-form textarea {
            flex: 1; padding: 10px 14px; border: 1px solid #ccc; border-radius: 8px;
            font-size: 0.9rem; resize: none; height: 70px; font-family: inherit;
        }
        .comment-form button {
            padding: 0 20px; background: #4f46e5; color: #fff; border: none;
            border-radius: 8px; cursor: pointer; font-size: 0.9rem; white-space: nowrap;
        }
        .comment-form button:hover { background: #4338ca; }
        .comment-list { display: flex; flex-direction: column; gap: 12px; }
        .comment-item { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px 16px; }
        .comment-item .c-meta { font-size: 0.82rem; color: #888; margin-bottom: 6px; display: flex; justify-content: space-between; }
        .comment-item .c-body { font-size: 0.9rem; line-height: 1.6; }
        .comment-item .c-del { color: #ef4444; background: none; border: none; cursor: pointer; font-size: 0.82rem; }
        .no-comments { color: #aaa; font-size: 0.9rem; text-align: center; padding: 24px 0; }

        /* 이미지 모달 */
        .modal-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,.75);
            z-index: 9999; align-items: center; justify-content: center; }
        .modal-overlay.active { display: flex; }
        .modal-overlay img { max-width: 90vw; max-height: 90vh; border-radius: 8px; object-fit: contain; }
        .modal-close { position: fixed; top: 20px; right: 28px; font-size: 2rem;
            color: #fff; cursor: pointer; line-height: 1; }
    </style>
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
            const res = await fetch('/api/notices/' + id + '/like', { method: 'POST', credentials: 'include' });
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
            const res = await fetch('/api/notices/' + id, { method: 'DELETE', credentials: 'include' });
            if (res.ok) { alert('삭제되었습니다.'); location.href = '/notices'; }
            else { const d = await res.json(); alert(d.message || '삭제에 실패했습니다.'); }
        } catch (e) { console.error(e); alert('오류가 발생했습니다.'); }
    }

    /* ── 댓글 목록 로드 ── */
    async function loadComments() {
        try {
            const res = await fetch('/api/comments/notices/' + NOTICE_ID, { credentials: 'include' });
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
            const res = await fetch('/api/comments/notices/' + NOTICE_ID, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
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
            const res = await fetch('/api/comments/' + id, { method: 'DELETE', credentials: 'include' });
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
