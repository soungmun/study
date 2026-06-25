const BASE_URL = 'http://localhost:8080/api';

/**
 * 실무형 API 클라이언트 유틸리티
 * - BASE_URL 설정 및 공통 헤더 자동 처리
 * - credentials: 'include' 기본 적용으로 세션/쿠키 연동 자동화
 * - 에러 처리를 일관되게 규격화하여 컴포넌트 레벨에서의 가독성 극대화
 */
async function request(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;
  
  const headers = {
    ...options.headers,
  };

  // FormData 전송 시에는 브라우저가 자동으로 boundary를 설정하도록 Content-Type을 지정하지 않음
  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const config = {
    ...options,
    headers,
    credentials: 'include', // 세션 쿠키 전송 필수
  };

  try {
    const response = await fetch(url, config);
    
    // JSON 응답 파싱 시도
    let data = null;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json().catch(() => null);
    } else {
      data = await response.text().catch(() => null);
    }

    if (!response.ok) {
      // 서버에서 전달한 에러 메시지 추출
      const errorMessage = (data && typeof data === 'object' && data.message) 
        ? data.message 
        : (typeof data === 'string' && data ? data : `API Error (${response.status})`);
      
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (error) {
    console.error(`[API Error] ${options.method || 'GET'} ${url} -`, error);
    throw error;
  }
}

export const api = {
  get: (endpoint, options = {}) => request(endpoint, { ...options, method: 'GET' }),
  post: (endpoint, body, options = {}) => 
    request(endpoint, { 
      ...options, 
      method: 'POST', 
      body: body instanceof FormData ? body : JSON.stringify(body) 
    }),
  put: (endpoint, body, options = {}) => 
    request(endpoint, { 
      ...options, 
      method: 'PUT', 
      body: body instanceof FormData ? body : JSON.stringify(body) 
    }),
  delete: (endpoint, options = {}) => request(endpoint, { ...options, method: 'DELETE' }),
};
