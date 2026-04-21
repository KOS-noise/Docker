# 세션 4 — Dockerfile 문법 & Layer 캐싱


---

## 4-1. Dockerfile이란?

이미지를 만드는 **레시피(설계도)** 파일입니다.
각 명령어 한 줄이 하나의 레이어가 됩니다.

```
Dockerfile → docker build → Image → docker run → Container
```

---

## 4-2. 핵심 지시어

아래는 Spring Boot 앱을 패키징하는 전형적인 Dockerfile 예시입니다.
각 지시어가 어떤 역할을 하는지 이어서 하나씩 살펴봅니다.

```dockerfile
FROM eclipse-temurin:17-jre-alpine          # 베이스 이미지 (Java 17 JRE)

WORKDIR /app                                 # 작업 디렉터리 설정

ARG JAR_FILE=build/libs/app.jar             # 빌드 시점 변수

COPY ${JAR_FILE} app.jar                    # JAR 파일 복사

ENV APP_ENV=production                       # 환경 변수

EXPOSE 8080                                  # 포트 선언 (문서화)

CMD ["java", "-jar", "app.jar"]             # 컨테이너 시작 명령
```

---

### FROM — 베이스 이미지 지정 (필수, 첫 번째)

```dockerfile
FROM eclipse-temurin:17-jre-alpine
```

- 모든 Dockerfile은 반드시 FROM으로 시작
- `scratch` : 완전히 빈 이미지 (Go 바이너리용)
- Alpine 계열: 경량, 보안 취약점 적음 → 프로덕션 권장

#### 언어/환경별 베이스 이미지 선택

| 언어/환경 | 빌드(컴파일) 스테이지 | 실행 스테이지 |
|-----------|----------------------|--------------|
| Java (Spring Boot) | `eclipse-temurin:17-jdk-alpine` | `eclipse-temurin:17-jre-alpine` |
| Node.js | `node:20-alpine` | `node:20-alpine` 또는 `node:20-slim` |
| Go | `golang:1.22-alpine` | `scratch` (바이너리만) |
| Python | `python:3.12-slim` | `python:3.12-slim` |
| .NET | `mcr.microsoft.com/dotnet/sdk:8.0` | `mcr.microsoft.com/dotnet/aspnet:8.0` |

> Java는 JDK(빌드) vs JRE(실행)로 분리 가능 → Multi-stage Build로 이미지 크기 절감 (세션 5 참고)
> Go는 컴파일 결과가 단일 바이너리이므로 실행 스테이지에 `scratch` 사용 가능

### WORKDIR — 작업 디렉터리 설정

```dockerfile
WORKDIR /app
```

- 이후 명령어의 기준 경로
- 없으면 자동 생성
- `cd` 대신 WORKDIR 사용 권장

### COPY — 파일 복사 (호스트 → 이미지)

빌드할 때 **내 PC에 있는 파일을 이미지 안으로 집어넣는** 명령어입니다.

```dockerfile
COPY target/*.jar app.jar   # 내 PC의 target/*.jar → 이미지의 /app/app.jar
COPY src/ /app/src/         # 내 PC의 src 폴더 전체 → 이미지의 /app/src/
```

```
[내 PC]                 →  COPY  →  [이미지 내부]
target/myapp.jar                     /app/app.jar
src/                                 /app/src/
```

### RUN — 이미지 빌드 시 실행할 명령어

`docker build` 를 실행하는 시점에 **이미지 안에서 명령어를 실행**합니다.
주로 패키지 설치, 사용자 생성 등 이미지를 준비하는 작업에 사용합니다.

```dockerfile
RUN apt-get update && apt-get install -y curl   # curl 패키지를 이미지에 설치
RUN addgroup -S app && adduser -S app -G app    # 보안용 non-root 사용자 생성
```

- RUN 한 줄 = 이미지 레이어 1개 생성
- `&&` 로 연결하면 레이어 1개로 합쳐짐 → 이미지 크기 줄어듦

```dockerfile
# 레이어 2개 (비효율)
RUN apt-get update
RUN apt-get install -y curl

# 레이어 1개 (권장)
RUN apt-get update && apt-get install -y curl
```

### CMD — 컨테이너 시작 시 기본 실행 명령

`docker run` 으로 컨테이너를 시작할 때 **자동으로 실행할 명령**을 지정합니다.
`docker run` 뒤에 다른 명령을 붙이면 덮어쓸 수 있습니다.

```dockerfile
CMD ["java", "-jar", "app.jar"]
```

```bash
docker run myapp                     # → java -jar app.jar 실행
docker run myapp echo "hello"        # → CMD 무시, echo "hello" 실행
```

### ENTRYPOINT — 반드시 실행될 명령 (CMD와의 차이)

CMD와 비슷하지만 **항상 고정으로 실행**됩니다.
`docker run` 뒤에 인자를 붙이면 ENTRYPOINT의 인자로 전달됩니다.

```dockerfile
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
# → 최종 실행: java -jar app.jar
```

```bash
docker run myapp                          # → java -jar app.jar
docker run myapp -jar other.jar           # → java -jar other.jar (CMD만 교체)
docker run --entrypoint echo myapp hello  # → echo hello (ENTRYPOINT 교체 시 옵션 필요)
```

| 항목 | CMD | ENTRYPOINT |
|------|-----|------------|
| 역할 | 기본 명령 | 실행 파일 고정 |
| 재정의 | `docker run` 뒤 인자로 가능 | `--entrypoint` 옵션 필요 |
| 함께 사용 | ENTRYPOINT의 기본 인자 역할 | - |

### ENV — 환경 변수

```dockerfile
ENV JAVA_OPTS="-Xmx512m"
ENV APP_PORT=8080
```

### EXPOSE — 문서화용 포트 선언

```dockerfile
EXPOSE 8080
```

> 실제 포트 열기는 `docker run -p`로 해야 함. EXPOSE는 의도 선언.

### ARG — 빌드 시점 변수

```dockerfile
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
```

```bash
docker build --build-arg JAR_FILE=target/myapp.jar .
```

---

## 4-3. Layer 캐싱 원리

### 캐싱 동작 방식

빌드 시 각 명령어를 실행하기 전, 이전에 빌드한 레이어가 있으면 **재사용**합니다.
**어떤 레이어가 변경되면, 그 이하 모든 레이어를 재빌드**합니다.

```
Layer 1: FROM eclipse-temurin:17-jre-alpine   ← 변경 없음 → 캐시 사용
Layer 2: WORKDIR /app                          ← 변경 없음 → 캐시 사용
Layer 3: COPY . /app                           ← 소스 변경됨 → 캐시 무효화!
Layer 4: RUN ./gradlew build                   ← 반드시 재실행
Layer 5: CMD ["java", "-jar", "app.jar"]       ← 반드시 재실행
```

### 캐시 최적화 — 변경 빈도 낮은 것을 먼저

```dockerfile
# 나쁜 예: 소스 변경 시 의존성도 매번 재설치
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY . .                          # 소스가 바뀌면 아래도 전부 재실행
RUN ./gradlew dependencies
CMD ["java", "-jar", "app.jar"]

# 좋은 예: 의존성 레이어 캐시 분리
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build.gradle settings.gradle ./   # 의존성 파일만 먼저 복사
RUN ./gradlew dependencies --no-daemon # 의존성만 다운로드 (캐시)
COPY src/ ./src/                        # 소스 코드 복사 (자주 변경)
RUN ./gradlew bootJar --no-daemon
CMD ["java", "-jar", "build/libs/app.jar"]
```

---

## 4-4. Spring Boot용 기본 Dockerfile

> 실습에서는 미리 빌드된 `app.jar`를 ZIP에서 받았으므로 아래를 사용합니다.

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY app.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

---

## 4-5. 실습 — 따라하기

### 준비

```bash
cd C:/docker-lab/spring-demo
ls
# app.jar  Dockerfile  Dockerfile.multi  .dockerignore
```

---

### 실습 1 — 첫 이미지 빌드 & 레이어 확인

#### docker build 옵션 설명

```bash
docker build -t myapp:1.0.0 . --progress=plain
#             ↑             ↑  ↑
#            -t            .   --progress
```

| 옵션 | 예시 | 의미 |
|------|------|------|
| `-t` | `-t myapp:1.0.0` | 만들어질 이미지 이름과 태그 지정 (`이름:태그`) |
| `.` | `docker build .` | Dockerfile과 파일들의 위치 — 현재 디렉터리 기준 |
| `-f` | `-f Dockerfile.multi` | 사용할 Dockerfile 파일명 지정 (기본값: `Dockerfile`) |
| `--no-cache` | `--no-cache` | 캐시를 무시하고 전체 레이어를 처음부터 재빌드 |
| `--build-arg` | `--build-arg JAR_FILE=app.jar` | ARG로 선언한 변수에 값을 외부에서 주입 |
| `--target` | `--target builder` | Multi-stage 중 특정 스테이지까지만 빌드 (세션 5) |
| `--progress` | `--progress=plain` | 빌드 로그 출력 방식 (`plain`: 전체 출력, `auto`: 요약) |

> `.` 을 빠뜨리면 `docker build -t myapp:1.0.0` 만 입력한 것이므로 에러 발생합니다.

```bash
# 빌드 (각 레이어 과정 출력)
docker build -t myapp:1.0.0 . --progress=plain
```

출력에서 확인할 것:
```
#1 [internal] load build definition from Dockerfile
#2 [1/3] FROM eclipse-temurin:17-jre-alpine   ← Layer 1
#3 [2/3] WORKDIR /app                          ← Layer 2
#4 [3/3] COPY app.jar app.jar                  ← Layer 3
```

```bash
# 이미지 크기 확인
docker images myapp

# 레이어 구성 히스토리
docker history myapp:1.0.0
```

```bash
# 컨테이너 실행 및 확인
docker run -d -p 8080:8080 --name myapp myapp:1.0.0
docker logs -f myapp
# 브라우저에서 http://localhost:8080 확인 후 Ctrl+C

docker stop myapp && docker rm myapp
```

---

### 실습 2 — 캐시 동작 체감

같은 Dockerfile을 연속으로 두 번 빌드해서 캐시 효과를 시간으로 비교.

```bash
# 1차 빌드 (전체 실행) — 시간 측정
docker build -t myapp:cache-test . --progress=plain
```

```bash
# 2차 빌드 (캐시 사용) — 훨씬 빠름
docker build -t myapp:cache-test . --progress=plain
```

출력에서 확인할 것:
```
#2 CACHED   ← 캐시 사용된 레이어
#3 CACHED
#4 CACHED
```

```bash
# 캐시 무시하고 전체 재빌드
docker build --no-cache -t myapp:cache-test . --progress=plain
# CACHED 없이 전부 실행됨을 확인
```

---

### 실습 3 — 레이어 순서에 따른 캐시 효율 비교

캐시 비효율 Dockerfile vs 최적화 Dockerfile 두 개를 직접 만들어 비교.

```bash
# 캐시 비효율 버전 작성
cat > Dockerfile.bad << 'EOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
RUN echo "build time: $(date)" >> /app/build-info.txt
RUN echo "version: 1.0" >> /app/build-info.txt
CMD ["java", "-jar", "app.jar"]
EOF

# 최적화 버전 작성 (변경 빈도 낮은 RUN을 COPY보다 앞에)
cat > Dockerfile.good << 'EOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN echo "version: 1.0" >> /app/build-info.txt
COPY app.jar app.jar
RUN echo "build time: $(date)" >> /app/build-info.txt
CMD ["java", "-jar", "app.jar"]
EOF

# 두 버전 빌드
docker build -f Dockerfile.bad -t myapp:bad .
docker build -f Dockerfile.good -t myapp:good .

# 레이어 수 비교
docker history myapp:bad
docker history myapp:good
```

---

## 4-6. 응용 실습 문제

---

### 문제 1 — Dockerfile 직접 작성

아래 조건을 만족하는 Dockerfile을 직접 작성하고 빌드하세요.

조건:
- 베이스 이미지: `eclipse-temurin:17-jre-alpine`
- 작업 디렉터리: `/service`
- `app.jar` 를 `/service/app.jar` 로 복사
- 환경 변수 `APP_ENV=production` 설정
- 포트 `8080` 선언
- 실행 명령: `java -jar app.jar`
- 이미지 이름: `myapp:custom`

---

### 문제 2 — CMD vs ENTRYPOINT 차이 체험

아래 두 Dockerfile을 빌드하고, `docker run` 시 동작 차이를 확인하세요.

```dockerfile
# Dockerfile.cmd
FROM alpine
CMD ["echo", "기본 메시지"]
```

```dockerfile
# Dockerfile.entry
FROM alpine
ENTRYPOINT ["echo"]
CMD ["기본 메시지"]
```

확인 내용:
- `docker run --rm [이미지]` → 기본 메시지 출력
- `docker run --rm [이미지] "커스텀 메시지"` → CMD 이미지는 CMD가 교체됨, ENTRYPOINT 이미지는 인자로 전달됨

> 힌트: ENTRYPOINT는 실행 파일 고정, CMD는 인자 교체 가능.

---

### 문제 3 — ARG로 빌드 시점 버전 주입

`ARG` 를 활용해 빌드 시 버전 정보를 이미지에 넣어보세요.

조건:
- `ARG APP_VERSION=1.0.0` 선언
- `ENV VERSION=$APP_VERSION` 으로 환경 변수에 전달
- `docker build --build-arg APP_VERSION=2.0.0 -t myapp:v2 .` 로 빌드
- `docker run --rm myapp:v2 env | findstr VERSION` 으로 `VERSION=2.0.0` 확인

---

## 정리

- Dockerfile 각 명령어 = 하나의 이미지 레이어
- 변경 빈도 낮은 것 → 높은 것 순서로 작성해야 캐시 효율 극대화
- `CMD` = 기본 실행 명령 (교체 가능), `ENTRYPOINT` = 실행 파일 고정
- `COPY` 우선, `ADD`는 URL/압축 해제 필요 시만 사용