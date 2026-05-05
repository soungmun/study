import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const BASE_URL = 'http://localhost:8080/api/notices';

function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleString();
}

function formatViews(n) {
  if (n == null) return 0;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`;
  return n;
}

function badgeClass(id) {
  return `id-badge c${(Number(id) || 0) % 6}`;
}

export default function NoticeList() {
  const navigate = useNavigate();
  const [notices, setNotices] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(BASE_URL)
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then(setNotices)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div className="card">
      <div className="toolbar">
        <h2>공지사항</h2>
        <button className="primary" onClick={() => navigate('/notices/new')}>+ 글쓰기</button>
      </div>
      {error && <div className="error">에러: {error}</div>}
      <table className="notice-table">
        <thead>
          <tr>
            <th style={{ width: 70 }}>번호</th>
            <th>제목</th>
            <th style={{ width: 120 }} className="hide-sm">작성자</th>
            <th style={{ width: 170 }} className="hide-sm">작성일</th>
            <th style={{ width: 80 }}>조회</th>
          </tr>
        </thead>
        <tbody>
          {notices.length === 0 && (
            <tr><td colSpan="5" className="empty">등록된 글이 없습니다.</td></tr>
          )}
          {notices.map((n) => (
            <tr key={n.id} onClick={() => navigate(`/notices/${n.id}`)} className="row">
              <td><span className={badgeClass(n.id)}>{n.id}</span></td>
              <td className="title">{n.title}</td>
              <td className="author hide-sm">{n.author}</td>
              <td className="date hide-sm">{formatDate(n.createdAt)}</td>
              <td className="views">{formatViews(n.viewCount)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}