import { useEffect, useState } from 'react';

const API = 'http://localhost:8080/api';

async function readError(res) {
  const text = await res.text();
  try { return JSON.parse(text).message || text; } catch { return text || `HTTP ${res.status}`; }
}

export default function CommentSection({ noticeId }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [draft, setDraft] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editingText, setEditingText] = useState('');
  const [authed, setAuthed] = useState(false);

  // 로그인 여부 확인 (작성 폼 노출용)
  useEffect(() => {
    fetch(`${API}/auth/me`, { credentials: 'include' })
      .then((r) => setAuthed(r.ok))
      .catch(() => setAuthed(false));
  }, []);

  const load = () => {
    setLoading(true);
    setError(null);
    fetch(`${API}/notices/${noticeId}/comments`, { credentials: 'include' })
      .then(async (r) => {
        if (!r.ok) throw new Error(await readError(r));
        return r.json();
      })
      .then((data) => setComments(Array.isArray(data) ? data : []))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [noticeId]);

  const submit = async (e) => {
    e.preventDefault();
    if (!draft.trim()) return;
    setSubmitting(true);
    try {
      const r = await fetch(`${API}/notices/${noticeId}/comments`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: draft.trim() }),
      });
      if (!r.ok) throw new Error(await readError(r));
      const created = await r.json();
      setComments((prev) => [...prev, created]);
      setDraft('');
    } catch (err) {
      alert(`작성 실패: ${err.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  const startEdit = (c) => {
    setEditingId(c.id);
    setEditingText(c.content);
  };

  const saveEdit = async (id) => {
    if (!editingText.trim()) return;
    try {
      const r = await fetch(`${API}/comments/${id}`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: editingText.trim() }),
      });
      if (!r.ok) throw new Error(await readError(r));
      const updated = await r.json();
      setComments((prev) => prev.map((c) => (c.id === id ? updated : c)));
      setEditingId(null);
      setEditingText('');
    } catch (err) {
      alert(`수정 실패: ${err.message}`);
    }
  };

  const toggleLike = async (id) => {
    try {
      const r = await fetch(`${API}/comments/${id}/like`, {
        method: 'POST',
        credentials: 'include',
      });
      if (!r.ok) {
        if (r.status === 401) { alert('로그인이 필요합니다.'); return; }
        throw new Error(await readError(r));
      }
      const { liked, count } = await r.json();
      setComments((prev) => prev.map((c) =>
        c.id === id ? { ...c, iLiked: liked, likeCount: count } : c
      ));
    } catch (err) {
      alert(`좋아요 실패: ${err.message}`);
    }
  };

  const remove = async (id) => {
    if (!confirm('이 댓글을 삭제할까요?')) return;
    try {
      const r = await fetch(`${API}/comments/${id}`, {
        method: 'DELETE',
        credentials: 'include',
      });
      if (!r.ok && r.status !== 204) throw new Error(await readError(r));
      setComments((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      alert(`삭제 실패: ${err.message}`);
    }
  };

  return (
    <div style={{ marginTop: 24, padding: '16px 0', borderTop: '2px solid #e2e8f0' }}>
      <h3 style={{ marginBottom: 12 }}>💬 댓글 {comments.length > 0 && `(${comments.length})`}</h3>

      {loading && <div style={{ color: '#94a3b8', padding: 12 }}>댓글을 불러오는 중…</div>}
      {error && <div className="error">에러: {error}</div>}

      {!loading && comments.length === 0 && !error && (
        <div style={{ color: '#94a3b8', padding: 12, textAlign: 'center' }}>
          아직 댓글이 없습니다. 첫 댓글을 남겨보세요.
        </div>
      )}

      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {comments.map((c) => (
          <li
            key={c.id}
            style={{
              padding: '12px 14px',
              borderBottom: '1px solid #f1f5f9',
              background: c.mine ? '#f8fafc' : 'transparent',
              borderRadius: c.mine ? 8 : 0,
              marginBottom: 4,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
              <div>
                <strong style={{ color: '#1e293b' }}>{c.authorName}</strong>
                <span style={{ color: '#94a3b8', fontSize: 12, marginLeft: 8 }}>
                  {c.createdAt}{c.edited && ' · 수정됨'}
                </span>
              </div>
              {c.mine && editingId !== c.id && (
                <div style={{ display: 'flex', gap: 6 }}>
                  <button className="ghost" style={{ padding: '2px 10px', fontSize: 12 }} onClick={() => startEdit(c)}>수정</button>
                  <button className="danger" style={{ padding: '2px 10px', fontSize: 12 }} onClick={() => remove(c.id)}>삭제</button>
                </div>
              )}
            </div>

            {editingId === c.id ? (
              <div style={{ marginTop: 6 }}>
                <textarea
                  value={editingText}
                  onChange={(e) => setEditingText(e.target.value)}
                  rows={3}
                  maxLength={2000}
                  style={{ width: '100%', padding: 8, border: '1px solid #cbd5e1', borderRadius: 8, fontFamily: 'inherit' }}
                />
                <div style={{ display: 'flex', gap: 6, marginTop: 6, justifyContent: 'flex-end' }}>
                  <button className="ghost" style={{ padding: '4px 12px', fontSize: 13 }} onClick={() => { setEditingId(null); setEditingText(''); }}>취소</button>
                  <button className="primary" style={{ padding: '4px 12px', fontSize: 13 }} onClick={() => saveEdit(c.id)}>저장</button>
                </div>
              </div>
            ) : (
              <>
                <div style={{ whiteSpace: 'pre-wrap', color: '#334155', lineHeight: 1.6 }}>{c.content}</div>
                <div style={{ marginTop: 8 }}>
                  <button
                    type="button"
                    onClick={() => toggleLike(c.id)}
                    style={{
                      display: 'inline-flex', alignItems: 'center', gap: 4,
                      padding: '2px 10px',
                      background: 'transparent',
                      border: c.iLiked ? '1px solid #ef4444' : '1px solid #cbd5e1',
                      color: c.iLiked ? '#ef4444' : '#64748b',
                      borderRadius: 999,
                      fontSize: 12,
                      cursor: 'pointer',
                    }}
                  >
                    <span>{c.iLiked ? '❤️' : '🤍'}</span>
                    <span>{c.likeCount ?? 0}</span>
                  </button>
                </div>
              </>
            )}
          </li>
        ))}
      </ul>

      {authed ? (
        <form onSubmit={submit} style={{ marginTop: 16 }}>
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="댓글을 입력하세요 (최대 2000자)"
            rows={3}
            maxLength={2000}
            style={{ width: '100%', padding: 10, border: '1px solid #cbd5e1', borderRadius: 8, fontFamily: 'inherit', resize: 'vertical' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
            <span style={{ color: '#94a3b8', fontSize: 12 }}>{draft.length} / 2000</span>
            <button type="submit" className="primary" disabled={submitting || !draft.trim()}>
              {submitting ? '등록 중…' : '댓글 등록'}
            </button>
          </div>
        </form>
      ) : (
        <div style={{ marginTop: 16, padding: 12, background: '#f1f5f9', borderRadius: 8, textAlign: 'center', color: '#64748b' }}>
          댓글을 작성하려면 로그인이 필요합니다.
        </div>
      )}
    </div>
  );
}
