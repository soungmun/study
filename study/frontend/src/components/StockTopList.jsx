import { useEffect, useState } from 'react';

const GAINERS_URL = 'http://localhost:8080/api/stocks/top-gainers';
const LOSERS_URL  = 'http://localhost:8080/api/stocks/top-losers';
const NEWS_URL    = 'http://localhost:8080/api/stocks/news';

function fmtPrice(v) {
  if (v == null) return '-';
  if (Math.abs(v) < 100000 && Math.floor(v) !== v) {
    return v.toLocaleString('en-US', { maximumFractionDigits: 2, minimumFractionDigits: 2 });
  }
  return Math.round(v).toLocaleString('en-US');
}

function fmtChange(v) {
  if (v == null) return '-';
  const sign = v > 0 ? '+' : '';
  return sign + fmtPrice(v);
}

function fmtPercent(v) {
  if (v == null) return '-';
  const sign = v > 0 ? '+' : '';
  return `${sign}${v.toFixed(2)}%`;
}

function changeColor(v) {
  if (v == null || v === 0) return '#64748b';
  return v > 0 ? '#ef4444' : '#2563eb';
}

// 뉴스 모달 컴포넌트
function NewsModal({ stockName, onClose }) {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetch(`${NEWS_URL}?name=${encodeURIComponent(stockName)}&limit=10`, { credentials: 'include' })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => setNews(Array.isArray(data) ? data : []))
      .catch((e) => setError(e.message || '뉴스 불러오기 실패'))
      .finally(() => setLoading(false));
  }, [stockName]);

  // 모달 바깥 클릭 시 닫기
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) onClose();
  };

  return (
    <div
      onClick={handleBackdropClick}
      style={{
        position: 'fixed', inset: 0, zIndex: 1000,
        background: 'rgba(0,0,0,0.45)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}
    >
      <div style={{
        background: '#fff', borderRadius: 12, padding: '28px 32px',
        width: '100%', maxWidth: 560, maxHeight: '80vh',
        display: 'flex', flexDirection: 'column', boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
      }}>
        {/* 헤더 */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>
            📰 {stockName} 관련 뉴스
          </h3>
          <button
            onClick={onClose}
            style={{
              background: 'none', border: 'none', fontSize: 20,
              cursor: 'pointer', color: '#64748b', lineHeight: 1,
            }}
            aria-label="닫기"
          >✕</button>
        </div>

        {/* 본문 */}
        <div style={{ overflowY: 'auto', flex: 1 }}>
          {loading && <p style={{ color: '#94a3b8', textAlign: 'center' }}>뉴스 불러오는 중…</p>}
          {error && <p style={{ color: '#ef4444' }}>에러: {error}</p>}
          {!loading && !error && news.length === 0 && (
            <p style={{ color: '#94a3b8', textAlign: 'center' }}>관련 뉴스가 없습니다.</p>
          )}
          <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
            {news.map((item, i) => (
              <li key={i} style={{
                padding: '12px 0',
                borderBottom: i < news.length - 1 ? '1px solid #f1f5f9' : 'none',
              }}>
                <a
                  href={item.link}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1e293b', textDecoration: 'none', fontWeight: 500, lineHeight: 1.5 }}
                >
                  {item.title}
                </a>
                {item.source && (
                  <span style={{ display: 'block', color: '#94a3b8', fontSize: 12, marginTop: 4 }}>
                    {item.source}
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}

export default function StockTopList() {
  const [mode, setMode] = useState('gainers'); // 'gainers' | 'losers'
  const [limit, setLimit] = useState(10);
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [updatedAt, setUpdatedAt] = useState(null);
  const [selectedStock, setSelectedStock] = useState(null); // 뉴스 모달용

  const load = () => {
    const url = mode === 'gainers' ? GAINERS_URL : LOSERS_URL;
    setLoading(true);
    setError(null);
    fetch(`${url}?limit=${limit}`, { credentials: 'include' })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setList(Array.isArray(data) ? data : []);
        setUpdatedAt(new Date());
      })
      .catch((e) => setError(e.message || '불러오기 실패'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [mode, limit]);

  return (
    <>
      <div className="card">
        <div className="toolbar">
          <h2>📈 한국 시장 {mode === 'gainers' ? '상승률' : '하락률'} TOP {limit}</h2>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              className={mode === 'gainers' ? 'primary' : 'ghost'}
              onClick={() => setMode('gainers')}
            >🔺 상승률</button>
            <button
              className={mode === 'losers' ? 'primary' : 'ghost'}
              onClick={() => setMode('losers')}
            >🔻 하락률</button>
            <select
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value))}
              className="search-select"
            >
              <option value={5}>TOP 5</option>
              <option value={10}>TOP 10</option>
              <option value={20}>TOP 20</option>
              <option value={30}>TOP 30</option>
            </select>
            <button className="ghost" onClick={load} disabled={loading}>
              {loading ? '불러오는 중…' : '새로고침'}
            </button>
          </div>
        </div>

        {error && <div className="error">에러: {error}</div>}

        <table className="notice-table">
          <thead>
            <tr>
              <th style={{ width: 60 }}>순위</th>
              <th>종목</th>
              <th style={{ width: 130, textAlign: 'right' }}>가격</th>
              <th style={{ width: 130, textAlign: 'right' }}>변동</th>
              <th style={{ width: 110, textAlign: 'right' }}>등락률</th>
            </tr>
          </thead>
          <tbody>
            {list.length === 0 && !loading && (
              <tr><td colSpan="5" className="empty">데이터가 없습니다.</td></tr>
            )}
            {list.map((q, i) => (
              <tr
                key={`${q.name}-${i}`}
                className="row"
                onClick={() => setSelectedStock(q.name)}
                style={{ cursor: 'pointer' }}
                title={`${q.name} 뉴스 보기`}
              >
                <td><span className={`id-badge c${i % 6}`}>{i + 1}</span></td>
                <td className="title" style={{ color: '#2563eb', fontWeight: 600 }}>
                  {q.name} <span style={{ fontSize: 11, color: '#94a3b8', fontWeight: 400 }}>📰</span>
                </td>
                <td style={{ textAlign: 'right' }}>{fmtPrice(q.price)}</td>
                <td style={{ textAlign: 'right', color: changeColor(q.change), fontWeight: 600 }}>
                  {fmtChange(q.change)}
                </td>
                <td style={{ textAlign: 'right', color: changeColor(q.changePercent), fontWeight: 600 }}>
                  {fmtPercent(q.changePercent)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <p style={{ color: '#94a3b8', fontSize: 12, marginTop: 8 }}>
          출처: Yahoo Finance · 큐레이션 종목(KOSPI 34 + KOSDAQ 6) 기준
          {updatedAt && ` · ${updatedAt.toLocaleTimeString()} 기준`}
          {' · 종목을 클릭하면 관련 뉴스를 확인할 수 있습니다.'}
        </p>
      </div>

      {/* 뉴스 모달 */}
      {selectedStock && (
        <NewsModal
          stockName={selectedStock}
          onClose={() => setSelectedStock(null)}
        />
      )}
    </>
  );
}
