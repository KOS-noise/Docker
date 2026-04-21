# 세션 7 — 이미지 보안 (민감 정보 관리)

> 시간: 16:40 – 17:30 | 강의 20분 + 실습 30분

---

## 7-1. 컨테이너 보안의 최우선 과제: "데이터 유출 방지"

대부분의 보안 사고는 기술적인 탈출 공격보다 **실수로 포함시킨 설정 파일(.env)**이나 **API 키**가 레지스트리에 올라가면서 발생합니다.

- **이미지는 영구적입니다**: 한 번 푸시된 이미지는 삭제해도 누군가 다운로드했을 수 있습니다.
- **레이어는 모든 것을 기억합니다**: Dockerfile에서 `RUN rm .env`를 해도 이전 레이어에 기록된 파일은 추출이 가능합니다.
- **공격자들의 타겟**: 공개된 레지스트리(Docker Hub 등)는 공격자들이 자동화 스캐너로 민감 정보를 수집하는 주요 장소입니다.

---

## 7-2. `.dockerignore` — 유출 방지의 첫 번째 방어선

빌드 시점에 아예 파일을 Docker 엔진으로 보내지 않는 것이 가장 확실한 방법입니다.

### 왜 `COPY . .`가 위험한가?
많은 개발자들이 편의를 위해 `COPY . .`를 사용합니다. 이때 `.env`, `.git`, `id_rsa` 같은 민감 파일이 의도치 않게 이미지 내부로 복사됩니다.

### `.dockerignore` 작성 예시
```text
# 반드시 제외해야 할 목록
.env
.env.*
*.key
*.pem
secrets/
.git
.gitignore

# 빌드 및 로그
build/
target/
*.log
```

---

## 7-3. 올바른 설정 주입법: "Runtime Injection"

민감 정보는 이미지에 박아넣는(Bake) 것이 아니라, 컨테이너를 **실행(Run)할 때 주입**해야 합니다.

### 1. 환경 변수 직접 주입 (`-e`)
```bash
docker run -d -e DB_PASSWORD=mysecret123 myapp:1.0.0
```

### 2. 환경 변수 파일 사용 (`--env-file`)
실무에서 가장 많이 쓰는 방식입니다. 로컬의 `.env` 파일을 컨테이너 내부에 전달합니다.
```bash
docker run -d --env-file .env myapp:1.0.0
```

> [!TIP]
> 이 방식을 사용하면 하나의 이미지로 개발용(Dev), 운영용(Prod) 설정을 자유롭게 바꿔가며 실행할 수 있습니다.

---

## 7-4. 이미지 레이어 이력(History) 확인

도커 이미지는 쌓아 올린 필름과 같아서, 중간에 파일을 지워도 아래 필름에는 남아있습니다.

### 유출 확인 명령어
```bash
docker history myapp:1.0.0
```
- 어떤 명령어(`COPY`, `RUN`)가 실행되었는지, 어떤 파일이 추가되었는지 한눈에 볼 수 있습니다.
- 만약 `COPY .env .` 같은 기록이 있다면, 해당 이미지는 보안상 폐기해야 합니다.

---

## 7-5. (참고) Spring Boot 환경 변수 매핑 규칙

스프링 부트는 도커 주입한 환경 변수를 설정 파일(`${...}`)과 자동으로 매핑해 줍니다.

### 1. YAML 환경 설정 예시
```yaml
# application.yml
spring:
  datasource:
    url: ${DB_URL}
    password: ${DB_PASSWORD}
```

### 2. 매핑 규칙 (Relaxed Binding)
직접 명시하지 않아도 스프링은 아래 규칙으로 자동 매핑합니다.
- **도커 주입 변수:** `SPRING_DATASOURCE_PASSWORD`
- **스프링 설정:** `spring.datasource.password`

---

## 7-6. 실습 — 따라하기 (중요!)

### 실습 1 — `.env` 파일 유출 사고 재현하기

1. **빌드 폴더로 이동 및 민감 파일 생성**
   ```bash
   cd D:/test/강의/20260418/spring-demo
   echo "MY_DB_PASSWORD=super-secret-1234" > .env
   ```

2. **`.dockerignore` 없이 빌드 (이름 잠시 변경)**
   ```bash
   mv .dockerignore .dockerignore.bak
   docker build -t myapp:leaked .
   ```

3. **파일 유출 확인 (보안 사고 상황)**
   ```bash
   docker run --rm myapp:leaked ls -la /app/.env
   # 결과: 파일이 존재함! 누구나 비밀번호를 볼 수 있는 상태
   ```

---

### 실습 2 — `.dockerignore` 차단 및 외부 주입하기

1. **`.dockerignore` 복구 및 다시 빌드**
   ```bash
   mv .dockerignore.bak .dockerignore
   docker build -t myapp:safe .
   ```

2. **이미지 내부 확인 (차단 확인)**
   ```bash
   docker run --rm myapp:safe ls -la /app/.env
   # 결과: ls: /app/.env: No such file or directory (안전함)
   ```

3. **외부 파일을 통한 '설정 주입' 실행**
   ```bash
   # 이미지 내부엔 파일이 없지만, 실행 시점에만 읽어오게 함
   docker run -d --name secure-app --env-file .env myapp:safe
   ```

4. **주입된 데이터 확인**
   ```bash
   docker exec secure-app env | Select-String "MY_DB_PASSWORD"
   # 결과: MY_DB_PASSWORD=super-secret-1234
   ```

---

### 실습 3 — "고스트 비밀번호" 찾기 (Layer 분석)

이미 안전하게 작성된 `Dockerfile.multi` 대신, **일부러 나쁜 습관을 담은 실험용 파일(`Dockerfile.bad`)**로 보안 유출을 재현해 봅니다.

1. **실험용 `Dockerfile.bad` 내용 확인**
   - 현재 폴더에 아래 내용으로 파일이 있는지 확인합니다.
   ```dockerfile
   FROM alpine
   COPY .env .env      # 1단계: 민감 파일 복사 (데이터가 레이어에 구워짐)
   RUN rm .env         # 2단계: 파일 삭제 (이미지 내부에서는 안 보임)
   CMD ["sh"]
   ```

2. **빌드 및 이력 확인**
   ```bash
   # 반드시 -f Dockerfile.bad 옵션을 사용하세요!
   docker build -f Dockerfile.bad -t myapp:ghost .

   docker history myapp:ghost
   # 결과: 'COPY .env .env' 레이어가 8kB 정도의 용량과 함께 남아있음을 확인

   docker save myapp:ghost -o ghost.tar

   # 윈도우 명령어
   Get-ChildItem temp_ghost/blobs/sha256 -File | ForEach-Object {
    $check = tar -tf $_.FullName 2>$null
    if ($check -match ".env") {
        Write-Host "find file: $($_.Name)" -ForegroundColor Cyan
        tar -xOf $_.FullName .env
    }
   }
   ```

> [!CAUTION]
> `RUN rm`으로 파일을 지우더라도, `docker history`에 해당 레이어의 용량(예: 8.19kB)이 남아있다면 이는 파일 데이터가 이미지 내부에 여전히 존재한다는 증거입니다.

---

## 7-7. 응용 실습 문제

### 문제 1 — 실무형 `.dockerignore` 완성하기
현재 프로젝트 폴더에서 `.git`, `Dockerfile`, `build.gradle` 등 앱 실행에 불필요한 모든 파일을 제외하고, 가장 핵심적인 `app.jar`만 포함되도록 `.dockerignore`를 최적화하세요.

### 문제 2 — 환경 변수로 Spring Profile 제어하기
Spring Boot는 `SPRING_PROFILES_ACTIVE` 환경 변수로 설정을 바꿀 수 있습니다. `docker run -e` 옵션을 사용하여 `prod` 프로필로 앱을 구동해 보세요.

---

## 정리
- **이미지에 비밀을 담지 마세요**: `.dockerignore`는 선택이 아닌 필수입니다.
- **설정은 실행 시 주입하세요**: `-e` 또는 `--env-file`을 활용합니다.
- **레이어를 신뢰하지 마세요**: `RUN rm`은 용량은 줄여주지만 보안은 책임지지 않습니다.
