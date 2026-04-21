# 세션 2 — Image · Container · Registry 3요소


## 2-1. 3요소 관계도

```
┌─────────────┐   push / pull   ┌──────────────────┐
│   Registry  │ ◄────────────── │      Image       │
│ (Docker Hub)│                 │  (읽기 전용 템플릿)│
└─────────────┘                 └────────┬─────────┘
                                         │ run (인스턴스화)
                                         ▼
                                ┌──────────────────┐
                                │    Container     │
                                │  (실행 중인 인스턴스)│
                                └──────────────────┘
```

| 요소 | 비유 | 특징 |
|------|------|------|
| Image | 클래스(Class) | 읽기 전용, 불변 |
| Container | 인스턴스(Object) | 실행 가능, 상태 변경 가능 |
| Registry | Maven Central / npm Registry | 이미지 저장소 |

---



## 2-2. Image Layer 구조

Docker 이미지는 **여러 레이어의 스택**으로 구성됨.

```
┌─────────────────────────────┐
│  Container Layer (읽기/쓰기) │ ← 컨테이너 실행 시 생성 (CoW)
├─────────────────────────────┤
│  Layer 4: COPY app.jar      │
├─────────────────────────────┤  읽기 전용
│  Layer 3: RUN apt-get ...   │  (이미지 레이어)
├─────────────────────────────┤
│  Layer 2: ENV JAVA_HOME=... │
├─────────────────────────────┤
│  Layer 1: FROM ubuntu:22.04 │ ← 베이스 이미지
└─────────────────────────────┘
```

### Union File System (OverlayFS)

- 여러 레이어를 하나의 파일시스템처럼 보이게 합함
- 동일한 레이어는 여러 이미지가 **공유** → 디스크 절약
- 컨테이너에서 파일 수정 시 **CoW(Copy-on-Write)**: 변경된 파일만 컨테이너 레이어에 복사

---

## 2-3. Docker Registry의 역할

### Registry란 무엇인가?
도커 이미지가 저장된 **'거대한 이미지 창고'**임. 
스마트폰의 **'앱스토어'**와 동일한 원리.

- **이미지(App)**: 제작된 프로그램 뭉치
- **레지스트리(App Store)**: 프로그램을 보관하고 배포하는 공간

### Registry의 필요성
로컬 PC의 이미지를 레지스트리에 업로드해야 하는 이유:

1.  **공유와 배포**: 로컬 PC의 이미지를 **운영 서버**나 **타인의 PC**로 전달하기 위해 필수 (메일/USB 대신 `push`/`pull` 사용)
2.  **버전 관리**: 프로그램 버전별(1.0, 2.0 등) 안전한 보관 및 필요 시 즉시 추출 가능
3.  **백업**: 로컬 데이터 손실 시 레지스트리를 통한 즉각적인 환경 복구 가능

---

## 2-4. Registry 종류

| Registry | 특징 | 사용 사례 |
|----------|------|----------|
| Docker Hub | 공개 레지스트리, 무료 플랜 있음 | 공개 이미지, 개인 프로젝트 |
| Amazon ECR | AWS 통합, 프라이빗 | AWS 기반 프로젝트 |
| Google GCR/AR | GCP 통합 | GCP 기반 프로젝트 |
| GitHub GHCR | GitHub Actions 통합 | CI/CD 파이프라인 |
| Harbor | 사내 구축형 오픈소스 | 보안 요구가 높은 기업 |

### 이미지 이름 구조

```
[registry]/[namespace]/[repository]:[tag]

예시:
docker.io/library/nginx:alpine        ← Docker Hub 공식 이미지
docker.io/myuser/myapp:1.0.0          ← 개인 이미지
123456789.dkr.ecr.ap-northeast-2.amazonaws.com/myapp:latest  ← ECR
```

> `docker pull nginx`는 `docker pull docker.io/library/nginx:latest`와 동일함.  
> `docker pull myuser/myapp`은 `docker pull docker.io/myuser/myapp:latest`와 동일함.

> **💡 이름이 똑같으면 어떻게 되나요?**  
> 공식 이미지 이름이 `nginx`인데 내 이미지의 이름도 `nginx`라면?  
> - 공식: `library/nginx` (단축형: `nginx`)  
> - 내 것: `myuser/nginx`  
> 앞의 **ID(네임스페이스)**가 다르기 때문에 도커는 이를 완전히 다른 서비스로 구분함. 즉, 이름이 겹쳐도 내 아이디만 붙이면 충돌 걱정 없음.

---


### 💡 도커 핵심 관리 명령어 (Advanced)

| 구분 | 명령어 | 주요 설명 |
|:---:|:---|:---|
| **이미지** | `docker pull [이미지]` | 레지스트리에서 이미지를 로컬로 다운로드 |
| | `docker history [이미지]` | 이미지의 레이어 구성 및 생성 이력 확인 |
| **라이프사이클** | `docker create [이미지]` | 컨테이너 생성 (실행은 하지 않음) |
| | `docker start [이름]` | 생성되었거나 정지된 컨테이너 실행 |
| | `docker pause / unpause` | 실행 중인 컨테이너 일시 정지 및 해제 |
| **상태 확인** | `docker inspect [이름]` | 컨테이너의 IP, 환경변수 등 상세 정보 확인 |
| **명령 실행** | `docker exec -it [이름] [명령]` | 실행 중인 컨테이너 내부에서 명령어 실행 |

> **💡 도커의 상호작용: 왜 `-it`를 쓰나요? (원리와 응용)**
> - **`-i` (Interactive)**: 컨테이너에 내 키보드 입력을 전달함. (**"내 말을 들어라"**)
> - **`-t` (TTY)**: 컨테이너의 출력을 터미널 화면처럼 예쁘게 보여줌. (**"프롬프트를 보여달라"**)
> 
> **어떨 때 사용할까요? (응용 가이드)**
> 1. **대화가 필요할 때 (필수)**: 터미널로 들어가서 이것저것 명령을 계속 입력할 때.
>    - 예: `docker exec -it my-container sh`
> 2. **결과만 필요할 때 (미사용)**: 명령 하나만 던지고 결과만 한 번 확인하고 끝낼 때.
>    - 예: `docker exec my-container ls /tmp` (파일 목록만 쓱 보고 나옴)
> 
> **결론**: 컨테이너와 **"대화를 계속 주고받아야 하는 상황"**이라면 반드시 `-it`를 붙이면 됨!

---

## 2-5. 실습 — 따라하기

### 실습 1 — 이미지 레이어 구조 눈으로 확인

바닥 필름(OS)이 같은 서로 다른 두 이미지를 받아서 레이어 공유 확인.

```bash
# 1. nginx alpine 버전 pull
docker pull nginx:alpine
```

확인 사항:
- 모든 레이어가 `Pull complete` 또는 `Download complete`로 뜸. (첫 다운로드)

```bash
# 2. redis alpine 버전 pull (nginx와 같은 alpine 리눅스 기반)
docker pull redis:alpine
```

확인 사항:
- 출력 중간에 **`Already exists`** 문구 확인!
- **결론**: 이미 내 PC에 있는 똑같은 레이어(OS 바닥 등)는 다시 받지 않고 재사용함. $\rightarrow$ 속도와 용량 절약의 핵심.

> **💡 한끗 차이 팁: 왜 nginx:latest와는 공유가 안 되나요?**
> - `latest` 태그는 보통 **Debian**이나 **Ubuntu** 기반의 무거운 OS 필름을 사용함.
> - `alpine` 태그는 **Alpine Linux**라는 아주 가벼운 OS 필름을 사용함.
> - 뿌리(OS)가 다르면 겹치는 필름이 없으므로 레이어를 공유할 수 없음.

```bash
# 두 이미지 크기 비교
docker images nginx
docker images redis
```

→ 같은 레이어는 중복 저장되지 않음. 디스크 공간 절약의 핵심 원리.

---

### 실습 2 — 컨테이너 라이프사이클 직접 체험

`docker run` 대신 `create → start → stop → rm` 단계별로 실행해서 상태 변화를 직접 확인.

```bash
# 1단계: 컨테이너 생성만 (실행 안 함)
docker create --name lifecycle-test nginx:alpine

# 상태 확인 → Created
docker ps -a
# STATUS: Created
```

```bash
# 2단계: 생성된 컨테이너 시작
docker start lifecycle-test

# 상태 확인 → Up
docker ps
# STATUS: Up X seconds
```

```bash
# 3단계: 일시 정지
docker pause lifecycle-test

# 상태 확인 → Paused
docker ps
# STATUS: Up X seconds (Paused)

# 브라우저나 curl로 접속 시도해도 응답 없음 → 프로세스 멈춤
```

```bash
# 4단계: 일시 정지 해제
docker unpause lifecycle-test

# 상태 확인 → 다시 Up
docker ps
```

```bash
# 5단계: 정지
docker stop lifecycle-test

# 상태 확인 → Exited
docker ps -a
# STATUS: Exited (0)
```

```bash
# 6단계: 삭제
docker rm lifecycle-test

# 확인 → 목록에서 사라짐
docker ps -a
```

→ 컨테이너를 삭제해도 이미지는 남아있음.

```bash
docker images nginx   # 이미지는 그대로 존재
```

---

### 실습 3 — 이미지 하나로 컨테이너 여러 개, 각자 독립

같은 이미지에서 만든 컨테이너 2개가 서로 완전히 독립적임을 확인.

```bash
# 컨테이너 A, B 동시 실행
docker run -d --name container-a nginx:alpine
docker run -d --name container-b nginx:alpine

# 두 컨테이너 모두 실행 중 확인
docker ps
```

```bash
# container-a 내부에서 파일 생성
docker exec container-a sh -c "echo 'hello from A' > /tmp/test.txt"

# container-a에서 파일 확인 → 있음
docker exec container-a cat /tmp/test.txt
# hello from A

# container-b에서 같은 경로 확인 → 없음
docker exec container-b cat /tmp/test.txt
# cat: can't open '/tmp/test.txt': No such file or directory
```

→ 같은 이미지에서 만들어도 컨테이너끼리 파일시스템은 완전히 독립.
→ 한 컨테이너를 수정해도 다른 컨테이너, 원본 이미지에 영향 없음.

```bash
# 정리
docker stop container-a container-b
docker rm container-a container-b
```

---

## 2-6. 응용 실습 문제

> 각 문제는 앞의 따라하기 내용만으로 풀 수 있음.
> 모르면 `docker --help` 또는 `docker [명령어] --help` 참고.

---

### 문제 1 — 이미지 크기 비교 분석

아래 세 이미지를 pull 하고 크기 비교

```
ubuntu:22.04
debian:slim
alpine:3.19
```

조건:
- 세 이미지 모두 pull
- `docker images` 로 크기 비교
- 가장 작은 이미지가 무엇인지 확인
- `docker history [이미지]` 로 레이어 개수도 비교

> 힌트: 크기가 작을수록 보안 취약점도 적고 배포도 빠름. 세션 7 보안 파트와 연결되는 내용.

---

### 문제 2 — 컨테이너 inspect로 정보 뽑기

`nginx:alpine` 컨테이너를 실행하고, `docker inspect` 출력에서 아래 정보 찾기

조건:
- 컨테이너 이름: `inspect-test`
- 포트: `8080:80`
- `docker inspect inspect-test` 출력에서 아래 항목 찾기
  - 컨테이너에 할당된 내부 IP 주소
  - 컨테이너가 사용 중인 이미지 ID
  - 컨테이너 시작 시각

> 힌트: 출력이 길면 `docker inspect inspect-test | findstr "IPAddress"` 로 필터링.

---

### 문제 3 — 컨테이너 종료 후 데이터 확인

컨테이너 안에서 파일을 만들고, 컨테이너 stop → start 시와 rm → run 시의 차이 확인

조건:
1. `nginx:alpine` 으로 컨테이너 `data-test` 실행
2. 컨테이너 내부에 파일 생성: `docker exec data-test sh -c "echo 'hello' > /tmp/myfile.txt"`
3. `docker stop` → `docker start` 후 파일이 남아있는지 확인
4. `docker stop` → `docker rm` → `docker run` 으로 새 컨테이너 실행 후 파일이 있는지 확인

> 힌트: stop/start는 같은 컨테이너를 재사용. rm 후 run은 이미지에서 새 컨테이너 생성.

---

## 정리

- Image = 읽기 전용 레이어 스택, Container = Image의 실행 인스턴스
- 동일 레이어는 여러 이미지가 공유 → 디스크 절약 및 다운로드 속도 향상
- 컨테이너별 파일시스템은 상호 완전 독립 (CoW 방식 활용)
- stop/start = 기존 컨테이너 재사용(데이터 유지) / rm 후 run = 신규 컨테이너 생성(데이터 초기화)
- Registry = 이미지 저장 및 배포를 위한 중앙 허브