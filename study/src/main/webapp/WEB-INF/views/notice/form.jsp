<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:choose><c:when test="${mode == 'edit'}">공지사항 수정</c:when><c:otherwise>공지사항 작성</c:otherwise></c:choose></title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; color: #333; }
        .container { max-width: 820px; margin: 40px auto; padding: 0 16px; }
        h1 { font-size: 1.6rem; margin-bottom: 28px; color: #1a1a2e; }

        .back-link { display: inline-flex; align-items: center; gap: 6px; color: #4f46e5;
            text-decoration: none; font-size: 0.9rem; margin-bottom: 20px; }
        .back-link:hover { text-decoration: underline; }

        .card { background: #fff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,.08); padding: 36px 40px; }

        .form-group { margin-bottom: 22px; }
        .form-group label { display: block; font-size: 0.88rem; font-weight: 600; color: #374151; margin-bottom: 6px; }
        .form-group input[type=text],
        .form-group textarea {
            width: 100%; padding: 10px 14px; border: 1px solid #d1d5db; border-radius: 8px;
            font-size: 0.95rem; font-family: inherit; transition: border-color .2s;
            outline: none;
        }
        .form-group input[type=text]:focus,
        .form-group textarea:focus { border-color: #4f46e5; box-shadow: 0 0 0 3px rgba(79,70,229,.12); }
        .form-group textarea { resize: vertical; min-height: 260px; }
        .char-count { font-size: 0.78rem; color: #9ca3af; text-align: right; margin-top: 4px; }

        /* 이미지 업로드 */
        .upload-area { border: 2px dashed #d1d5db; border-radius: 10px; padding: 24px;
            text-align: center; cursor: pointer; transition: border-color .2s; }
        .upload-area:hover { border-color: #4f46e5; background: #f5f3ff; }
        .upload-area input[type=file] { display: none; }
        .upload-area p { color: #6b7280; font-size: 0.9rem; }
        .upload-area p span { color: #4f46e5; font-weight: 600; }

        .preview-list { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 16px; }
        .preview-item { position: relative; }
        .preview-item img { width: 120px; height: 120px; object-fit: cover; border-radius: 8px;
            border: 1px solid #e5e7eb; }
        .preview-item .remove-btn {
            position: absolute; top: -8px; right: -8px; width: 22px; height: 22px;
            background: #ef4444; color: #fff; border: none; border-radius: 50%;
            cursor: pointer; font-size: 0.75rem; display: flex; align-items: center; justify-content: center;
            line-height: 1;
        }
        .preview-item .remove-btn:hover { background: #dc2626; }

        /* 버튼 */
        .btn-row { display: flex; justify-content: flex-end; gap: 10px; margin-top: 10px; }
        .btn { display: inline-block; padding: 10px 28px; border-radius: 8px;
            font-size: 0.95rem; cursor: pointer; border: none; font-family: inherit; }
        .btn-primary   { background: #4f46e5; color: #fff; }
        .btn-primary:hover { background: #4338ca; }
        .btn-secondary { background: #f3f4f6; color: #374151; border: 1px solid #d1d5db; text-decoration: none; }
        .btn-secondary:hover { background: #e5e7eb; }
        .btn:disabled { opacity: .6; cursor: not-allowed; }

        .error-msg { color: #ef4444; font-size: 0.85rem; margin-top: 4px; display: none; }
    </style>
</head>
<body>
<div class="container">
    <%-- 돌아가기/취소 URL: mode에 따라 다르게 --%>
    <c:url var="backUrl" value="/notices">
        <c:if test="${mode == 'edit'}">
            <c:param name="id" value="${notice.id}"/>
        </c:if>
    </c:url>
    <a href="${backUrl}" class="back-link">← ${mode == 'edit' ? '돌아가기' : '목록으로'}</a>
    <h1><c:choose><c:when test="${mode == 'edit'}">✏️ 공지사항 수정</c:when><c:otherwise>📝 공지사항 작성</c:otherwise></c:choose></h1>

    <div class="card">
        <div class="form-group">
            <label for="author">작성자</label>
            <input type="text" id="author" maxlength="100"
                   value="<c:out value='${mode == "edit" ? notice.author : ""}'/>"
                   placeholder="작성자 이름을 입력하세요">
            <div class="char-count"><span id="authorLen">0</span> / 100</div>
            <div class="error-msg" id="authorErr">작성자를 입력하세요.</div>
        </div>

        <div class="form-group">
            <label for="title">제목</label>
            <input type="text" id="title" maxlength="200"
                   value="<c:out value='${mode == "edit" ? notice.title : ""}'/>"
                   placeholder="제목을 입력하세요">
            <div class="char-count"><span id="titleLen">0</span> / 200</div>
            <div class="error-msg" id="titleErr">제목을 입력하세요.</div>
        </div>

        <div class="form-group">
            <label for="content">내용</label>
            <textarea id="content" maxlength="10000"
                      placeholder="내용을 입력하세요..."><c:out value='${mode == "edit" ? notice.content : ""}'/></textarea>
            <div class="char-count"><span id="contentLen">0</span> / 10000</div>
            <div class="error-msg" id="contentErr">내용을 입력하세요.</div>
        </div>

        <div class="form-group">
            <label>이미지 첨부 (JPEG, PNG, GIF, WEBP / 최대 10MB)</label>
            <div class="upload-area" onclick="document.getElementById('fileInput').click()">
                <input type="file" id="fileInput" accept="image/jpeg,image/png,image/gif,image/webp" multiple>
                <p>클릭하여 이미지를 선택하거나 <span>드래그 앤 드롭</span>하세요</p>
            </div>
            <div class="preview-list" id="previewList"></div>
        </div>

        <div class="btn-row">
            <%-- 취소 버튼 --%>
            <a href="${backUrl}" class="btn btn-secondary">취소</a>
            <button class="btn btn-primary" id="submitBtn" onclick="submitForm()">
                <c:choose><c:when test="${mode == 'edit'}">수정 완료</c:when><c:otherwise>등록</c:otherwise></c:choose>
            </button>
        </div>
    </div>
</div>

<script>
    const MODE = '${mode}';
    const NOTICE_ID = ${mode == 'edit' ? notice.id : 'null'};

    // 수정 모드일 때 기존 이미지 ID 목록
    const existingImages = [
        <c:if test="${mode == 'edit' and not empty notice.images}">
            <c:forEach var="img" items="${notice.images}" varStatus="st">
                { id: ${img.imageId}, url: '${img.url}', name: '${img.originalName}' }<c:if test="${!st.last}">,</c:if>
            </c:forEach>
        </c:if>
    ];

    // 현재 첨부된 이미지 목록 { id, url, name, isNew }
    let attachedImages = existingImages.map(i => ({ ...i, isNew: false }));

    /* ── 초기화: 기존 이미지 미리보기 렌더링 ── */
    function initPreviews() {
        updateCharCount('author', 'authorLen');
        updateCharCount('title', 'titleLen');
        updateCharCount('content', 'contentLen');
        renderPreviews();
    }

    /* ── 문자 수 카운터 ── */
    function updateCharCount(inputId, countId) {
        const el = document.getElementById(inputId);
        const counter = document.getElementById(countId);
        counter.textContent = el.value.length;
        el.addEventListener('input', () => { counter.textContent = el.value.length; });
    }

    /* ── 파일 선택 ── */
    document.getElementById('fileInput').addEventListener('change', async function () {
        for (const file of Array.from(this.files)) {
            await uploadFile(file);
        }
        this.value = ''; // 동일 파일 재선택 허용
    });

    /* 드래그 앤 드롭 */
    const uploadArea = document.querySelector('.upload-area');
        }
    });

    /* ── 이미지 서버 업로드 ── */
    async function uploadFile(file) {
        const btn = document.getElementById('submitBtn');
        btn.disabled = true;
        try {
            const formData = new FormData();
            formData.append('file', file);
            const res = await fetch('/api/notices/images', {
                method: 'POST',
                credentials: 'include',
                body: formData
            });
            if (res.status === 401) { alert('로그인이 필요합니다.'); return; }
            if (!res.ok) { const d = await res.json(); alert(d.message || '업로드 실패'); return; }
            const data = await res.json();
            attachedImages.push({ id: data.id, url: data.url, name: file.name, isNew: true });
            renderPreviews();
        } catch (e) {
            console.error(e);
            alert('이미지 업로드 중 오류가 발생했습니다.');
        } finally {
            btn.disabled = false;
        }
    }

    /* ── 미리보기 렌더링 ── */
    function renderPreviews() {
        const list = document.getElementById('previewList');
        list.innerHTML = '';
        attachedImages.forEach((img, idx) => {
            const div = document.createElement('div');
            div.className = 'preview-item';
            div.innerHTML =
                '<img src="' + img.url + '" alt="' + escapeHtml(img.name) + '" title="' + escapeHtml(img.name) + '">' +
                '<button class="remove-btn" onclick="removeImage(' + idx + ')" type="button">✕</button>';
            list.appendChild(div);
        });
    }

    /* ── 이미지 제거 ── */
    function removeImage(idx) {
        attachedImages.splice(idx, 1);
        renderPreviews();
    }

    /* ── 폼 유효성 검사 ── */
    function validate() {
        let ok = true;
        ['author', 'title', 'content'].forEach(id => {
            const el = document.getElementById(id);
            const err = document.getElementById(id + 'Err');
            if (!el.value.trim()) { err.style.display = 'block'; ok = false; }
            else { err.style.display = 'none'; }
        });
        return ok;
    }

    /* ── 폼 제출 ── */
    async function submitForm() {
        if (!validate()) return;

        const btn = document.getElementById('submitBtn');
        btn.disabled = true;

        const payload = {
            author:   document.getElementById('author').value.trim(),
            title:    document.getElementById('title').value.trim(),
            content:  document.getElementById('content').value.trim(),
            imageIds: attachedImages.map(i => i.id)
        };

        try {
            let res;
            if (MODE === 'edit') {
                res = await fetch('/api/notices/' + NOTICE_ID, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(payload)
                });
            } else {
                res = await fetch('/api/notices', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(payload)
                });
            }

            if (res.status === 401) { alert('로그인이 필요합니다.'); return; }
            if (res.status === 403) { alert('권한이 없습니다.'); return; }

            if (!res.ok) {
                const d = await res.json();
                alert(d.message || '저장에 실패했습니다.');
                return;
            }

            const saved = await res.json();
            alert(MODE === 'edit' ? '수정되었습니다.' : '등록되었습니다.');
            location.href = '/notices/' + (saved.id || NOTICE_ID);
        } catch (e) {
            console.error(e);
            alert('오류가 발생했습니다.');
        } finally {
            btn.disabled = false;
        }
    }

    function escapeHtml(str) {
        const d = document.createElement('div');
        d.appendChild(document.createTextNode(str || ''));
        return d.innerHTML;
    }

    /* 입력 시 오류 메시지 숨김 */
    ['author', 'title', 'content'].forEach(id => {
        document.getElementById(id).addEventListener('input', () => {
            document.getElementById(id + 'Err').style.display = 'none';
        });
    });

    initPreviews();
</script>
</body>
</html>
