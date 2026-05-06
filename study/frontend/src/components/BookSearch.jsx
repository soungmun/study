import { useEffect, useRef, useState } from 'react';

const BASE_URL = 'http://localhost:8080/api/books/search';
const PAGE_SIZE = 12;

const TARGET_OPTIONS = [
  { value: '',          label: '전체' },
  { value: 'title',     label: '제목' },
  { value: 'person',    label: '저자' },
  { value: 'publisher', label: '출판사' },
  { value: 'isbn',      label: 'ISBN' },
];

const SORT_OPTIONS = [
  { value: 'accuracy', label: '정확도순' },
  { value: 'latest',   label: '최신순' },
];

function formatPrice(book) {
  const p = book.sale_price > 0 ? book.sale_price : book.price;
  if (!p || p <= 0) return '가격 정보 없음';
  return `${p.toLocaleString()}원`;
}

function formatDate(datetime) {
  if (!datetime) return '';
  return datetime.substring(0, 10);
}

export default function BookSearch() {
  const [query, setQuery] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [target, setTarget] = useState('');
  const [sort, setSort] = useState('accuracy');
  const [page, setPage] = useState(1);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const topRef = useRef(null);

  useEffect(() => {
    if (!submitted.trim()) {
      setResult(null);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({
      query: submitted.trim(),
      sort,
      page: String(page),
      size: String(PAGE_SIZE),
    });
    if (target) params.set('target', target);

    fetch(`${BASE_URL}?${params.toString()}`, { signal: controller.signal })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setResult(data);
        setLoading(false);
        topRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setError(e.message);
        setLoading(false);
      });

    return () => controller.abort();
  }, [submitted, sort, target, page]);

  const onSearch = (e) => {
    e.preventDefault();
    setPage(1);
    setSubmitted(query);
  };

  const onReset = () => {
    setQuery('');
    setSubmitted('');
    setTarget('');
    setSort('accuracy');
    setPage(1);
    setResult(null);
    setError(null);
  };

  const documents = result?.documents ?? [];
  const meta = result?.meta;
  const isEnd = meta?.is_end ?? true;

  return (
    <div className="card" ref={topRef}>
      <div className="toolbar">
        <h2>📚 책 검색</h2>
        <span className="muted">카카오 도서 검색 API</span>
      </div>

      <form className="search-bar" onSubmit={onSearch}>
        <select
          value={target}
          onChange={(e) => setTarget(e.target.value)}
          className="search-select"
        >
          {TARGET_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
        <select
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          className="search-select"
        >
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
        <input
          type="text"
          className="search-input"
          placeholder="책 제목, 저자, 출판사 등 검색"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" className="primary" disabled={!query.trim()}>검색</button>
        {submitted && (
          <button type="button" className="ghost" onClick={onReset}>초기화</button>
        )}
      </form>

      {error && <div className="error">에러: {error}</div>}

      {!submitted && !loading && (
        <div className="book-empty">
          <div className="book-empty-icon">🔍</div>
          <p>검색어를 입력해 책을 찾아보세요</p>
        </div>
      )}

      {loading && (
        <div className="book-empty">
          <div className="spinner" />
          <p>검색 중…</p>
        </div>
      )}

      {!loading && submitted && documents.length === 0 && !error && (
        <div className="book-empty">
          <div className="book-empty-icon">📭</div>
          <p>"{submitted}"에 대한 검색 결과가 없습니다.</p>
        </div>
      )}

      {!loading && documents.length > 0 && (
        <>
          <div className="book-meta">
            <span>
              총 <strong>{meta.pageable_count.toLocaleString()}</strong>건
            </span>
            <span className="dot">·</span>
            <span>{page} 페이지</span>
          </div>

          <div className="book-grid">
            {documents.map((b, idx) => (
              <a
                key={`${b.isbn}-${idx}`}
                href={b.url}
                target="_blank"
                rel="noopener noreferrer"
                className="book-card"
              >
                <div className="book-thumb">
                  {b.thumbnail ? (
                    <img src={b.thumbnail} alt="" loading="lazy" />
                  ) : (
                    <div className="book-thumb-placeholder">No Image</div>
                  )}
                  {b.status && b.status !== '정상판매' && (
                    <span className="book-status">{b.status}</span>
                  )}
                </div>
                <div className="book-info">
                  <div className="book-title">{b.title}</div>
                  <div className="book-authors">
                    {(b.authors ?? []).join(', ') || '저자 미상'}
                  </div>
                  <div className="book-sub">
                    {b.publisher && <span>{b.publisher}</span>}
                    {b.datetime && <span className="muted">{formatDate(b.datetime)}</span>}
                  </div>
                  <div className="book-price">{formatPrice(b)}</div>
                </div>
              </a>
            ))}
          </div>

          <div className="pagination">
            <button onClick={() => setPage(1)} disabled={page === 1}>«</button>
            <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}>‹</button>
            <span className="page-num active">{page}</span>
            <button onClick={() => setPage((p) => p + 1)} disabled={isEnd}>›</button>
          </div>
        </>
      )}
    </div>
  );
}