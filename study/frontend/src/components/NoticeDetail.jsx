import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

const BASE_URL = 'http://localhost:8080/api/notices';

function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleString();
}

async function readError(res) {
  const text = await res.text();
  try { return JSON.parse(text).message || text; } catch { return text || `HTTP ${res.status}`; }
}

export default function NoticeDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [notice, setNotice] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(`${BASE_URL}/${id}`)
      .then(async (r) => {
        if (!r.ok) throw new Error(await readError(r));
        return r.json();
      })
      .then(setNotice)
      .catch((e) => setError(e.message));
  }, [id]);

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
        <span className="chip date">{formatDate(notice.createdAt)}</span>
        <span className="chip views">조회 {notice.viewCount ?? 0}</span>
      </div>
      <pre className="content">{notice.content}</pre>
      <div className="actions">
        <button type="button" className="ghost" onClick={() => navigate('/')}>목록</button>
        <button type="button" className="success" onClick={() => navigate(`/notices/${id}/edit`)}>수정</button>
        <button type="button" className="danger" onClick={remove}>삭제</button>
      </div>
    </div>
  );
}