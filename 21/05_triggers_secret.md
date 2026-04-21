# 세션 5 — Event 트리거 유형 & GitHub Secrets

---

## 5-1. Event 트리거 유형

### push — 브랜치에 코드가 push 됐을 때

```yaml
on:
  push:
    branches: [main, develop]          # 특정 브랜치만
    branches-ignore: [feature/*]       # 제외할 브랜치
    paths: ['src/**', 'build.gradle']  # 특정 파일 변경 시만
    tags: ['v*']                       # 태그 push 시
```

---

### pull_request — PR 생성/업데이트 시

```yaml
on:
  pull_request:
    branches: [main]                   # main으로 PR 올릴 때
    types: [opened, synchronize, reopened]
```

| types 값 | 의미 |
|----------|------|
| `opened` | PR 새로 생성 |
| `synchronize` | PR에 커밋 추가 |
| `closed` | PR 닫힘 (머지 포함) |
| `reopened` | 닫힌 PR 재오픈 |

---

### schedule — 정해진 시간에 자동 실행

```yaml
on:
  schedule:
    - cron: '0 0 * * *'    # 매일 자정 (UTC)
    - cron: '0 9 * * 1-5'  # 평일 오전 9시 (UTC)
```

**cron 표현식:**
```
┌─────── 분 (0-59)
│ ┌───── 시 (0-23)
│ │ ┌─── 일 (1-31)
│ │ │ ┌─ 월 (1-12)
│ │ │ │ ┌ 요일 (0-7, 0=일요일)
│ │ │ │ │
0 9 * * 1-5
```

> **주의:** GitHub Actions cron은 UTC 기준. 한국(KST = UTC+9) 오전 9시 → `0 0 * * *`

---

### workflow_dispatch — 수동 실행 (버튼 클릭)

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: '배포 환경'
        required: true
        default: 'staging'
        type: choice
        options: [staging, production]
      version:
        description: '배포 버전'
        required: false
        default: 'latest'
```

→ GitHub Actions 탭에서 "Run workflow" 버튼이 생기고, 입력값을 받아서 실행 가능.

---

### 트리거 조합 예시

```yaml
# 실무에서 자주 쓰는 패턴
on:
  push:
    branches: [main]          # main에 머지될 때 자동 배포
  pull_request:
    branches: [main]          # PR 올릴 때 테스트
  workflow_dispatch:           # 긴급 수동 배포
  schedule:
    - cron: '0 0 * * *'       # 매일 자정 보안 스캔
```

---

## 5-2. GitHub Secrets — 민감 정보 관리

### 왜 Secrets인가?

```yaml
# 절대 이렇게 하면 안 됨 — 소스코드에 비밀번호 노출
- run: docker login -u myuser -p mypassword123

# 올바른 방법 — Secrets 사용
- run: echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
```

소스코드에 비밀번호를 직접 넣으면:
- GitHub 저장소에 평문으로 저장
- 로그에 출력될 가능성
- 협업자 전원이 볼 수 있음
- Public 저장소면 전 세계에 공개

---

### Secrets 등록 방법

```
1. 저장소 → Settings 탭
2. 좌측 메뉴 → Secrets and variables → Actions
3. "New repository secret" 클릭
4. Name: DOCKER_USERNAME
   Value: 본인의 Docker Hub username
5. "Add secret" 클릭
6. 같은 방법으로 DOCKER_PASSWORD 추가
```

---

### Secrets 사용 방법

```yaml
- name: Docker Hub 로그인
  run: echo "${{ secrets.DOCKER_PASSWORD }}" | docker login \
    -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
```

```yaml
# with 절에서도 사용 가능
- name: Docker Hub 로그인
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKER_USERNAME }}
    password: ${{ secrets.DOCKER_PASSWORD }}
```

**Secrets 특징:**
- 로그에서 자동으로 `***` 로 마스킹
- 등록 후 값을 다시 볼 수 없음 (수정만 가능)
- Fork된 PR에서는 접근 불가 (보안)

---

## 5-3. 환경 변수 vs Secrets

| 항목 | 환경 변수 (`env`) | Secrets |
|------|-----------------|---------|
| 민감도 | 낮음 | 높음 |
| 로그 마스킹 | 없음 | 자동 마스킹 |
| 소스코드 노출 | 됨 | 안 됨 |
| 용도 | APP_ENV, PORT 등 | 비밀번호, API Key |

```yaml
jobs:
  build:
    env:
      APP_ENV: production       # 환경 변수 (평문, 로그에 보임)
    steps:
      - name: 빌드
        run: ./gradlew build
        env:
          JAVA_OPTS: -Xmx512m   # step 단위 환경 변수

      - name: Docker 로그인
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | \
          docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
```

---

## 5-4. 실습 — 따라하기

### 실습 1 — workflow_dispatch 트리거로 수동 실행

```yaml
# .github/workflows/manual.yml
name: 수동 실행 테스트

on:
  workflow_dispatch:
    inputs:
      message:
        description: '출력할 메시지'
        required: true
        default: 'Hello!'
      environment:
        description: '환경'
        required: true
        type: choice
        options: [dev, staging, production]

jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - name: 입력값 출력
        run: |
          echo "메시지: ${{ github.event.inputs.message }}"
          echo "환경: ${{ github.event.inputs.environment }}"
```

```
저장 후 Actions 탭 → "수동 실행 테스트" → "Run workflow" 버튼
→ 입력값 넣고 실행 → 로그 확인
```

---

### 실습 2 — GitHub Secrets 등록 및 사용

```
1. 저장소 Settings → Secrets and variables → Actions
2. DOCKER_USERNAME 등록 (Docker Hub 계정)
3. DOCKER_PASSWORD 등록 (Docker Hub 비밀번호)
```

```yaml
# .github/workflows/secrets-test.yml
name: Secrets 테스트

on:
  workflow_dispatch:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Secret 사용 확인
        run: |
          echo "유저명: ${{ secrets.DOCKER_USERNAME }}"
          echo "비밀번호: ${{ secrets.DOCKER_PASSWORD }}"
```

실행 후 로그에서:
- `유저명` 은 실제 값 출력
- `비밀번호` 는 `***` 로 마스킹 확인

---

### 실습 3 — pull_request 트리거로 PR 시 자동 검증

```yaml
# .github/workflows/pr-check.yml
name: PR 검증

on:
  pull_request:
    branches: [main]

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: PR 정보 출력
        run: |
          echo "PR 번호: ${{ github.event.number }}"
          echo "PR 제목: ${{ github.event.pull_request.title }}"
          echo "작성자: ${{ github.actor }}"
          echo "베이스 브랜치: ${{ github.base_ref }}"
          echo "소스 브랜치: ${{ github.head_ref }}"
```

```
1. 새 브랜치 생성 (GitHub 웹에서 "feature/test" 브랜치 생성)
2. 파일 하나 수정 후 커밋
3. main으로 PR 생성
4. Actions 탭에서 자동 실행 확인
```

---

## 5-5. 응용 실습 문제

---

### 문제 1 — schedule 트리거 설정

매일 한국 시간 오전 9시(평일만)에 실행되는 Workflow를 작성하세요.

조건:
- cron 표현식으로 UTC 기준 시간 계산
- 실행 시 현재 날짜와 "정기 점검 실행" 메시지 출력
- workflow_dispatch 트리거도 함께 추가 (수동으로도 실행 가능하게)

> 힌트: KST = UTC+9, 오전 9시 KST = 0시 UTC → `0 0 * * 1-5`

---

### 문제 2 — Secret을 사용한 Docker Hub 로그인 Workflow

Docker Hub Secrets를 활용해 로그인하는 Workflow를 작성하세요.

조건:
- `DOCKER_USERNAME`, `DOCKER_PASSWORD` Secrets 등록
- `docker/login-action@v3` Action 사용
- 로그인 성공 후 `docker images` 로 확인
- 로그에서 비밀번호가 `***` 로 마스킹되는지 확인

---

### 문제 3 — pull_request 시 빌드 차단 설정

main 브랜치로 PR을 올릴 때 자동으로 아래를 실행하는 Workflow를 작성하세요.

조건:
- `actions/checkout@v4` 로 체크아웃
- `actions/setup-java@v4` 로 Java 17 설치
- `./gradlew build` 실행
- 빌드 실패 시 PR 머지 차단 (Branch Protection Rules 설정)

> 힌트: Settings → Branches → Branch protection rules → "Require status checks"

---

## 정리

- `push` = 브랜치 push 시, `pull_request` = PR 시, `schedule` = 정기 실행, `workflow_dispatch` = 수동
- Secrets = 민감 정보 저장소, 로그 자동 마스킹
- 소스코드에 비밀번호 직접 입력 절대 금지
- 환경 변수는 평문, Secrets는 암호화 저장