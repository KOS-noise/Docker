# 세션 0 — Git 핵심 개념 및 실습 가이드

> 대상: Git 사용이 낯설거나 협업 환경에서의 기본기가 필요한 분
> 학습 목표: Git의 3단계 영역을 이해하고, 실무에서 가장 많이 쓰이는 명령어를 직접 실습하며 익힙니다.

---

## 0-1. Git의 핵심: 3단계 영역

Git은 파일을 세 가지 상태로 관리합니다. 이 흐름을 이해하는 것이 가장 중요합니다.

1. **Working Directory**: 내가 현재 작업하고 있는 실제 폴더 (수정 중인 파일)
2. **Staging Area (Index)**: 커밋하기 전, "기록할 대상"으로 찜해두는 장바구니
3. **Repository (Local)**: `.git` 폴더에 최종 기록된 버전 저장소

```text
 [ Working Directory ] ──( git add )──> [ Staging Area ] ──( git commit )──> [ Repository ]
       (수정 중)                         (준비 완료)                        (기록 완료)
```

---

## 0-2. 필수 명령어 한눈에 보기

### 1) 기본 설정 및 흐름

- `git init`: 현재 폴더를 Git 저장소로 초기화
- `git status`: 현재 상태 확인 (제일 많이 사용!)
- `git add <파일명>`: 파일을 Staging Area로 올리기
- `git commit -m "메시지"`: 변경 사항을 기록
- `git log`: 커밋 히스토리 확인

### 2) 브랜치와 병합 (Branch & Merge)

- `git branch`: 브랜치 목록 확인
- `git switch -c <이름>`: 새 브랜치 만들고 이동 (옛날 방식: `checkout -b`)
- `git merge <대상>`: 다른 브랜치의 내용을 현재 브랜치로 합치기
- `git rebase <대상브랜치>`: 현재 브랜치의 시작점을 대상 브랜치의 최신 커밋으로 이동 (직선형 히스토리)
- `git cherry-pick <커밋ID>`: 다른 브랜치의 특정 커밋 하나만 현재 브랜치에 적용

### 3) 원격 저장소 (Remote)

- `git remote add origin <URL>`: 원격 저장소 연결
- `git push origin <브랜치>`: 로컬 내용을 원격에 올리기
- `git pull origin <브랜치>`: 원격 내용을 로컬로 가져와서 합치기
- `git clone <URL>`: 원격 저장소를 통째로 복제해오기

---

## 0-3. 단계별 실습 미션 (Hands-on)

실습을 위해 터미널(또는 CMD/PowerShell)을 열고 아래 과정을 직접 따라 하세요.

### 🚩 미션 1: 로컬 저장소 시작하기

1. `C:\git-lab` (또는 원하는 경로) 폴더 생성 후 이동
2. `git init` 으로 저장소 시작
3. `a.txt` 파일 생성 (내용: `version 1`)
4. `git status` 로 빨간색(Untracked) 파일 확인
5. `git add a.txt` 후 `git status` 로 녹색(Staged) 파일 확인
6. `git commit -m "feat: first commit"` 실행
7. `git log` 로 기록 확인

### 🚩 미션 2: 브랜치 나누고 기능 개발하기

1. `git switch -c feature/login` 개발용 브랜치 생성
2. `b.txt` 파일 생성 (내용: `login logic`)
3. `git add .` -> `git commit -m "feat: add login logic"`
4. `git switch master` 으로 복귀 (이때 `b.txt`가 사라지는지 확인!)

### 🚩 미션 3: ⚠️ 충돌(Conflict) 해결하기 (매우 중요)

1. `master` 브랜치에서 `a.txt` 2행에 `master edit` 추가 후 커밋
2. `feature/login` 브랜치로 이동 후 `a.txt` 2행에 `login edit` 추가 후 커밋
3. `master` 브랜치로 이동 후 `git merge feature/login` 실행
4. **CONFLICT** 에러 발생 확인!
5. `a.txt` 파일을 열어 `<<<<`, `====`, `>>>>` 부분 수정 후 저장
6. `git add a.txt` -> `git commit` (머지 완료)

### 🚩 미션 4: 원격 저장소와 놀기

1. GitHub에서 `git-practice` 라는 비어있는 리포지토리 생성
2. `git remote add origin <본인URL>`
3. `git push origin master`
4. GitHub 웹사이트에서 파일들이 올라갔는지 확인

---

## 0-4.응용 명령어


| 상황                                   | 해결책                            |
| ------------------------------------ | ------------------------------ |
| 방금 한 commit 메시지를 바꾸고 싶을 때            | `git commit --amend -m "새메시지"` |
| `add` 한 파일을 취소하고 싶을 때                | `git restore --staged <파일>`    |
| 코드를 수정했는데 다 날리고 커밋 상태로 돌아갈 때         | `git restore <파일>`             |
| 하던 작업을 잠시 치워두고 브랜치를 바꾸고 싶을 때         | `git stash` -> `git stash pop` |
| 아예 과거 커밋으로 완전히 돌아가고 싶을 때 (위험!)       | `git reset --hard <커밋ID>`      |
| 브랜치 분기점을 최신 커밋 뒤로 옮겨 히스토리를 정리하고 싶을 때 | `git rebase <대상브랜치>`           |
| 다른 브랜치의 특정 커밋 하나만 현재 브랜치에 가져오고 싶을 때  | `git cherry-pick <커밋ID>`       |


---

## 0-5. 응용 실습 문제 (Git Challenges)

---

### 문제 1 — 작업 내용 임시 보관 후 다시 적용하기

`d.txt` 파일에서 복잡한 코드를 작성하던 중(커밋 안 함), 갑자기 `master` 브랜치의 버그를 고쳐야 하는 상황이 생겼습니다. 현재 작업 중인 내용을 임시로 보관한 뒤, 버그 수정 작업을 끝내고 나서 보관했던 내용을 다시 복원하세요.

- **조건**: stash 직후 `git status`에서 Clean state여야 하고, pop 이후 `d.txt`의 작업 내용이 되살아나야 함.
- **사용 명령어**: `git stash`, `git stash pop`

---

### 문제 2 — 브랜치 히스토리 정리하기 (Rebase)

`feature/signup` 브랜치를 만들고 커밋을 2개 추가하세요. 그 사이 `master` 브랜치에도 새로운 커밋이 하나 생겼습니다. `feature/signup` 브랜치의 시작점을 `master`의 최신 커밋 위로 옮겨 히스토리를 일직선으로 정리하세요.

- **조건**: `git log --oneline --graph` 로 분기 없는 직선형 히스토리가 보여야 함.
- **사용 명령어**: `git rebase`

---

### 문제 3 — 특정 커밋만 골라 가져오기 (Cherry-pick)

`feature/experiment` 브랜치에 커밋이 3개 있습니다. 그 중 두 번째 커밋의 변경 사항만 `master` 브랜치에 적용하고 싶습니다. 해당 커밋 ID를 찾아 `master` 브랜치에 선택적으로 반영하세요.

- **조건**: `master` 브랜치의 `git log`에 해당 커밋이 새로 추가되어야 함.
- **사용 명령어**: `git cherry-pick`

---

### 문제 4 — 특정 시점으로 강제 이동 (Hard Reset)

최근 두 개의 커밋이 모두 마음에 들지 않습니다. `git log`를 확인하여 2개 이전의 커밋 ID를 찾고, 프로젝트 전체를 그 시점으로 완전히 되돌리세요.

- **조건**: 현재 작업 폴더의 모든 파일이 해당 과거 시점으로 바뀌어야 함.
- **사용 명령어**: `git reset --hard`

---

## 정리 및 마무리

- **Local (3단계 영역)**: Working Directory -> Staging Area -> Repository
- **Remote**: `git push`로 공유, `git pull`로 업데이트
- **Branch**: 독립된 작업 공간, `merge` 시 충돌 주의
- **Undo**: `restore`(파일), `amend`(메시지), `reset`(전체)

> **Next Session**: 이제 소스코드가 준비되었으니, 이 코드를 자동으로 빌드해주는 GitHub Actions를 배워보겠습니다.

