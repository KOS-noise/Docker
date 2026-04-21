# 세션 6 — Docker Hub & 이미지 Push/Pull

> 시간: 15:40 – 16:30 | 강의 20분 + 실습 30분

## 6-0. 왜 최적화된 이미지를 Push해야 하는가? (Multi-stage Build의 목적)
 
 이미지 전송(Push/Pull)과 저장(Registry) 단계에서 **Multi-stage Build**를 통해 최적화된 이미지를 사용하는 이유는 다음과 같습니다.
 
- **전송 속도 및 네트워크 비용 절감**: 빌드 도구가 빠진 가벼운 이미지만 전송하여 빠른 배포가 가능합니다.
- **저장 공간 효율성**: Docker Hub 저장 공간(Storage)을 절약하고, 이미지 풀(Pull) 시 디스크 사용량을 최소화합니다.
- **보안 및 규정 준수**: 실행 환경에 불필요한 소스코드와 컴파일러가 포함되지 않아 보안 사고 위험을 줄입니다.
 
 > **핵심**: "빌드는 무겁게(Full JDK/Build Tools), 운영 이미지은 가볍게(Runtime JRE Only)" 원칙을 지켜야 합니다.

---

## 6-1. Docker Hub 가입 및 준비

### 계정 생성

1. [https://hub.docker.com](https://hub.docker.com) 접속
2. Sign Up → 이메일 인증 완료
3. **Username** 기억하기 (이미지 이름에 사용)

### CLI 로그인

```bash
docker login

# Username: 본인계정
# Password: 비밀번호 입력 (화면에 표시 안됨)
# Login Succeeded
```

### 로그인 정보 확인

```bash
# 로그인 정보 저장 위치
cat ~/.docker/config.json
```

---

## 6-2. 이미지 이름 규칙

Docker Hub에 push하려면 이미지 이름이 아래 형식이어야 합니다.

```
[Docker Hub Username]/[Repository 이름]:[태그]

예: myuser/myapp:1.0.0
```

### 잘못된 예 vs 올바른 예

```bash
# 잘못됨 - Docker Hub에 push 불가
myapp:1.0.0

# 올바름
myuser/myapp:1.0.0
```

---

## 6-3. Tag 전략

| 전략 | 예시 | 장점 | 단점 |
|------|------|------|------|
| `latest` | `myapp:latest` | 편리함 | 어떤 버전인지 불명확, 프로덕션 지양 |
| Semantic Ver | `myapp:1.2.3` | 명확한 버전 관리 | 수동 관리 필요 |
| Git SHA | `myapp:a1b2c3d` | CI/CD 추적 용이 | 사람이 읽기 어려움 |
| 날짜 | `myapp:20260418` | 배포 시점 명확 | 기능 버전 불명확 |
| 혼합 | `myapp:1.0.0-20260418` | 버전 + 날짜 모두 | 태그가 길어짐 |

### 실무 권장 패턴

```bash
# CI/CD 파이프라인에서
IMAGE=myuser/myapp
GIT_SHA=$(git rev-parse --short HEAD)

docker build -t $IMAGE:$GIT_SHA .
docker tag $IMAGE:$GIT_SHA $IMAGE:latest
docker push $IMAGE:$GIT_SHA
docker push $IMAGE:latest
```

---

## 6-4. docker tag — 이미지에 태그 붙이기

```bash
# 현재 이미지 확인
docker images myapp

# 태그 추가 (원본 이미지는 유지됨)
docker tag myapp:1.0.0 본인username/myapp:1.0.0
docker tag myapp:1.0.0 본인username/myapp:latest

# 결과 확인
docker images 본인username/myapp
```

> `docker tag`는 이미지를 복사하는 게 아니라 **동일한 이미지에 이름 라벨을 추가**합니다.
> IMAGE ID가 같은 것을 확인할 수 있습니다.

---

## 6-5. docker push — 이미지 업로드

```bash
# Docker Hub에 push
docker push 본인username/myapp:1.0.0
docker push 본인username/myapp:latest
```

### 출력 분석

```
The push refers to repository [docker.io/본인username/myapp]
3a4f5e6b: Pushed    ← 레이어 업로드
...
1.0.0: digest: sha256:abc123... size: 1234
```

- 이미 Hub에 있는 레이어는 `Layer already exists` → 재업로드 없음
- **Docker Hub 무료 플랜:** Public 레포지토리 무제한, Private 1개

---

## 6-6. docker pull — 이미지 다운로드

```bash
# 로컬 이미지 삭제
docker rmi 본인username/myapp:1.0.0

# Docker Hub에서 다시 pull
docker pull 본인username/myapp:1.0.0

# 실행 확인
docker run -d -p 8080:8080 본인username/myapp:1.0.0
```

### 옆 사람 이미지 pull 해보기

```bash
# 옆 사람 Docker Hub 계정으로 pull
docker pull 옆사람username/myapp:1.0.0
docker run -d -p 8081:8080 옆사람username/myapp:1.0.0
```

---

## 6-7. Docker Hub Repository 관리

### 웹 UI에서 확인

- hub.docker.com → 본인 계정 → Repositories
- 태그별 이미지 목록, 크기, 업로드 시간 확인
- README 작성 가능 (이미지 설명 문서화)

### CLI로 태그 목록 조회

```bash
# Docker Hub API로 태그 목록 조회
curl -s "https://hub.docker.com/v2/repositories/본인username/myapp/tags" \
  | python -m json.tool
```

---

## 6-8. 로그아웃

```bash
docker logout
```

---

## 6-9. 실습 — 따라하기

### 실습 1 — Docker Hub 로그인 & tag 생성

```bash
# Docker Hub 로그인
docker login
# Username: 본인계정 입력
# Password: 입력 (화면에 안 보임)
# Login Succeeded 확인
```

```bash
# 현재 이미지 확인
docker images myapp

# Docker Hub용 태그 추가
docker tag myapp:1.0.0 본인username/myapp:1.0.0
docker tag myapp:1.0.0 본인username/myapp:latest

# 결과 확인 — IMAGE ID가 동일한 것 확인
docker images 본인username/myapp
```

---

### 실습 2 — push & Docker Hub 웹에서 확인

```bash
# Docker Hub에 push
docker push 본인username/myapp:1.0.0
docker push 본인username/myapp:latest
```

출력에서 확인할 것:
```
3a4f5e6b: Pushed        ← 새 레이어 업로드
c926b61b: Layer already exists  ← 베이스 이미지 레이어는 재사용
```

```bash
# 브라우저에서 확인
# hub.docker.com → 본인 계정 → Repositories → myapp
# Tags 탭에서 1.0.0 과 latest 두 태그 확인
```

---

### 실습 3 — 로컬 삭제 후 pull로 복원

로컬 이미지를 지우고 Docker Hub에서 다시 받아 실행.

```bash
# 로컬 이미지 삭제
docker rmi 본인username/myapp:1.0.0
docker rmi 본인username/myapp:latest

# 삭제 확인
docker images 본인username/myapp

# Docker Hub에서 다시 pull
docker pull 본인username/myapp:1.0.0

# 실행 확인
docker run -d -p 8080:8080 --name pulled-app 본인username/myapp:1.0.0
docker logs -f pulled-app
# 브라우저에서 http://localhost:8080 확인

docker stop pulled-app && docker rm pulled-app
```

---

## 6-10. 응용 실습 문제

---

### 문제 1 — 버전 태그 두 개 동시 관리

`myapp:1.0.0` 이미지에 버전 태그와 날짜 태그를 **동시에** push하세요.

조건:
- `본인username/myapp:1.0.0` 태그 push
- `본인username/myapp:20260418` 태그도 함께 push
- Docker Hub 웹에서 Tags 탭에 두 태그가 모두 보이는지 확인
- 두 태그의 IMAGE ID가 같은지 확인

> 힌트: `docker tag` 를 두 번 실행해서 태그를 두 개 만들면 됨.

---

### 문제 2 — 옆 사람 이미지 pull & 실행

옆 사람이 push한 이미지를 pull해서 실행하세요.

조건:
- 옆 사람의 Docker Hub username 확인
- `docker pull 옆사람username/myapp:1.0.0`
- `docker run -d -p 8081:8080 --name neighbor-app 옆사람username/myapp:1.0.0`
- 브라우저에서 `http://localhost:8081` 접속 확인
- `docker ps` 로 내 앱(8080)과 옆 사람 앱(8081)이 동시에 실행 중인 것 확인

---

### 문제 3 — 이미지 태그 전략 설계

실제 프로젝트에서 아래 상황에 어떤 태그를 사용할지 직접 정해보고, 태그를 만들어서 push해보세요.

상황:
1. 개발 중인 최신 버전 → 태그명: ?
2. 운영 배포용 안정 버전 1.2.3 → 태그명: ?
3. 오늘(2026-04-18) 배포한 버전 → 태그명: ?

조건:
- 위 3가지 태그를 모두 동일한 `myapp:1.0.0` 이미지에 붙여서 push
- Docker Hub 웹에서 3개 태그 모두 확인

---

## 정리

- Docker Hub push = `[username]/[repo]:[tag]` 형식 필수
- `docker tag` = 동일 이미지에 이름 라벨 추가 (IMAGE ID 동일)
- `latest` 태그는 편리하지만 프로덕션에서 버전 고정 권장
- 레이어 재사용으로 이미 있는 레이어는 재업로드 없음