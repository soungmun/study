import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api } from '../utils/api';

const AuthContext = createContext(null);

/**
 * 실무형 전역 인증 Context Provider
 * - 기존의 개별 fetch 호출 및 비표준 Custom Event(auth-changed) 패턴을 완벽히 대체
 * - 단일 상태원칙(Single Source of Truth)을 준수하여 애플리케이션 전역의 로그인/관리자 상태 동기화
 * - 성능 최적화를 위한 useCallback 적용
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [loading, setLoading] = useState(true);

  // 인증 상태 동기화 함수
  const refresh = useCallback(async () => {
    try {
      // 1. 유저 정보 조회
      const userData = await api.get('/auth/me');
      setUser(userData);
      
      // 2. 관리자 정보 조회 (유저가 로그인되어 있을 때만 실행하여 불필요한 요청 방지)
      if (userData) {
        try {
          const adminData = await api.get('/admin/me');
          setIsAdmin(!!adminData?.admin);
        } catch {
          setIsAdmin(false);
        }
      } else {
        setIsAdmin(false);
      }
    } catch {
      setUser(null);
      setIsAdmin(false);
    } finally {
      setLoading(false);
    }
  }, []);

  // 컴포넌트 마운트 시 초기 인증 정보 조회
  useEffect(() => {
    refresh();
  }, [refresh]);

  // 로그인 처리 함수
  const login = useCallback(async (username, password) => {
    try {
      const userData = await api.post('/auth/login', { username, password });
      setUser(userData);
      
      // 관리자 권한 확인
      try {
        const adminData = await api.get('/admin/me');
        setIsAdmin(!!adminData?.admin);
      } catch {
        setIsAdmin(false);
      }
      return userData;
    } catch (error) {
      throw error;
    }
  }, []);

  // 로그아웃 처리 함수
  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      setIsAdmin(false);
    }
  }, []);

  const value = {
    user,
    isAdmin,
    loading,
    refresh,
    login,
    logout,
    setUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// 실무에서 사용되는 안전하고 직관적인 custom hook
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
