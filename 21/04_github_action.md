# 세션 4 — GitHub Actions 구성요소: on, jobs, steps, uses, run

---

## 4-1. Workflow 파일 위치 & 구조

GitHub Actions Workflow는 저장소의 `.github/workflows/` 폴더에 YAML 파일로 작성.

```
저장소/
└── .github/
    └── workflows/
        ├── build.yml      ← Workflow 파일 1
        └── deploy.yml     ← Workflow 파일 2
```

---

## 4-2. 전체 구성요소 한눈에 보기

```yaml
name: 워크플로우 이름          # ← Workflow 이름 (GitHub UI에 표시)

on:                            # ← 트리거 (언제 실행할지)
  push:
    branches: [main]

jobs:                          # ← 작업 목록
  build:                       # ← Job 이름
    runs-on: ubuntu-latest     # ← 실행 환경 (Runner)

    steps:                     # ← 단계 목록
      - name: 코드 체크아웃    # ← step 이름 (선택)
        uses: actions/checkout@v4   # ← 외부 Action 사용

      - name: Java 설치
        uses: actions/setup-java@v4
        with:                  # ← Action 입력값
          java-version: '17'
          distribution: 'temurin'

      - name: 빌드
        run: ./gradlew build   # ← 쉘 명령어 직접 실행
```

---

## 4-3. on — 트리거

> **목적**: Workflow가 "어떤 사건이 발생했을 때" 자동으로 실행될지 조건을 선언합니다.
> 사람이 수동으로 실행하는 대신, 코드 push·PR 생성·스케줄 등 특정 이벤트에 반응하도록 만드는 것이 핵심입니다.

언제 Workflow를 실행할지 정의.

```yaml
# push 시 실행
on: push

# 여러 이벤트
on: [push, pull_request]

# 브랜치 지정
on:
  push:
    branches: [main, develop]

# 파일 경로 지정 (해당 파일 변경 시만 실행)
on:
  push:
    paths:
      - 'src/**'
      - 'build.gradle'
```

---

## 4-4. jobs — 작업 단위

> **목적**: 하나의 Workflow 안에서 해야 할 큰 작업들을 독립적인 단위로 나눕니다.
> 각 Job은 별도의 가상 머신에서 실행되므로, 테스트·빌드·배포처럼 역할이 다른 작업을 분리하거나 병렬로 돌릴 수 있습니다.
> `needs`를 사용하면 "이 Job이 끝나야 다음 Job을 시작"하는 순서 제어도 가능합니다.

여러 Job을 정의할 수 있고 각각 독립된 Runner에서 실행.

```yaml
jobs:
  test:                          # Job 1
    runs-on: ubuntu-latest
    steps:
      - run: echo "테스트"

  build:                         # Job 2
    runs-on: ubuntu-latest
    needs: test                  # test Job 완료 후 실행
    steps:
      - run: echo "빌드"

  deploy:                        # Job 3
    runs-on: ubuntu-latest
    needs: [test, build]         # 두 Job 모두 완료 후 실행
    steps:
      - run: echo "배포"
```

```
test ──► build ──► deploy
```

---

## 4-5. runs-on — 실행 환경

> **목적**: 각 Job이 어떤 운영체제의 가상 머신(Runner) 위에서 실행될지 지정합니다.
> 빌드·테스트 명령어가 OS에 따라 달라지므로, 배포 대상 환경과 맞는 OS를 선택하는 것이 중요합니다.
> 대부분의 경우 `ubuntu-latest`를 사용하며, Windows 전용 빌드나 iOS 빌드가 필요할 때 다른 값을 선택합니다.

| 값 | 환경 |
|----|------|
| `ubuntu-latest` | Ubuntu (가장 많이 사용) |
| `ubuntu-22.04` | Ubuntu 22.04 고정 |
| `windows-latest` | Windows Server |
| `macos-latest` | macOS |

```yaml
jobs:
  build:
    runs-on: ubuntu-latest    # Linux 환경에서 실행
```

---

## 4-6. steps — 단계

> **목적**: Job 안에서 실제로 수행할 작업들을 순서대로 나열하는 목록입니다.
> 각 step은 "코드 받아오기 → 환경 설치 → 빌드 → 테스트"처럼 하나의 흐름을 단계별로 표현하며,
> `uses`(외부 Action 재사용)와 `run`(직접 명령어 실행) 두 가지 방식으로 작성합니다.

각 Job 내 순서대로 실행되는 단계.

```yaml
steps:
  # 이름 있는 step
  - name: 체크아웃
    uses: actions/checkout@v4

  # 이름 없는 step (권장하지 않음)
  - run: echo "hello"

  # 여러 줄 명령어
  - name: 빌드 & 테스트
    run: |
      ./gradlew build
      ./gradlew test

  # 환경 변수 설정
  - name: 환경 변수 사용
    run: echo "앱 이름: $APP_NAME"
    env:
      APP_NAME: myapp
```



## 4-7. run — 쉘 명령어 실행

> **목적**: 터미널에서 직접 입력하던 명령어를 Workflow step 안에서 그대로 실행합니다.
> `uses`가 미리 만들어진 Action을 가져다 쓰는 것이라면, `run`은 내가 원하는 명령어를 자유롭게 실행할 때 사용합니다.
> 빌드 명령(`./gradlew build`), 파일 조작, 스크립트 실행 등 어떤 쉘 명령어든 사용 가능합니다.

```yaml
# 단일 명령
- run: echo "Hello"

# 여러 줄 (| 사용)
- run: |
    echo "step 1"
    echo "step 2"
    ./gradlew bootJar

# 쉘 지정
- run: echo "Hello"
  shell: bash

# 작업 디렉터리 지정
- run: ./gradlew build
  working-directory: ./backend
```

---

## 4-9. 컨텍스트 변수 — 자동으로 제공되는 값

> **목적**: Workflow 실행 시 GitHub이 자동으로 제공하는 정보(커밋 ID, 브랜치명, 실행자 등)를 참조합니다.
> 이미지 태그에 커밋 SHA를 붙이거나, 특정 브랜치일 때만 배포하는 조건 분기 등에 활용합니다.
> 직접 하드코딩하지 않아도 되므로 Workflow를 재사용 가능하게 만드는 핵심 수단입니다.

```yaml
# 자주 쓰는 컨텍스트 변수
${{ github.sha }}          # 현재 커밋 SHA (이미지 태그로 사용)
${{ github.ref_name }}     # 브랜치 또는 태그 이름
${{ github.actor }}        # 커밋한 사람
${{ github.repository }}   # 저장소 이름 (owner/repo)
${{ secrets.MY_SECRET }}   # Secrets 값
```

---

## 4-10. 실습 — 따라하기

### 실습 1 — Hello World Workflow 생성

GitHub 저장소에서 직접 Workflow 파일 생성.

```
1. 저장소 → Actions 탭
2. "New workflow" 또는 "set up a workflow yourself"
3. 파일명: hello.yml
```

```yaml
name: Hello World

on:
  push:
    branches: [main]

jobs:
  greet:
    runs-on: ubuntu-latest

    steps:
      - name: 인사
        run: echo "Hello, GitHub Actions!"

      - name: 현재 시간
        run: date

      - name: 환경 정보
        run: |
          echo "OS: $RUNNER_OS"
          echo "워크스페이스: $GITHUB_WORKSPACE"
          echo "커밋: $GITHUB_SHA"
```

```
4. Commit changes → main 브랜치에 직접 커밋
5. Actions 탭에서 실행 확인
6. 각 step 클릭해서 출력 확인
```

---

### 실습 2 — Java 설치 포함 Workflow

```yaml
name: Java Build

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: 코드 체크아웃
        uses: actions/checkout@v4

      - name: Java 17 설치
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Java 버전 확인
        run: java -version

      - name: 저장소 내용 확인
        run: ls -la
```

GitHub에서 파일 저장 → Actions 탭에서 실행 확인.

---

### 실습 3 — 실패하는 step과 후속 동작 확인

```yaml
name: Step Control

on: [push]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: 성공 step
        run: echo "성공"

      - name: 실패 step
        run: exit 1

      - name: 실패 후 이 step은?
        run: echo "이건 실행될까?"

      - name: 항상 실행
        if: always()
        run: echo "always()는 항상 실행됨"
```

확인할 것:
- "실패 후 이 step은?" 이 스킵되는지
- `if: always()` step은 실행되는지
- 전체 Job 상태가 실패(빨간 X)인지

---

## 4-11. 응용 실습 문제

---

### 문제 1 — Hello World Workflow 작성

아래 조건의 Workflow를 직접 작성하고 실행하세요.

조건:
- 이름: `My First Workflow`
- 트리거: `main` 브랜치 push
- Job 이름: `hello`
- steps:
  1. "안녕하세요" 출력
  2. 현재 날짜와 시간 출력
  3. `github.actor` 컨텍스트로 커밋한 사람 이름 출력

---

### 문제 2 — 멀티 step Java Workflow

아래 조건의 Workflow를 작성하세요.

조건:
- `actions/checkout@v4` 로 코드 체크아웃
- `actions/setup-java@v4` 로 Java 17 설치
- `java -version` 출력
- `./gradlew --version` 출력 (Gradle wrapper 버전)
- 커밋 SHA 출력 (`${{ github.sha }}`)

---

### 문제 3 — needs로 Job 순서 제어

아래 구조의 Workflow를 작성하세요.

```
test Job → build Job → notify Job
```

조건:
- `test` Job: `echo "테스트 실행"` 출력
- `build` Job: test 완료 후 `echo "빌드 실행"` 출력
- `notify` Job: build 완료 후 `echo "배포 완료 알림"` 출력
- Actions 탭에서 세 Job이 순서대로 실행되는지 확인

---

## 정리

- Workflow = `.github/workflows/*.yml` 파일
- `on` = 트리거, `jobs` = 작업 단위, `steps` = 실행 단계
- `uses` = 외부 Action 재사용, `run` = 쉘 명령어 직접 실행
- `needs` 로 Job 간 의존 순서 설정
- `${{ github.sha }}` 등 컨텍스트 변수로 자동 값 참조