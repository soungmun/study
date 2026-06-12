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
  const [likers, setLikers] = useState(null); // null = 미오픈, 배열 = 오픈됨
  const [loadingLikers, setLoadingLikers] = useState(false);
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

  const toggleLikersList = async () => {
    if (likers !== null) { setLikers(null); return; }
    setLoadingLikers(true);
    try {
      const r = await fetch(`${BASE_URL}/${id}/likes`, { credentials: 'include' });
      if (!r.ok) throw new Error(await readError(r));
      const data = await r.json();
      setLikers(Array.isArray(data) ? data : []);
    } catch (e) {
      alert(`좋아요 목록 실패: ${e.message}`);
    } finally {
      setLoadingLikers(false);
    }
  };

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
      // 목록이 펼쳐져 있으면 즉시 갱신
      if (likers !== null) {
        const lr = await fetch(`${BASE_URL}/${id}/likes`, { credentials: 'include' });
        if (lr.ok) setLikers(await lr.json());
      }
    } catch (e) {
      alert(`좋아요 실패: ${e.message}`);
    }
  };

  const remove = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    const r = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE', credentials: 'include' });
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

      {/* 이미지 목록 */}
      {notice.imageUrls && notice.imageUrls.length > 0 && (
        <div style={{ margin: '16px 0', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: '#64748b' }}>첨부 이미지 ({notice.imageUrls.length})</span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            {notice.imageUrls.map((url, i) => (
              <a key={i} href={url} target="_blank" rel="noreferrer" style={{ display: 'block', borderRadius: 8, overflow: 'hidden', border: '1px solid #e2e8f0' }}>
                <img
                  src={url}
                  alt={`첨부 이미지 ${i + 1}`}
                  style={{ width: 180, height: 135, objectFit: 'cover', display: 'block' }}
                />
              </a>
            ))}
          </div>
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', margin: '16px 0', gap: 8 }}>
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

        {(notice.likeCount ?? 0) > 0 && (
          <button
            type="button"
            onClick={toggleLikersList}
            style={{ background: 'transparent', border: 'none', color: '#6366f1', fontSize: 13, cursor: 'pointer', textDecoration: 'underline' }}
          >
            {likers === null ? '누가 좋아했는지 보기' : '닫기'}
          </button>
        )}

        {loadingLikers && <div style={{ color: '#94a3b8', fontSize: 13 }}>불러오는 중…</div>}

        {likers !== null && likers.length > 0 && (
          <div style={{ width: '100%', maxWidth: 480, padding: 10, background: '#f8fafc', borderRadius: 8 }}>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {likers.map((u) => (
                <li
                  key={`${u.userId}-${u.likedAt}`}
                  style={{
                    display: 'inline-flex', alignItems: 'center', gap: 6,
                    background: '#fff', padding: '4px 10px', borderRadius: 999,
                    border: '1px solid #e2e8f0', fontSize: 13, color: '#1e293b',
                  }}
                  title={u.likedAt}
                >
                  {u.profileImage ? (
                    <img src={u.profileImage} alt="" style={{ width: 18, height: 18, borderRadius: '50%' }} />
                  ) : (
                    <span style={{ width: 18, height: 18, borderRadius: '50%', background: '#cbd5e1', display: 'inline-block' }} />
                  )}
                  <span>{u.nickname}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="actions">
        <button type="button" className="ghost" onClick={() => navigate('/')}>목록</button>
        {notice.canEdit && (
          <>
            <button type="button" className="success" onClick={() => navigate(`/notices/${id}/edit`)}>수정</button>
            <button type="button" className="danger" onClick={remove}>삭제</button>
          </>
        )}
      </div>

      <CommentSection noticeId={id} />
    </div>
  );
}