import { useEffect, useState } from 'react';

const GAINERS_URL = 'http://localhost:8080/api/stocks/top-gainers';
const LOSERS_URL  = 'http://localhost:8080/api/stocks/top-losers';

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

export default function StockTopList() {
  const [mode, setMode] = useState('gainers'); // 'gainers' | 'losers'
  const [limit, setLimit] = useState(10);
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [updatedAt, setUpdatedAt] = useState(null);

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
            <tr key={`${q.name}-${i}`} className="row">
              <td><span className={`id-badge c${i % 6}`}>{i + 1}</span></td>
              <td className="title">{q.name}</td>
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
      </p>
    </div>
  );
}
