import { useState } from 'react';

export default function NoticeForm({ initial, onSubmit, onCancel }) {
  const [author, setAuthor] = useState(initial?.author ?? '');
  const [title, setTitle] = useState(initial?.title ?? '');
  const [content, setContent] = useState(initial?.content ?? '');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!author.trim() || !title.trim() || !content.trim()) {
      alert('작성자, 제목, 내용을 모두 입력하세요.');
      return;
    }
    try {
      setSubmitting(true);
      await onSubmit({ author, title, content });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="toolbar">
        <h2>{initial ? '글 수정' : '새 글 작성'}</h2>
      </div>
      <form onSubmit={handleSubmit} className="form">
        <label>
          작성자
          <input value={author} onChange={(e) => setAuthor(e.target.value)} maxLength={100} placeholder="이름을 입력하세요" />
        </label>
        <label>
          제목
          <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} placeholder="제목을 입력하세요" />
        </label>
        <label>
          내용
          <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={10} placeholder="내용을 입력하세요" />
        </label>
        <div className="actions">
          <button type="button" className="ghost" onClick={onCancel} disabled={submitting}>취소</button>
          <button type="submit" className="primary" disabled={submitting}>{initial ? '수정' : '등록'}</button>
        </div>
      </form>
    </div>
  );
}