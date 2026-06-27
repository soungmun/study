import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../utils/api'; // api 유틸리티 임포트
import { useAuth } from '../context/AuthContext'; // useAuth 훅 임포트

const BASE_URL = '/notices'; // api 유틸리티 사용 시 /api/ 접두사 제거

// readError 함수는 api 유틸리티에서 에러를 던지므로 더 이상 필요 없을 수 있지만,
// fetch 직접 사용 시를 대비해 남겨두거나 api 유틸리티의 에러 처리 방식에 맞게 조정
async function readError(res) {
  const text = await res.text();
  try { return JSON.parse(text).message || text; } catch { return text || `HTTP ${res.status}`; }
}

export default function NoticeCreate() {
  const navigate = useNavigate();
  const { user } = useAuth(); // 현재 로그인한 사용자 정보 가져오기
  const [form, setForm] = useState({ title: '', content: '', author: user.username || user.nickname || '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null); // 에러 상태 추가

  // 업로드된 이미지 목록: [{ imageId, url, originalName, fileSize }]
  const [images, setImages] = useState([]);
  const [uploading, setUploading] = useState(false);

  // user.nickname이 변경될 때마다 폼의 author를 업데이트하는 로직은 필요 없음.
  // 백엔드에서 currentUserId로 닉네임을 가져오므로.

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  // 파일 선택 시 바로 서버에 업로드
  const handleFileChange = async (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;
    e.target.value = ''; // 같은 파일 재선택 허용

    setUploading(true);
    setError(null); // 새 업로드 시 에러 초기화

    for (const file of files) {
      const fd = new FormData();
      fd.append('file', file);
      try {
        // api 유틸리티 사용
        const data = await api.post(`${BASE_URL}/images`, fd, {
          headers: { 'Content-Type': undefined }, // FormData 사용 시 Content-Type은 브라우저가 자동으로 설정
        });
        setImages((prev) => [...prev, data]);
      } catch (err) {
        setError(`업로드 실패 (${file.name}): ${err.message}`);
        // alert(`업로드 실패 (${file.name}): ${err.message}`); // alert 대신 에러 상태 사용
      }
    }
    setUploading(false); // 모든 파일 처리 후 업로드 상태 해제
  };

  const removeImage = (imageId) => {
    setImages((prev) => prev.filter((img) => img.imageId !== imageId));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null); // 새 제출 시 에러 초기화

    try {
      // api 유틸리티 사용
      await api.post(BASE_URL, {
        title: form.title,
        content: form.content,
        author: form.author, // 작성자 필드 추가
        imageIds: images.map((img) => img.imageId),
      });
      alert('등록되었습니다.');
      navigate('/');
    } catch (err) {
      setError(err.message); // alert 대신 에러 상태 사용
      // alert(err.message);
    } finally {
      setSubmitting(false); // 제출 완료 후 상태 해제
    }
  };

  // 로그인하지 않은 경우 (user가 null) 처리
  if (!user) {
    return (
      <div className="card">
        <div className="book-empty">
          <div className="spinner" />
          <p>로그인 정보가 없습니다. 로그인 후 다시 시도해주세요.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="toolbar">
        <h2>새 글 작성</h2>
      </div>
      <form onSubmit={handleSubmit} className="form">
        <label>
          작성자
          {/* 작성자 입력 필드 대신 닉네임 표시 */}
          <input name="author" value={form.author} readOnly className="readonly-input" />
        </label>
        <label>
          제목
          <input name="title" value={form.title} onChange={onChange} placeholder="제목을 입력하세요" />
        </label>
        <label>
          내용
          <textarea name="content" value={form.content} onChange={onChange} rows={10} placeholder="내용을 입력하세요" />
        </label>

        {/* 이미지 업로드 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span style={{ fontWeight: 600, fontSize: 14, color: '#374151' }}>이미지 첨부</span>
          <label
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              padding: '8px 16px',
              border: '1px dashed #94a3b8',
              borderRadius: 8,
              cursor: uploading ? 'not-allowed' : 'pointer',
              color: '#475569',
              fontSize: 14,
              width: 'fit-content',
              background: uploading ? '#f1f5f9' : '#fff',
            }}
          >
            <span>📎</span>
            <span>{uploading ? '업로드 중...' : '이미지 선택 (JPEG, PNG, GIF, WEBP · 최대 10MB)'}</span>
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              multiple
              onChange={handleFileChange}
              disabled={uploading || submitting}
              style={{ display: 'none' }}
            />
          </label>

          {/* 업로드된 이미지 미리보기 */}
          {images.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 4 }}>
              {images.map((img) => (
                <div
                  key={img.imageId}
                  style={{
                    position: 'relative',
                    width: 120,
                    borderRadius: 8,
                    overflow: 'hidden',
                    border: '1px solid #e2e8f0',
                    background: '#f8fafc',
                  }}
                >
                  <img
                    src={img.url}
                    alt={`첨부 이미지 ${i + 1}`}
                    style={{ width: '100%', height: 90, objectFit: 'cover', display: 'block' }}
                  />
                  <div style={{ padding: '4px 6px', fontSize: 11, color: '#64748b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {img.originalName}
                  </div>
                  <button
                    type="button"
                    onClick={() => removeImage(img.imageId)}
                    style={{
                      position: 'absolute', top: 4, right: 4,
                      width: 22, height: 22,
                      borderRadius: '50%',
                      background: 'rgba(0,0,0,0.55)',
                      color: '#fff',
                      border: 'none',
                      cursor: 'pointer',
                      fontSize: 13,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      lineHeight: 1,
                    }}
                    title="제거"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {error && <div className="error" style={{ marginTop: 12 }}>⚠️ {error}</div>} {/* 에러 메시지 표시 */}

        <div className="actions">
          <button type="button" className="ghost" onClick={() => navigate(-1)} disabled={submitting || uploading}>취소</button>
          <button type="submit" className="primary" disabled={submitting || uploading}>등록</button>
        </div>
      </form>
    </div>
  );
}
