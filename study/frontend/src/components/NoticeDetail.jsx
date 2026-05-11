import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import CommentSection from './CommentSection';

const BASE_URL = 'http://localhost:8080/api/notices';

async function readError(res) {
  const text = await res.text();
  try { return JSON.parse(text).message || text; } catch { return text || `HTTP ${res.status}`; }
}

export default function NoticeDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [notice, setNotice] = useState(null);
  const [error, setError] = useState(null);
  const fetchedIdRef = useRef(null);

  useEffect(() => {
    if (fetchedIdRef.current === id) return;
    fetchedIdRef.current = id;
    fetch(`${BASE_URL}/${id}/views`, { method: 'POST', credentials: 'include' })
      .then(async (r) => {
        if (!r.ok) throw new Error(await readError(r));
        return r.json();
      })
      .then(setNotice)
      .catch((e) => setError(e.message));
  }, [id]);

  const toggleLike = async () => {
    try {
      const r = await fetch(`${BASE_URL}/${id}/like`, {
        method: 'POST',
        credentials: 'include',
      });
      if (!r.ok) {
        const msg = await readError(r);
        if (r.status === 401) {
          alert('로그인이 필요합니다.');
        } else {
          alert(`좋아요 실패: ${msg}`);
        }
        return;
      }
      const { liked, count } = await r.json();
      setNotice((prev) => prev && { ...prev, iLiked: liked, likeCount: count });
    } catch (e) {
      alert(`좋아요 실패: ${e.message}`);
    }
  };

  const remove = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    const r = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
    if (!r.ok) {
      alert(`삭제 실패: ${await readError(r)}`);
      return;
    }
    alert('삭제되었습니다.');
    navigate('/');
  };

  if (error) return <div className="card"><div className="error">에러: {error}</div></div>;
  if (!notice) return <div className="card">로딩중...</div>;

  return (
    <div className="card">
      <div className="detail-header">
        <h2>{notice.title}</h2>
      </div>
      <div className="meta">
        <span className="chip author">작성자 · {notice.author}</span>
        <span className="chip date">{notice.createdAt}</span>
        <span className="chip views">조회 {notice.viewCountText}</span>
      </div>
      <pre className="content">{notice.content}</pre>

      <div style={{ display: 'flex', justifyContent: 'center', margin: '16px 0' }}>
        <button
          type="button"
          onClick={toggleLike}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: 8,
            padding: '10px 22px',
            border: notice.iLiked ? '2px solid #ef4444' : '2px solid #cbd5e1',
            background: notice.iLiked ? '#fef2f2' : '#fff',
            color: notice.iLiked ? '#ef4444' : '#475569',
            borderRadius: 999,
            fontWeight: 700,
            fontSize: 15,
            cursor: 'pointer',
            transition: 'transform 0.1s',
          }}
          onMouseDown={(e) => { e.currentTarget.style.transform = 'scale(0.95)'; }}
          onMouseUp={(e) => { e.currentTarget.style.transform = 'scale(1)'; }}
          onMouseLeave={(e) => { e.currentTarget.style.transform = 'scale(1)'; }}
        >
          <span style={{ fontSize: 18 }}>{notice.iLiked ? '❤️' : '🤍'}</span>
          <span>좋아요</span>
          <span style={{ background: notice.iLiked ? '#ef4444' : '#94a3b8', color: '#fff', padding: '1px 10px', borderRadius: 999, fontSize: 13 }}>
            {notice.likeCount ?? 0}
          </span>
        </button>
      </div>

      <div className="actions">
        <button type="button" className="ghost" onClick={() => navigate('/')}>목록</button>
        <button type="button" className="success" onClick={() => navigate(`/notices/${id}/edit`)}>수정</button>
        <button type="button" className="danger" onClick={remove}>삭제</button>
      </div>

      <CommentSection noticeId={id} />
    </div>
  );
}