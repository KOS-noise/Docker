# 세션 6 — GitHub Actions로 백엔드 Docker 이미지 빌드 자동화

---

## 6-1. CI Workflow 구조

GitHub Actions를 활용하면 코드를 푸시할 때마다 클라우드에서 자동으로 소스코드를 가져와서 Docker 이미지를 만들 수 있습니다. 내 로컬 컴퓨터에서 수동으로 `docker build` 하던 과정을 자동화하여, 새로 올린 코드가 에러 없이 컨테이너 이미지로 잘 만들어지는지 검증하는 단계입니다.

```
push / PR (코드 변경 발생)
  ↓
1. 코드 체크아웃 (빈 컴퓨터에 소스코드 다운로드)
  ↓
2. Docker 빌드 (백엔드 Dockerfile 실행)
  ↓
3. 이미지 생성 확인 (docker images 확인)
```

---

## 6-2-1. 내부 DockerFile 사용 Workflow 파일 작성

복잡한 캐시나 테스트 설정 등은 모두 빼고, 오직 **백엔드 코드가 정상적으로 Docker 이미지로 구워지는지 빌드만 검증**하는 아주 직관적인 스크립트입니다.

```yaml
# .github/workflows/backend-build.yml
name: Backend Docker Build Test

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: 1. 소스코드 내려받기 (Checkout)
        uses: actions/checkout@v4

      - name: 2. 백엔드 Docker 이미지 빌드
        # backend 폴더 안에 있는 Dockerfile을 사용해 'my-backend:latest'라는 이름으로 빌드합니다.
        run: docker build -t my-backend:latest ./backend
        
      - name: 3. 생성된 Docker 이미지 목록 확인
        # 도커 이미지가 실제로 정상 생성되었는지 리스트를 출력해 눈으로 확인합니다.
        run: docker images
```


## 6-2-2. 내부 DockerFile 미사용 Workflow 파일 작성

이 방식은 코드를 빌드할 때 사용하는 `bootJar` 대신, 컨테이너 이미지까지 한 방에 생성해 주는 `bootBuildImage`라는 특수한 Gradle 명령어를 사용합니다.

```yaml
# .github/workflows/backend-buildpack.yml
name: Dockerfile-less Spring Boot Build

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: 1. 소스코드 체크아웃
        uses: actions/checkout@v4

      - name: 2. Java 17 및 Gradle 셋업 (캐시 적용)
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle' 

      - name: 3. ✨ Dockerfile 없이 도커 이미지 생성 (Buildpacks)
        working-directory: ./backend
        
        run: gradle bootBuildImage --imageName=my-magic-backend:latest

      - name: 4. 생성된 Docker 이미지 확인
        # 정말로 Dockerfile 없이 이미지가 생겼는지 확인합니다.
        run: docker images
```

---