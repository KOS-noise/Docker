# 1단계: 빌드 스테이지
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 그래들 설정 파일 복사
COPY build.gradle settings.gradle ./
# 의존성 먼저 다운로드 (캐시 활용)
RUN gradle build -x test --no-daemon || return 0

# 전체 소스 복사 및 빌드
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 실행 포트
EXPOSE 8080

# 환경 변수 기본값 (Docker Compose에서 덮어쓰기 가능)
ENV SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb
ENV SPRING_DATASOURCE_USERNAME=myuser
ENV SPRING_DATASOURCE_PASSWORD=mypass

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
