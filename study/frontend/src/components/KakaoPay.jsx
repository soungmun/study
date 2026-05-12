import { useState } from 'react';
import { Link } from 'react-router-dom';

const API = 'http://localhost:8080/api/payments/kakao';

const PRESETS = [
  { label: '☕ 커피 한 잔', itemName: '응원 커피', quantity: 1, totalAmount: 4500 },
  { label: '🍱 점심 한 끼', itemName: '점심 한 끼', quantity: 1, totalAmount: 9000 },
  { label: '🎁 후원 (100원 테스트)', itemName: '테스트 후원', quantity: 1, totalAmount: 100 },
];

export default function KakaoPay() {
  const [form, setForm] = useState({
    itemName: '테스트 후원',
    quantity: 1,
    totalAmount: 100,
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const applyPreset = (preset) => {
    setForm({
      itemName: preset.itemName,
      quantity: preset.quantity,
      totalAmount: preset.totalAmount,
    });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!form.itemName.trim()) {
      setError('상품명을 입력해 주세요.');
      return;
    }
    const qty = Number(form.quantity);
    const amt = Number(form.totalAmount);
    if (!Number.isFinite(qty) || qty < 1) {
      setError('수량은 1 이상이어야 합니다.');
      return;
    }
    if (!Number.isFinite(amt) || amt < 1) {
      setError('금액은 1원 이상이어야 합니다.');
      return;
    }

    setSubmitting(true);
    try {
      const r = await fetch(`${API}/ready`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          itemName: form.itemName.trim(),
          quantity: qty,
          totalAmount: amt,
        }),
      });
      if (!r.ok) {
        const text = await r.text();
        throw new Error(text || `HTTP ${r.status}`);
      }
      const data = await r.json();
      const next =
        data.nextRedirectPcUrl ||
        data.nextRedirectMobileUrl ||
        data.nextRedirectAppUrl;
      if (!next) throw new Error('리다이렉트 URL이 없습니다.');
      window.location.href = next;
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="toolbar">
        <h2>💳 카카오페이 결제</h2>
        <Link to="/pay/history" className="nav-link">🧾 결제 내역 보기</Link>
      </div>

      <div className="kakao-pay-presets">
        {PRESETS.map((p) => (
          <button
            key={p.label}
            type="button"
            className="kakao-pay-preset"
            onClick={() => applyPreset(p)}
          >
            <span className="kakao-pay-preset-label">{p.label}</span>
            <span className="kakao-pay-preset-amount">
              {p.totalAmount.toLocaleString()}원
            </span>
          </button>
        ))}
      </div>

      <form className="form kakao-pay-form" onSubmit={onSubmit}>
        <label>
          상품명
          <input
            type="text"
            value={form.itemName}
            onChange={(e) => setForm({ ...form, itemName: e.target.value })}
            placeholder="예: 응원 후원"
            maxLength={200}
          />
        </label>
        <label>
          수량
          <input
            type="number"
            min={1}
            value={form.quantity}
            onChange={(e) => setForm({ ...form, quantity: e.target.value })}
          />
        </label>
        <label>
          총 결제 금액 (원)
          <input
            type="number"
            min={1}
            value={form.totalAmount}
            onChange={(e) => setForm({ ...form, totalAmount: e.target.value })}
          />
        </label>

        {error && <div className="error">에러: {error}</div>}

        <div className="actions">
          <button type="submit" className="primary" disabled={submitting}>
            {submitting ? '카카오페이로 이동 중…' : '카카오페이로 결제하기'}
          </button>
        </div>
      </form>

      <p className="muted" style={{ marginTop: 14, fontSize: 12 }}>
        ※ 테스트 CID(TC0ONETIME)로 동작합니다. 실제 청구는 발생하지 않으며,
        카카오페이 개발자 콘솔에서 발급한 테스트 Secret Key가 서버에 설정되어 있어야 합니다.
      </p>
    </div>
  );
}
