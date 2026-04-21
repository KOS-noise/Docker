# 세션 5 — Multi-stage Build (Spring Boot 최적화)

## 5-1. Multi-stage Build 개요

### 개념 (Concept)
- 하나의 Dockerfile 내에서 **여러 개의 `FROM` 구문**을 사용하여 빌드 단계를 분리하는 기법
- **빌드 단계(Build Stage)**: 소스 컴파일, 의존성 다운로드 등 빌드 세부 도구 포함
- **실행 단계(Runtime Stage)**: 최종 실행에 필요한 결과물(Artifact)만 복사하여 최소한의 환경으로 구성

### 필요성 (Necessity)
1. **이미지 크기 최소화**: 빌드 인프라(JDK, Gradle, Cache)를 최종 이미지에서 제외 (예: 800MB → 200MB)
2. **보안 강화**: 이미지 내 소스코드, 빌드 툴이 없어 공격 표면(Attack Surface) 최소화
3. **효율성**: 필요한 파일만 포함하므로 배포 속도가 빠르고 네트워크 비용 절감

### 언제 써야 하는가? (When to use)
- **프로덕션(운영) 이미지 제작 시**: 필수 권장
- **컴파일 기반 언어**: Java, Go, C++, Rust 등 빌드 결과물이 명확한 경우
- **CI/CD 환경**: 레포지토리 저장 공간 절약 및 빠른 이미지 전송이 필요한 경우

---

### 단순 빌드 vs Multi-stage 비교

#### 단순 빌드 (문제가 있는 예시)
Spring Boot 앱을 빌드하려면 JDK + Gradle이 필요하지만, 실행 시에는 JRE만 있으면 됩니다.

```dockerfile
# 문제가 있는 단순 Dockerfile
FROM gradle:8-jdk17
WORKDIR /app
COPY . .
RUN gradle bootJar
CMD ["java", "-jar", "build/libs/app.jar"]
```

결과: JDK + Gradle + 소스코드 + 빌드 캐시가 모두 포함 → **~800MB 이상**

#### Multi-stage 빌드
```
Stage 1 (Build)              Stage 2 (Runtime)
┌────────────────────┐       ┌────────────────────┐
│ gradle:8-jdk17     │       │ eclipse-temurin:   │
│ + 소스코드          │       │ 17-jre-alpine      │
│ + 빌드 결과 (JAR)  │──────►│ + app.jar만        │
│ (~800MB)           │ COPY  │ (~200MB)           │
└────────────────────┘       └────────────────────┘
       버림                        최종 이미지
```

---

## 5-2. Multi-stage Dockerfile 작성

```dockerfile
# Stage 1: Build
FROM gradle:8-jdk17 AS builder
WORKDIR /workspace
COPY . .
RUN gradle bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### 핵심 문법

| 문법 | 설명 |
|------|------|
| `AS builder` | 스테이지에 이름 부여 |
| `COPY --from=builder` | 다른 스테이지에서 파일 복사 |
| `--from=0` | 이름 대신 인덱스(0부터) 사용 가능 |

---

## 5-3. 실습용 Dockerfile.multi

> 이미 ZIP 파일에 포함되어 있습니다: `spring-demo/Dockerfile.multi`

```dockerfile
# ---- Stage 1: Build ----
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# 의존성 레이어 캐시 분리
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# 소스 빌드
COPY src ./src
RUN gradle bootJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# builder 스테이지에서 JAR만 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

---

## 5-4. 실습 — 이미지 크기 비교

### 세션 4의 단순 이미지 (app.jar 직접 COPY)

```bash
cd C:/docker-lab/spring-demo

# 세션 4에서 만든 이미지 (이미 있음)
docker images myapp
# myapp   1.0.0   ...   ~200MB (JRE + JAR)
```

### Multi-stage 이미지 빌드

> 실습에서는 소스 없이 JAR만 있으므로, Dockerfile.multi 내 Stage 2만 실행하는 방법으로 진행합니다.

```bash
# Dockerfile.multi로 빌드 (Stage 2: JAR → JRE-alpine)
docker build -f Dockerfile.multi -t myapp:multi .

# 이미지 크기 비교
docker images myapp
```

### 비교 시연용 — JDK 기반 이미지도 만들어보기

```bash
# 비교용: JDK 전체 포함 이미지
Dockerfile.jdk
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]


docker build -f Dockerfile.jdk -t myapp:jdk .

# 세 이미지 크기 비교
docker images myapp
# REPOSITORY  TAG     SIZE
# myapp       jdk     ~550MB  (JDK 전체)
# myapp       1.0.0   ~200MB  (JRE alpine)
# myapp       multi   ~200MB  (Multi-stage, JRE alpine)
```

---

## 5-5. 레이어 분석

```bash
# 각 이미지의 레이어 구성 확인
docker history myapp:1.0.0
docker history myapp:jdk
```

---

## 5-6. JVM 컨테이너 최적화 옵션

컨테이너 환경에서 JVM이 호스트 메모리 전체를 인식하는 문제가 있었습니다.
JDK 8u191+ / JDK 10+부터는 자동으로 컨테이너 리소스를 인식합니다.

```dockerfile
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-jar", "app.jar"]
```

| 옵션 | 설명 |
|------|------|
| `-XX:+UseContainerSupport` | 컨테이너 cgroup 리소스 인식 (JDK 10+ 기본 활성화) |
| `-XX:MaxRAMPercentage=75.0` | 컨테이너 메모리의 75%를 힙으로 사용 |
| `-XX:InitialRAMPercentage=50.0` | 초기 힙 크기 비율 |

---

## 5-7. 특정 스테이지만 빌드하기

```bash
# [방법 1] Dockerfile.multi 파일을 지정해서 빌드
docker build -f Dockerfile.multi --target builder -t myapp:build-only .

# 빌드 결과 확인
docker run --rm myapp:build-only ls /workspace/build/libs/
```

---

## 5-8. 실습 — 따라하기

### 실습 1 — JDK vs JRE 이미지 크기 비교

```bash
cd C:/docker-lab/spring-demo

# JDK 전체 포함 이미지 (나쁜 예)
Dockerfile.jdk 
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]

docker build -f Dockerfile.jdk -t myapp:jdk .

# JRE alpine 이미지 (좋은 예 — 세션 4에서 이미 빌드)
# myapp:1.0.0 이미 있음

# 크기 비교
docker images myapp
# REPOSITORY  TAG     SIZE
# myapp       jdk     ~550MB  ← JDK 전체
# myapp       1.0.0   ~200MB  ← JRE alpine
```

---

### 실습 2 — Dockerfile.multi 빌드 & 크기 확인

```bash
# Multi-stage 이미지 빌드
docker build -f Dockerfile.multi -t myapp:multi .

# 세 이미지 크기 한눈에 비교
docker images myapp
```

```bash
# 레이어 구성 차이 확인
docker history myapp:jdk    # JDK 레이어 포함
docker history myapp:multi  # JRE + JAR만
```

```bash
# multi 이미지 실행 확인
docker run -d -p 8080:8080 --name myapp-multi myapp:multi
docker logs -f myapp-multi
# 브라우저에서 http://localhost:8080 확인 후 Ctrl+C

docker stop myapp-multi && docker rm myapp-multi
```

---

### 실습 3 — 특정 스테이지만 빌드해서 내부 확인

Multi-stage 중 builder 스테이지만 빌드하고, 빌드 결과물을 직접 확인.

```bash
# builder 스테이지까지만 빌드
docker build -f Dockerfile.multi --target builder -t myapp:builder-only .

# builder 이미지 크기 확인 (훨씬 큼)
docker images myapp

# builder 내부에서 빌드 결과물 확인
docker run --rm myapp:builder-only ls /workspace/build/libs/
# *.jar 파일 목록 출력
```

→ Multi-stage는 이 결과물만 뽑아서 최종 이미지에 넣는 것.

---

## 5-9. 응용 실습 문제

---

### 문제 1 — 빌드 유연성 (Build Arguments로 버전 주입)

이미지를 빌드할 때마다 해당 이미지의 버전 정보를 내부에 남기고 싶습니다. `ARG`를 사용하여 빌드 시점에 버전 번호를 주입하고, 앱 내부에서 이를 확인할 수 있도록 환경 변수로 설정하세요.

**조건:**
1.  **Dockerfile.multi** 의 Stage 2 (Runtime) 최상단에 아래 내용을 추가하세요.
    ```dockerfile
    ARG APP_VERSION=1.0.0
    ENV VERSION=${APP_VERSION}
    ```
2.  **빌드 실행:** 빌드 인자를 `2.0.0-beta`와 같이 넘겨서 이미지를 생성하세요.
    ```bash
    docker build -f Dockerfile.multi --build-arg APP_VERSION=2.0.0-beta -t myapp:v2 .
    ```
3.  **확인:** 실행 중인 컨테이너 내부의 환경 변수를 조회하여 주입한 버전이 나오는지 확인하세요.
    - 명령어: `docker run --rm myapp:v2 env | grep VERSION`
    - 결과 예시: `VERSION=2.0.0-beta`

---

### 문제 2 — 서비스 모니터링 (Healthcheck 추가)

애플리케이션이 단순히 "떠 있는 것"과 "정상 응답 중인 것"은 다릅니다. Spring Boot Actuator와 Docker Healthcheck를 연동하세요.

**조건:**
1.  **build.gradle 수정:** `dependencies` 블록에 아래 내용을 추가하고 새로 빌드하세요.
    ```gradle
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    ```
2.  **Dockerfile.multi 수정:** `HEALTHCHECK` 구문을 추가하세요.
    ```dockerfile
    HEALTHCHECK --interval=30s --timeout=3s \
      CMD wget -qO- http://localhost:8080/actuator/health | grep UP || exit 1
    ```
3.  **확인:** 컨테이너 실행 후 약 30초~1분 뒤에 `docker ps`를 입력하여 `STATUS` 항목에 `(healthy)`가 뜨는지 확인하세요.

---

## 정리

- Multi-stage Build = 빌드 환경과 실행 환경 분리
- `COPY --from=스테이지명` 으로 필요한 결과물만 추출
- 빌드 도구(JDK, Gradle)가 최종 이미지에 포함되지 않음 → 크기·보안 개선
- `--target` 옵션으로 특정 스테이지까지만 빌드 가능 (디버깅 유용)