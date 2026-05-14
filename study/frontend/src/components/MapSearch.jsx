import { useEffect, useRef, useState } from 'react';

const BASE_URL = 'http://localhost:8080/api/places/search';
const NEARBY_URL = 'http://localhost:8080/api/places/nearby';
const WEATHER_URL = 'http://localhost:8080/api/weather';
const AIR_URL = 'http://localhost:8080/api/air';
const PAGE_SIZE = 15;
const DEFAULT_CENTER = { lat: 37.4892775, lng: 126.7246513 }; // 부평역

function loadKakao() {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps?.Map) {
      resolve(window.kakao);
      return;
    }
    if (!window.kakao?.maps?.load) {
      reject(new Error('Kakao SDK가 로드되지 않았습니다.'));
      return;
    }
    window.kakao.maps.load(() => resolve(window.kakao));
  });
}

const CUSTOM_ID = '__custom__';

const POI_CATEGORIES = [
  'FD6', 'CE7', 'CS2', 'BK9', 'HP8', 'PM9',
  'CT1', 'AT4', 'SW8', 'AD5', 'MT1', 'PO3', 'SC4', 'OL7',
];

const RESTAURANT_RADIUS_M = 800;
const RESTAURANT_MAX = 15;
const RESTAURANT_MARKER_IMAGE =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">' +
      '<path d="M16 0C7.16 0 0 7.16 0 16c0 11 16 24 16 24s16-13 16-24C32 7.16 24.84 0 16 0z" fill="#ef4444" stroke="#7f1d1d" stroke-width="1.5"/>' +
      '<text x="16" y="22" text-anchor="middle" font-size="16" font-family="Apple Color Emoji,Segoe UI Emoji">🍽️</text>' +
    '</svg>'
  );

export default function MapSearch() {
  const mapRef = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef([]);
  const restaurantMarkersRef = useRef([]);
  const infoWindowRef = useRef(null);
  const customMarkerRef = useRef(null);
  const geocoderRef = useRef(null);
  const placesRef = useRef(null);

  const [kakaoReady, setKakaoReady] = useState(false);
  const [kakaoError, setKakaoError] = useState(null);
  const [query, setQuery] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [page, setPage] = useState(1);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [customPlace, setCustomPlace] = useState(null);
  const [weather, setWeather] = useState(null);
  const [weatherLoading, setWeatherLoading] = useState(false);
  const [weatherError, setWeatherError] = useState(null);
  const [air, setAir] = useState(null);
  const [airLoading, setAirLoading] = useState(false);
  const [airError, setAirError] = useState(null);
  const [fallbackAddress, setFallbackAddress] = useState('');
  const [restaurants, setRestaurants] = useState([]);
  const [restaurantsLoading, setRestaurantsLoading] = useState(false);

  const findNearestPlace = (lat, lng) =>
    new Promise((resolve) => {
      const kakao = window.kakao;
      if (!kakao?.maps?.services?.Places) {
        resolve(null);
        return;
      }
      if (!placesRef.current) {
        placesRef.current = new kakao.maps.services.Places();
      }
      const places = placesRef.current;
      const opts = {
        location: new kakao.maps.LatLng(lat, lng),
        radius: 300,
        sort: kakao.maps.services.SortBy.DISTANCE,
      };
      let pending = POI_CATEGORIES.length;
      let best = null;
      POI_CATEGORIES.forEach((code) => {
        places.categorySearch(
          code,
          (data, status) => {
            if (status === kakao.maps.services.Status.OK && data?.[0]) {
              const cand = data[0];
              const candDist = Number(cand.distance ?? Infinity);
              const bestDist = best ? Number(best.distance ?? Infinity) : Infinity;
              if (candDist < bestDist) best = cand;
            }
            if (--pending === 0) resolve(best);
          },
          opts
        );
      });
    });

  const reverseGeocode = (lat, lng, fallbackName) => {
    const kakao = window.kakao;
    if (!geocoderRef.current && kakao?.maps?.services) {
      geocoderRef.current = new kakao.maps.services.Geocoder();
    }

    const coord2AddressPromise = new Promise((resolve) => {
      const geocoder = geocoderRef.current;
      if (!geocoder) {
        resolve(null);
        return;
      }
      geocoder.coord2Address(lng, lat, (res, status) => {
        const ok = status === kakao.maps.services.Status.OK && res?.[0];
        resolve(ok ? res[0] : null);
      });
    });

    const coord2RegionPromise = new Promise((resolve) => {
      const geocoder = geocoderRef.current;
      if (!geocoder) {
        resolve(null);
        return;
      }
      geocoder.coord2RegionCode(lng, lat, (res, status) => {
        const ok = status === kakao.maps.services.Status.OK && Array.isArray(res) && res.length > 0;
        resolve(ok ? res : null);
      });
    });

    Promise.all([findNearestPlace(lat, lng), coord2AddressPromise, coord2RegionPromise]).then(([poi, addr, regions]) => {
      const buildingName = addr?.road_address?.building_name;
      const roadAddr = addr?.road_address?.address_name || '';
      const jibunAddr = addr?.address?.address_name || '';
      const regionAddr =
        regions?.find((r) => r.region_type === 'B')?.address_name ||
        regions?.find((r) => r.region_type === 'H')?.address_name ||
        regions?.[0]?.address_name ||
        '';

      if (poi) {
        setCustomPlace({
          id: CUSTOM_ID,
          place_name: poi.place_name || poi.road_address_name || poi.address_name || roadAddr || jibunAddr || regionAddr || '',
          category_name: poi.category_name || '',
          category_group_name: poi.category_group_name || '근처 장소',
          road_address_name: poi.road_address_name || roadAddr || regionAddr,
          address_name: poi.address_name || jibunAddr || regionAddr,
          place_url: poi.place_url,
          x: String(lng),
          y: String(lat),
        });
        return;
      }

      const coordsLabel = `위도 ${Number(lat).toFixed(5)}, 경도 ${Number(lng).toFixed(5)}`;
      const placeName = buildingName || roadAddr || jibunAddr || regionAddr || fallbackName || coordsLabel;
      setCustomPlace({
        id: CUSTOM_ID,
        place_name: placeName,
        category_name: '',
        category_group_name: buildingName ? '건물' : '',
        road_address_name: roadAddr || regionAddr,
        address_name: jibunAddr || regionAddr,
        x: String(lng),
        y: String(lat),
      });
    });
  };

  const placeCustomMarker = (lat, lng, fallbackName) => {
    const kakao = window.kakao;
    const map = mapInstance.current;
    if (!kakao || !map) return;
    infoWindowRef.current?.close();
    const pos = new kakao.maps.LatLng(lat, lng);
    if (customMarkerRef.current) {
      customMarkerRef.current.setMap(null);
    }
    const marker = new kakao.maps.Marker({
      position: pos,
      map,
      zIndex: 10,
    });
    customMarkerRef.current = marker;
    setSelectedId(CUSTOM_ID);
    map.panTo(pos);
    reverseGeocode(lat, lng, fallbackName);
  };

  useEffect(() => {
    let cancelled = false;
    loadKakao()
      .then((kakao) => {
        if (cancelled) return;
        const map = new kakao.maps.Map(mapRef.current, {
          center: new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng),
          level: 5,
        });
        mapInstance.current = map;
        infoWindowRef.current = new kakao.maps.InfoWindow({ removable: true });
        if (kakao.maps.services) {
          geocoderRef.current = new kakao.maps.services.Geocoder();
        }
        kakao.maps.event.addListener(map, 'click', (mouseEvent) => {
          const latlng = mouseEvent.latLng;
          placeCustomMarker(latlng.getLat(), latlng.getLng());
        });
        setKakaoReady(true);
        placeCustomMarker(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng, '부평역');
      })
      .catch((e) => setKakaoError(e.message));
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!submitted.trim()) {
      setResult(null);
      return;
    }
    const controller = new AbortController();
    setError(null);
    const params = new URLSearchParams({
      query: submitted.trim(),
      page: String(page),
      size: String(PAGE_SIZE),
    });
    fetch(`${BASE_URL}?${params.toString()}`, { signal: controller.signal })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setResult(data);
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setError(e.message);
      });
    return () => controller.abort();
  }, [submitted, page]);

  useEffect(() => {
    if (!kakaoReady || !mapInstance.current) return;
    const kakao = window.kakao;
    const map = mapInstance.current;

    markersRef.current.forEach((m) => m.setMap(null));
    markersRef.current = [];
    infoWindowRef.current?.close();

    const docs = result?.documents ?? [];
    if (docs.length === 0) return;

    if (customMarkerRef.current) {
      customMarkerRef.current.setMap(null);
      customMarkerRef.current = null;
    }
    setCustomPlace(null);

    const bounds = new kakao.maps.LatLngBounds();
    docs.forEach((p) => {
      const pos = new kakao.maps.LatLng(parseFloat(p.y), parseFloat(p.x));
      const marker = new kakao.maps.Marker({ position: pos, map });
      kakao.maps.event.addListener(marker, 'click', () => {
        focusPlace(p);
      });
      markersRef.current.push(marker);
      bounds.extend(pos);
    });
    map.setBounds(bounds);
    setSelectedId(docs[0].id);
  }, [result, kakaoReady]);

  const selectedPlace =
    selectedId === CUSTOM_ID
      ? customPlace
      : (result?.documents ?? []).find((d) => d.id === selectedId) ?? null;

  useEffect(() => {
    if (!selectedPlace) {
      setWeather(null);
      setWeatherError(null);
      setWeatherLoading(false);
      setAir(null);
      setAirError(null);
      setAirLoading(false);
      return;
    }
    const lat = parseFloat(selectedPlace.y);
    const lng = parseFloat(selectedPlace.x);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

    const controller = new AbortController();
    setWeatherLoading(true);
    setWeatherError(null);
    setAirLoading(true);
    setAirError(null);

    fetch(`${WEATHER_URL}?lat=${lat}&lng=${lng}`, { signal: controller.signal })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setWeather(data);
        setWeatherLoading(false);
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setWeatherError(e.message);
        setWeatherLoading(false);
      });

    fetch(`${AIR_URL}?lat=${lat}&lng=${lng}`, { signal: controller.signal })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setAir(data);
        setAirLoading(false);
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setAirError(e.message);
        setAirLoading(false);
      });

    return () => controller.abort();
  }, [selectedPlace?.id, selectedPlace?.x, selectedPlace?.y]);

  useEffect(() => {
    restaurantMarkersRef.current.forEach((m) => m.setMap(null));
    restaurantMarkersRef.current = [];
    setRestaurants([]);
    setRestaurantsLoading(false);

    if (!kakaoReady || !selectedPlace) return;
    const kakao = window.kakao;
    const map = mapInstance.current;
    if (!kakao || !map) return;

    const lat = parseFloat(selectedPlace.y);
    const lng = parseFloat(selectedPlace.x);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

    setRestaurantsLoading(true);
    const controller = new AbortController();

    fetch(`${NEARBY_URL}?lat=${lat}&lng=${lng}&category=FD6&radius=${RESTAURANT_RADIUS_M}&size=${RESTAURANT_MAX}`, {
      signal: controller.signal,
    })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setRestaurantsLoading(false);
        const docs = Array.isArray(data?.documents) ? data.documents : [];
        const list = docs.filter((d) => d.id !== selectedPlace.id);
        setRestaurants(list);

        const markerImage = new kakao.maps.MarkerImage(
          RESTAURANT_MARKER_IMAGE,
          new kakao.maps.Size(32, 40),
          { offset: new kakao.maps.Point(16, 40) }
        );
        list.forEach((r) => {
          const pos = new kakao.maps.LatLng(parseFloat(r.y), parseFloat(r.x));
          const marker = new kakao.maps.Marker({
            position: pos,
            map,
            image: markerImage,
            zIndex: 5,
            title: r.placeName,
          });
          kakao.maps.event.addListener(marker, 'click', () => {
            infoWindowRef.current?.close();
            const lastCat = (r.categoryName || '').split('>').map((s) => s.trim()).filter(Boolean).pop() || '';
            const html = `
              <div style="padding:8px 10px;font-size:13px;line-height:1.5;min-width:180px;max-width:240px;">
                <div style="font-weight:700;color:#7f1d1d;margin-bottom:4px;">🍽️ ${r.placeName}</div>
                ${lastCat ? `<div style="color:#475569;font-size:12px;margin-bottom:4px;">${lastCat}</div>` : ''}
                ${r.roadAddressName ? `<div style="color:#475569;">${r.roadAddressName}</div>` : ''}
                ${r.phone ? `<div style="color:#64748b;font-size:12px;margin-top:4px;">📞 ${r.phone}</div>` : ''}
                ${r.placeUrl ? `<a href="${r.placeUrl}" target="_blank" rel="noopener" style="display:inline-block;margin-top:6px;color:#ef4444;text-decoration:none;font-weight:600;">카카오맵에서 보기 →</a>` : ''}
              </div>`;
            infoWindowRef.current?.setContent(html);
            infoWindowRef.current?.open(map, marker);
          });
          restaurantMarkersRef.current.push(marker);
        });
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setRestaurantsLoading(false);
      });

    return () => controller.abort();
  }, [kakaoReady, selectedPlace?.id, selectedPlace?.x, selectedPlace?.y]);

  useEffect(() => {
    setFallbackAddress('');
    if (!selectedPlace) return;
    const hasName =
      selectedPlace.place_name ||
      selectedPlace.road_address_name ||
      selectedPlace.address_name;
    if (hasName) return;
    const lat = parseFloat(selectedPlace.y);
    const lng = parseFloat(selectedPlace.x);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
    const kakao = window.kakao;
    if (!kakao?.maps?.services) return;
    if (!geocoderRef.current) {
      geocoderRef.current = new kakao.maps.services.Geocoder();
    }
    let cancelled = false;
    const geocoder = geocoderRef.current;
    geocoder.coord2Address(lng, lat, (res, status) => {
      if (cancelled) return;
      if (status === kakao.maps.services.Status.OK && res?.[0]) {
        const r = res[0];
        const addr = r.road_address?.address_name || r.address?.address_name || '';
        if (addr) {
          setFallbackAddress(addr);
          return;
        }
      }
      geocoder.coord2RegionCode(lng, lat, (rres, rstatus) => {
        if (cancelled) return;
        if (rstatus === kakao.maps.services.Status.OK && Array.isArray(rres)) {
          const region =
            rres.find((r) => r.region_type === 'B')?.address_name ||
            rres.find((r) => r.region_type === 'H')?.address_name ||
            rres[0]?.address_name ||
            '';
          if (region) setFallbackAddress(region);
        }
      });
    });
    return () => { cancelled = true; };
  }, [selectedPlace?.id, selectedPlace?.x, selectedPlace?.y, selectedPlace?.place_name, selectedPlace?.road_address_name, selectedPlace?.address_name]);

  const focusPlace = (place) => {
    if (!kakaoReady) return;
    const kakao = window.kakao;
    const map = mapInstance.current;
    const pos = new kakao.maps.LatLng(parseFloat(place.y), parseFloat(place.x));
    map.panTo(pos);
    setSelectedId(place.id);

    infoWindowRef.current?.close();

    const parts = [
      place.place_name ? `<div style="font-weight:700;color:#1e1b4b;margin-bottom:4px;">${place.place_name}</div>` : '',
      place.road_address_name ? `<div style="color:#475569;">${place.road_address_name}</div>` : '',
      place.address_name && place.address_name !== place.road_address_name ? `<div style="color:#94a3b8;font-size:12px;">${place.address_name}</div>` : '',
      place.place_url ? `<a href="${place.place_url}" target="_blank" rel="noopener" style="display:inline-block;margin-top:6px;color:#a855f7;text-decoration:none;font-weight:600;">카카오맵에서 보기 →</a>` : '',
    ].filter(Boolean);
    if (parts.length === 0) return;
    const html = `<div style="padding:8px 10px;font-size:13px;line-height:1.5;min-width:180px;max-width:240px;">${parts.join('')}</div>`;
    const tempMarker = markersRef.current.find((m) => {
      const p = m.getPosition();
      return p.getLat() === pos.getLat() && p.getLng() === pos.getLng();
    });
    if (tempMarker && infoWindowRef.current) {
      infoWindowRef.current.setContent(html);
      infoWindowRef.current.open(map, tempMarker);
    }
  };

  const onSearch = (e) => {
    e.preventDefault();
    setPage(1);
    setSubmitted(query);
    setSelectedId(null);
  };

  const onReset = () => {
    setQuery('');
    setSubmitted('');
    setResult(null);
    setError(null);
    setSelectedId(null);
    if (kakaoReady) {
      markersRef.current.forEach((m) => m.setMap(null));
      markersRef.current = [];
      restaurantMarkersRef.current.forEach((m) => m.setMap(null));
      restaurantMarkersRef.current = [];
      infoWindowRef.current?.close();
      const kakao = window.kakao;
      mapInstance.current.setCenter(new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng));
      mapInstance.current.setLevel(5);
      placeCustomMarker(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng, '부평역');
    }
  };

  return (
    <div className="card">
      <div className="toolbar">
        <h2>📍 장소 검색</h2>
        <span className="muted">카카오 지도 + 키워드 검색</span>
      </div>

      <form className="search-bar" onSubmit={onSearch}>
        <input
          type="text"
          className="search-input"
          placeholder="검색할 장소 (예: 카페, 강남역, 서울대)"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" className="primary" disabled={!query.trim()}>검색</button>
        {submitted && (
          <button type="button" className="ghost" onClick={onReset}>초기화</button>
        )}
      </form>

      {kakaoError && <div className="error">지도 로드 실패: {kakaoError}</div>}
      {error && <div className="error">에러: {error}</div>}

      <div className="map-layout">
        <div className="map-canvas-wrap">
          <div className="map-canvas" ref={mapRef} />
        </div>
        <PlaceDetail
          place={selectedPlace}
          weather={weather}
          weatherLoading={weatherLoading}
          weatherError={weatherError}
          air={air}
          airLoading={airLoading}
          airError={airError}
          fallbackAddress={fallbackAddress}
          restaurants={restaurants}
          restaurantsLoading={restaurantsLoading}
          onRestaurantClick={(r) => {
            const kakao = window.kakao;
            const map = mapInstance.current;
            if (!kakao || !map) return;
            const pos = new kakao.maps.LatLng(parseFloat(r.y), parseFloat(r.x));
            map.panTo(pos);
            const marker = restaurantMarkersRef.current.find((m) => {
              const p = m.getPosition();
              return p.getLat() === pos.getLat() && p.getLng() === pos.getLng();
            });
            if (marker) {
              kakao.maps.event.trigger(marker, 'click');
            }
          }}
        />
      </div>
    </div>
  );
}

function PlaceDetail({ place, weather, weatherLoading, weatherError, air, airLoading, airError, fallbackAddress, restaurants, restaurantsLoading, onRestaurantClick }) {
  if (!place) {
    return (
      <aside className="place-detail place-detail-empty">
        <div className="book-empty-icon">🗺️</div>
        <p>지도 마커나 목록을 클릭하면<br />상세 정보가 여기에 표시돼요</p>
      </aside>
    );
  }

  const lastCat = (place.category_name || '').split('>').map((s) => s.trim()).filter(Boolean).pop();
  const summary = place.category_group_name || lastCat || '';
  const displayName =
    place.place_name ||
    place.road_address_name ||
    place.address_name ||
    fallbackAddress ||
    '';

  return (
    <aside className="place-detail">
      <div className="place-detail-header">
        {displayName && <div className="place-detail-name">{displayName}</div>}
        {summary && <div className="place-detail-summary">{summary}</div>}
      </div>

      <div className="place-detail-body">
        <div className="place-detail-row">
          <span className="place-detail-label">🌡️ 온도</span>
          <span className="place-detail-value">
            {weatherLoading && '불러오는 중…'}
            {!weatherLoading && weatherError && '온도 정보를 가져오지 못했어요'}
            {!weatherLoading && !weatherError && weather && (
              <>
                {weather.icon || ''} {weather.temperature != null ? `${Math.round(weather.temperature)}°C` : '—'}
                {weather.description ? ` · ${weather.description}` : ''}
              </>
            )}
          </span>
        </div>

        <AirCard
          air={air}
          loading={airLoading}
          error={airError}
        />

        <RestaurantList
          items={restaurants}
          loading={restaurantsLoading}
          onItemClick={onRestaurantClick}
        />
      </div>
    </aside>
  );
}

function RestaurantList({ items, loading, onItemClick }) {
  return (
    <div className="restaurant-card">
      <div className="restaurant-card-title">🍽️ 주변 맛집 <span className="muted">(반경 800m)</span></div>
      {loading && (
        <div className="restaurant-card-status">
          <span className="spinner spinner-sm" />
          <span>주변 음식점을 찾는 중…</span>
        </div>
      )}
      {!loading && (!items || items.length === 0) && (
        <div className="restaurant-card-status muted">주변에 음식점이 없어요</div>
      )}
      {!loading && items && items.length > 0 && (
        <ul className="restaurant-list">
          {items.map((r) => {
            const cat = (r.categoryName || '').split('>').map((s) => s.trim()).filter(Boolean).pop() || '';
            const dist = Number(r.distance);
            const distLabel = Number.isFinite(dist)
              ? (dist >= 1000 ? `${(dist / 1000).toFixed(1)}km` : `${dist}m`)
              : '';
            return (
              <li key={r.id} className="restaurant-item" onClick={() => onItemClick?.(r)}>
                <div className="restaurant-item-main">
                  <div className="restaurant-item-name">{r.placeName}</div>
                  {cat && <div className="restaurant-item-cat">{cat}</div>}
                </div>
                {distLabel && <div className="restaurant-item-dist">{distLabel}</div>}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function AirCard({ air, loading, error }) {
  return (
    <div className="air-card">
      <div className="air-card-title">🌫️ 미세먼지</div>
      {loading && (
        <div className="air-card-status">
          <span className="spinner spinner-sm" />
          <span>대기질 정보를 불러오는 중…</span>
        </div>
      )}
      {!loading && error && (
        <div className="air-card-status" style={{ color: '#b91c1c' }}>
          ⚠️ 대기질 정보를 가져오지 못했어요
        </div>
      )}
      {!loading && !error && air && (
        <>
          <div className="air-card-grid">
            <div className="air-card-cell" style={{ background: air.pm10Color || '#94a3b8' }}>
              <span className="air-card-label">PM10 (미세먼지)</span>
              <span className="air-card-value">
                {air.pm10 != null ? `${Math.round(air.pm10)} ㎍/㎥` : '—'}
              </span>
              <span className="air-card-grade">{air.pm10Grade || '정보없음'}</span>
            </div>
            <div className="air-card-cell" style={{ background: air.pm25Color || '#94a3b8' }}>
              <span className="air-card-label">PM2.5 (초미세)</span>
              <span className="air-card-value">
                {air.pm25 != null ? `${Math.round(air.pm25)} ㎍/㎥` : '—'}
              </span>
              <span className="air-card-grade">{air.pm25Grade || '정보없음'}</span>
            </div>
          </div>
          {air.europeanAqi != null && (
            <div className="air-card-aqi">
              유럽 AQI 지수: <strong>{air.europeanAqi}</strong>
            </div>
          )}
        </>
      )}
    </div>
  );
}
