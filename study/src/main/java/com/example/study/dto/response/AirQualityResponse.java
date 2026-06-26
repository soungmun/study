package com.example.study.dto.response;

public record AirQualityResponse(
        double latitude,
        double longitude,
        Double pm10,
        Double pm25,
        Integer europeanAqi,
        String pm10Grade,
        String pm25Grade,
        String pm10Color,
        String pm25Color,
        String time
) {
    // API 호출 실패 시 사용할 빈 응답 생성 메서드
    public static AirQualityResponse empty(double lat, double lng) {
        return new AirQualityResponse(
                lat,
                lng,
                null,
                null,
                null,
                "정보없음",
                "정보없음",
                "#94a3b8", // 회색
                "#94a3b8", // 회색
                null
        );
    }
}
