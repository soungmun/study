import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const BASE_URL = 'http://localhost:8080/api/notices';
const ADMIN_ME_URL = 'http://localhost:8080/api/admin/me';
const PAGE_SIZE = 10;

const PAGE_KEY = 'noticeListPage';
const TYPE_KEY = 'noticeListType';
const KEYWORD_KEY = 'noticeListKeyword';

function badgeClass(n) {
  return `id-badge c${(Number(n) || 0) % 6}`;
}

/* ───────────────────────── 서브 컴포넌트 ───────────────────────── */

function Toolbar({ isAdmin, onWrite }) {
  return (
    <div className="toolbar">
      <h2>공지사항</h2>
      {isAdmin && (
        <button className="primary" onClick={onWrite}>+ 글쓰기</button>
      )}
    </div>
  );
}

function SearchBar({ type, keyword, searchTerm, onTypeChange, onKeywordChange, onSubmit, onReset }) {
  return (
    <form className="search-bar" onSubmit={onSubmit}>
      <select value={type} onChange={onTypeChange} className="search-select">
        <option value="title">제목</option>
        <option value="content">내용</option>
      </select>
      <input
        type="text"
        className="search-input"
        placeholder="검색어를 입력하세요"
        value={keyword}
        onChange={onKeywordChange}
      />
      <button type="submit" className="primary">검색</button>
      {searchTerm && (
        <button type="button" className="ghost" onClick={onReset}>초기화</button>
      )}
    </form>
  );
}

function NoticeRow({ notice, seq, onClick }) {
  return (
    <tr onClick={onClick} className="row">
      <td><span className={badgeClass(seq)}>{seq}</span></td>
      <td className="title">
        <span>{notice.title}</span>
        {notice.commentCount > 0 && (
          <span style={{ marginLeft: 8, fontSize: 12, color: '#6366f1', fontWeight: 600 }}>
            💬 {notice.commentCount}
          </span>
        )}
        {notice.likeCount > 0 && (
          <span style={{ marginLeft: 6, fontSize: 12, color: '#ef4444', fontWeight: 600 }}>
            ❤️ {notice.likeCount}
          </span>
        )}
      </td>
      <td className="author hide-sm">{notice.author}</td>
      <td className="author hide-sm">{notice.content}</td>
      <td className="date hide-sm">{notice.createdAt}</td>
      <td className="views">{notice.viewCountText}</td>
    </tr>
  );
}

function NoticeTable({ notices, pageNo, searchTerm, onRowClick }) {
  return (
    <table className="notice-table">
      <thead>
        <tr>
          <th style={{ width: 70 }}>번호</th>
          <th>제목</th>
          <th style={{ width: 120 }} className="hide-sm">작성자</th>
          <th style={{ width: 120 }} className="hide-sm">내용</th>
          <th style={{ width: 170 }} className="hide-sm">작성일</th>
          <th style={{ width: 80 }}>조회</th>
        </tr>
      </thead>
      <tbody>
        {notices.length === 0 ? (
          <tr>
            <td colSpan="6" className="empty">
              {searchTerm ? '검색 결과가 없습니다.' : '등록된 글이 없습니다.'}
            </td>
          </tr>
        ) : (
          notices.map((n, i) => (
            <NoticeRow
              key={n.id}
              notice={n}
              seq={pageNo * PAGE_SIZE + i + 1}
              onClick={() => onRowClick(n.id)}
            />
          ))
        )}
      </tbody>
    </table>
  );
}

function Pagination({ pageNo, totalPages, isFirst, isLast, onPageChange }) {
  if (totalPages <= 0) return null;
  return (
    <div className="pagination">
      <button onClick={() => onPageChange(0)} disabled={isFirst}>«</button>
      <button onClick={() => onPageChange(pageNo - 1)} disabled={isFirst}>‹</button>
      {Array.from({ length: totalPages }, (_, i) => (
        <button
          key={i}
          className={i === pageNo ? 'page-num active' : 'page-num'}
          onClick={() => onPageChange(i)}
        >
          {i + 1}
        </button>
      ))}
      <button onClick={() => onPageChange(pageNo + 1)} disabled={isLast}>›</button>
      <button onClick={() => onPageChange(totalPages - 1)} disabled={isLast}>»</button>
    </div>
  );
}

/* ───────────────────────── 메인 컴포넌트 ───────────────────────── */

export default function NoticeList() {
  const navigate = useNavigate();
  const [page, setPage] = useState(null);
  const [pageNo, setPageNo] = useState(() => Number(sessionStorage.getItem(PAGE_KEY) ?? 0));
  const [type, setType] = useState(() => sessionStorage.getItem(TYPE_KEY) ?? 'title');
  const [keyword, setKeyword] = useState(() => sessionStorage.getItem(KEYWORD_KEY) ?? '');
  const [searchTerm, setSearchTerm] = useState(() => sessionStorage.getItem(KEYWORD_KEY) ?? '');
  const [error, setError] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);

  // 관리자 여부 확인 — 로그인/로그아웃 시 auth-changed 로 갱신
  useEffect(() => {
    const check = () => {
      fetch(ADMIN_ME_URL, { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : { admin: false }))
        .then((d) => setIsAdmin(!!d.admin))
        .catch(() => setIsAdmin(false));
    };
    check();
    window.addEventListener('auth-changed', check);
    return () => window.removeEventListener('auth-changed', check);
  }, []);

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

  const notices = Array.isArray(page?.content) ? page.content : [];
  const totalPages = Number.isFinite(page?.totalPages) ? page.totalPages : 0;
  const isFirst = page?.first ?? true;
  const isLast = page?.last ?? true;

  return (
    <div className="card">
      <Toolbar isAdmin={isAdmin} onWrite={() => navigate('/notices/new')} />
      <SearchBar
        type={type}
        keyword={keyword}
        searchTerm={searchTerm}
        onTypeChange={(e) => setType(e.target.value)}
        onKeywordChange={(e) => setKeyword(e.target.value)}
        onSubmit={onSearch}
        onReset={onReset}
      />
      {error && <div className="error">에러: {error}</div>}
      <NoticeTable
        notices={notices}
        pageNo={pageNo}
        searchTerm={searchTerm}
        onRowClick={(id) => navigate(`/notices/${id}`)}
      />
      <Pagination
        pageNo={pageNo}
        totalPages={totalPages}
        isFirst={isFirst}
        isLast={isLast}
        onPageChange={setPageNo}
      />
    </div>
  );
}
