import { Link, useSearchParams } from 'react-router-dom';

export function KakaoPaySuccess() {
  const [params] = useSearchParams();
  const orderId = params.get('orderId') || '';
  const amount = params.get('amount') || '';
  const itemName = params.get('itemName') || '';

  return (
    <div className="card">
      <div className="toolbar">
        <h2>✅ 결제 완료</h2>
      </div>
      <div className="kakao-pay-result">
        <p>결제가 정상적으로 승인되었습니다.</p>
        <ul className="kakao-pay-result-list">
          {itemName && <li><strong>상품명</strong><span>{itemName}</span></li>}
          {amount && <li><strong>결제 금액</strong><span>{Number(amount).toLocaleString()}원</span></li>}
          {orderId && <li><strong>주문 번호</strong><span>{orderId}</span></li>}
        </ul>
        <div className="actions">
          <Link to="/pay" className="nav-link active">결제 페이지로</Link>
        </div>
      </div>
    </div>
  );
}

export function KakaoPayFail() {
  const [params] = useSearchParams();
  const reason = params.get('reason') || '알 수 없는 사유';

  return (
    <div className="card">
      <div className="toolbar">
        <h2>⚠️ 결제 실패/취소</h2>
      </div>
      <div className="kakao-pay-result">
        <p>결제가 완료되지 않았습니다.</p>
        <p className="muted">사유: {reason}</p>
        <div className="actions">
          <Link to="/pay" className="nav-link active">다시 시도</Link>
        </div>
      </div>
    </div>
  );
}