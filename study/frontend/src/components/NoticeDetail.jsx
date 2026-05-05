function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  return d.toLocaleString();
}

export default function NoticeDetail({ notice, onBack, onEdit, onDelete }) {
  return (
    <div className="card">
      <div className="detail-header">
        <h2>{notice.title}</h2>
      </div>
      <div className="meta">
        <span className="chip author">작성자 · {notice.author}</span>
        <span className="chip date">{formatDate(notice.createdAt)}</span>
        <span className="chip views">조회 {notice.viewCount ?? 0}</span>
      </div>
      <pre className="content">{notice.content}</pre>
      <div className="actions">
        <button type="button" className="ghost" onClick={onBack}>목록</button>
        <button type="button" className="success" onClick={onEdit}>수정</button>
        <button type="button" onClick={onDelete} className="danger">삭제</button>
      </div>
    </div>
  );
}