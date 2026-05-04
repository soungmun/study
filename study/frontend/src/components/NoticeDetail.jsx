function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  return d.toLocaleString();
}

export default function NoticeDetail({ notice, onBack, onEdit, onDelete }) {
  return (
    <div className="card">
      <div className="toolbar">
        <h2>{notice.title}</h2>
      </div>
      <div className="meta">
        <span>작성자 {notice.author}</span>
        <span>{formatDate(notice.createdAt)}</span>
      </div>
      <pre className="content">{notice.content}</pre>
      <div className="actions">
        <button type="button" onClick={onBack}>목록</button>
        <button type="button" onClick={onEdit}>수정</button>
        <button type="button" onClick={onDelete} className="danger">삭제</button>
      </div>
    </div>
  );
}