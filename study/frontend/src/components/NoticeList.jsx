import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const BASE_URL = 'http://localhost:8080/api/notices';
const PAGE_SIZE = 10;

function badgeClass(n) {
  return `id-badge c${(Number(n) || 0) % 6}`;
}

const PAGE_KEY = 'noticeListPage';
const TYPE_KEY = 'noticeListType';
const KEYWORD_KEY = 'noticeListKeyword';

export default function NoticeList() {
  const navigate = useNavigate();
  const [page, setPage] = useState(null);
  const [pageNo, setPageNo] = useState(() => Number(sessionStorage.getItem(PAGE_KEY) ?? 0));
  const [type, setType] = useState(() => sessionStorage.getItem(TYPE_KEY) ?? 'title');
  const [keyword, setKeyword] = useState(() => sessionStorage.getItem(KEYWORD_KEY) ?? '');
  const [searchTerm, setSearchTerm] = useState(() => sessionStorage.getItem(KEYWORD_KEY) ?? '');
  const [error, setError] = useState(null);

  useEffect(() => {
    sessionStorage.setItem(PAGE_KEY, String(pageNo));
    sessionStorage.setItem(TYPE_KEY, type);
    sessionStorage.setItem(KEYWORD_KEY, searchTerm);
    const params = new URLSearchParams({
      page: String(pageNo),
      size: String(PAGE_SIZE),
      type,
    });
    if (searchTerm.trim()) params.set('keyword', searchTerm.trim());
    fetch(`${BASE_URL}?${params.toString()}`)
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then(setPage)
      .catch((e) => setError(e.message));
  }, [pageNo, type, searchTerm]);

  const onSearch = (e) => {
    e.preventDefault();
    setPageNo(0);
    setSearchTerm(keyword);
  };

  const onReset = () => {
    setKeyword('');
    setSearchTerm('');
    setType('title');
    setPageNo(0);
  };

  const notices = page?.content ?? [];
  const totalPages = page?.totalPages ?? 0;
  const isFirst = page?.first ?? true;
  const isLast = page?.last ?? true;

  return (
    <div className="card">
      <div className="toolbar">
        <h2>공지사항</h2>
        <button className="primary" onClick={() => navigate('/notices/new')}>+ 글쓰기</button>
      </div>
      <form className="search-bar" onSubmit={onSearch}>
        <select value={type} onChange={(e) => setType(e.target.value)} className="search-select">
          <option value="title">제목</option>
          <option value="content">내용</option>
        </select>
        <input
          type="text"
          className="search-input"
          placeholder="검색어를 입력하세요"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button type="submit" className="primary">검색</button>
        {searchTerm && (
          <button type="button" className="ghost" onClick={onReset}>초기화</button>
        )}
      </form>
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
            <tr><td colSpan="5" className="empty">
              {searchTerm ? '검색 결과가 없습니다.' : '등록된 글이 없습니다.'}
            </td></tr>
          )}
          {notices.map((n, i) => {
            const seq = pageNo * PAGE_SIZE + i + 1;
            return (
              <tr key={n.id} onClick={() => navigate(`/notices/${n.id}`)} className="row">
                <td><span className={badgeClass(seq)}>{seq}</span></td>
                <td className="title">{n.title}</td>
                <td className="author hide-sm">{n.author}</td>
                <td className="date hide-sm">{n.createdAt}</td>
                <td className="views">{n.viewCountText}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {totalPages > 0 && (
        <div className="pagination">
          <button onClick={() => setPageNo(0)} disabled={isFirst}>«</button>
          <button onClick={() => setPageNo((p) => p - 1)} disabled={isFirst}>‹</button>
          {Array.from({ length: totalPages }, (_, i) => (
            <button
              key={i}
              className={i === pageNo ? 'page-num active' : 'page-num'}
              onClick={() => setPageNo(i)}
            >{i + 1}</button>
          ))}
          <button onClick={() => setPageNo((p) => p + 1)} disabled={isLast}>›</button>
          <button onClick={() => setPageNo(totalPages - 1)} disabled={isLast}>»</button>
        </div>
      )}
    </div>
  );
}