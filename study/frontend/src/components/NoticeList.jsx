import { useEffect, useState, memo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../utils/api';

const PAGE_SIZE = 10;

const PAGE_KEY = 'noticeListPage';
const TYPE_KEY = 'noticeListType';
const KEYWORD_KEY = 'noticeListKeyword';

function badgeClass(n) {
  return `id-badge c${(Number(n) || 0) % 6}`;
}

/* ───────────────────────── 서브 컴포넌트 ───────────────────────── */

const Toolbar = memo(function Toolbar({ isAdmin, onWrite }) {
  return (
    <div className="toolbar">
      <h2>공지사항</h2>
      {isAdmin && (
        <button className="primary" onClick={onWrite}>+ 글쓰기</button>
      )}
    </div>
  );
});

const SearchBar = memo(function SearchBar({ type, keyword, searchTerm, onTypeChange, onKeywordChange, onSubmit, onReset }) {
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
});

const NoticeRow = memo(function NoticeRow({ notice, onClick }) {
  return (
    <tr onClick={onClick} className="row">
      <td><span className={badgeClass(notice.id)}>{notice.id}</span></td>
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
      <td className="date hide-sm">{notice.createdAt}</td>
      <td className="views">{notice.viewCountText}</td>
    </tr>
  );
});

const NoticeTable = memo(function NoticeTable({ notices, searchTerm, onRowClick, isLoading }) {
  return (
    <table className="notice-table">
      <thead>
        <tr>
          <th style={{ width: 70 }}>번호</th>
          <th>제목</th>
          <th style={{ width: 120 }} className="hide-sm">작성자</th>
          <th style={{ width: 170 }} className="hide-sm">작성일</th> {/* No change needed here, it's consistent with NoticeRow */}
          <th style={{ width: 80 }}>조회</th>
        </tr>
      </thead>
      <tbody>
        {isLoading ? (
          <tr>
            <td colSpan="5" className="empty" style={{ padding: '40px 0' }}> {/* colSpan is correct for 5 columns */}
              <div className="spinner" style={{ margin: '0 auto 10px' }} />
              <p style={{ margin: 0, color: '#94a3b8' }}>로딩 중입니다...</p>
            </td>
          </tr>
        ) : notices.length === 0 ? (
          <tr>
            <td colSpan="5" className="empty">
              {searchTerm ? '검색 결과가 없습니다.' : '등록된 글이 없습니다.'} {/* colSpan is correct for 5 columns */}
            </td>
          </tr>
        ) : (
          notices.map((n) => (
            <NoticeRow
              key={n.id}
              notice={n}
              onClick={() => onRowClick(n.id)}
            />
          ))
        )}
      </tbody>
    </table>
  );
});

const Pagination = memo(function Pagination({ pageNo, totalPages, isFirst, isLast, onPageChange }) {
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
});

/* ───────────────────────── 메인 컴포넌트 ───────────────────────── */

export default function NoticeList() {
  const navigate = useNavigate();
  const { isAdmin } = useAuth();
  const [page, setPage] = useState(null);
  const [pageNo, setPageNo] = useState(() => Number(sessionStorage.getItem(PAGE_KEY) ?? 0));
  const [type, setType] = useState(() => sessionStorage.getItem(TYPE_KEY) ?? 'title');
  const [keyword, setKeyword] = useState(() => sessionStorage.getItem(KEYWORD_KEY) ?? '');
  const [searchTerm, setSearchTerm] = useState(() => sessionStorage.getItem(KEYWORD_KEY) ?? '');
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  // 실무 API 연동 패턴: 의존성 변경 시 AbortController를 통한 경합 상태(Race Condition) 완벽 통제 및 로딩 처리
  useEffect(() => {
    sessionStorage.setItem(PAGE_KEY, String(pageNo));
    sessionStorage.setItem(TYPE_KEY, type);
    sessionStorage.setItem(KEYWORD_KEY, searchTerm);

    const controller = new AbortController();
    const params = new URLSearchParams({
      page: String(pageNo),
      size: String(PAGE_SIZE),
      type,
    });
    if (searchTerm.trim()) params.set('keyword', searchTerm.trim());
    
    setIsLoading(true);
    setError(null);

    api.get(`/notices?${params.toString()}`, { signal: controller.signal })
      .then((data) => {
        setPage(data);
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [pageNo, type, searchTerm]);

  // 하위 컴포넌트 전달용 이벤트 핸들러들은 useCallback으로 메모이제이션 처리 (리렌더링 최소화)
  const onSearch = useCallback((e) => {
    e.preventDefault();
    setPageNo(0);
    setSearchTerm(keyword);
  }, [keyword]);

  const onReset = useCallback(() => {
    setKeyword('');
    setSearchTerm('');
    setType('title');
    setPageNo(0);
  }, []);

  const handleRowClick = useCallback((id) => {
    navigate(`/notices/${id}`);
  }, [navigate]);

  const handleWriteClick = useCallback(() => {
    navigate('/notices/new');
  }, [navigate]);

  const handleTypeChange = useCallback((e) => {
    setType(e.target.value);
  }, []);

  const handleKeywordChange = useCallback((e) => {
    setKeyword(e.target.value);
  }, []);

  const notices = Array.isArray(page?.content) ? page.content : [];
  const totalPages = Number.isFinite(page?.totalPages) ? page.totalPages : 0;
  const isFirst = page?.first ?? true;
  const isLast = page?.last ?? true;

  return (
    <div className="card">
      <Toolbar isAdmin={isAdmin} onWrite={handleWriteClick} />
      <SearchBar
        type={type}
        keyword={keyword}
        searchTerm={searchTerm}
        onTypeChange={handleTypeChange}
        onKeywordChange={handleKeywordChange}
        onSubmit={onSearch}
        onReset={onReset}
      />
      {error && <div className="error">에러: {error}</div>}
      <NoticeTable
        notices={notices}
        searchTerm={searchTerm}
        onRowClick={handleRowClick}
        isLoading={isLoading}
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
