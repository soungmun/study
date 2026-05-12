import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

const API = 'http://localhost:8080/api/payments/kakao';

const STATUS_META = {
  READY: { label: '결제 대기', color: '#64748b', bg: '#f1f5f9' },
  APPROVED: { label: '승인 완료', color: '#047857', bg: '#d1fae5' },
  CANCELED: { label: '취소', color: '#b45309', bg: '#fef3c7' },
  FAILED: { label: '실패', color: '#b91c1c', bg: '#fee2e2' },
};

function formatDateTime(s) {
  if (!s) return '';
  return s.replace('T', ' ').slice(0, 19);
}

function formatAmount(n) {
  if (n == null) return '';
  return Number(n).toLocaleString() + '원';
}

export default function KakaoPayHistory() {
  const [list, setList] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(`${API}/my`, { credentials: 'include' })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then(setList)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div className="card">
      <div className="toolbar">
        <h2>🧾 내 결제 내역</h2>
        <Link to="/pay" className="nav-link">결제 페이지</Link>
      </div>

      {error && <div className="error">에러: {error}</div>}

      {list && list.length === 0 && (
        <div className="book-empty">
          <div className="book-empty-icon">🧾</div>
          <p>아직 결제 내역이 없어요. 결제 페이지에서 결제를 진행해보세요.</p>
        </div>
      )}

      {list && list.length > 0 && (
        <div className="user-table-wrap">
          <table className="user-table">
            <thead>
              <tr>
                <th>주문번호</th>
                <th>상품명</th>
                <th style={{ textAlign: 'right' }}>수량</th>
                <th style={{ textAlign: 'right' }}>금액</th>
                <th>상태</th>
                <th>결제수단</th>
                <th>주문 시각</th>
                <th>승인 시각</th>
              </tr>
            </thead>
            <tbody>
              {list.map((p) => {
                const meta = STATUS_META[p.status] || { label: p.status, color: '#1e1b4b', bg: '#e2e8f0' };
                return (
                  <tr key={p.id}>
                    <td><code style={{ fontSize: 12 }}>{p.partnerOrderId}</code></td>
                    <td>{p.itemName}</td>
                    <td style={{ textAlign: 'right' }}>{p.quantity}</td>
                    <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatAmount(p.totalAmount)}</td>
                    <td>
                      <span className="user-badge" style={{ background: meta.bg, color: meta.color, border: `1px solid ${meta.color}33` }}>
                        {meta.label}
                      </span>
                    </td>
                    <td>{p.paymentMethodType || '—'}</td>
                    <td className="user-date">{formatDateTime(p.createdAt)}</td>
                    <td className="user-date">{p.approvedAt ? formatDateTime(p.approvedAt) : '—'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}