# 3교시. systemctl & 서비스 관리
> 백엔드 개발자를 위한 리눅스 실무 | 50분 | 이론 20분 + 실습 30분

---

## 실습 환경

```bash
# systemd가 활성화된 컨테이너 실행 (일반 ubuntu 컨테이너는 systemd 미지원)
docker run -it --privileged --name linux-systemd ubuntu:22.04 /bin/bash


sed -i 's/archive.ubuntu.com/ftp.kaist.ac.kr/g' /etc/apt/sources.list


# 컨테이너 안에서 systemd 설치 및 시작
apt update && apt install -y systemd python3 curl vim

# systemd 초기화 (컨테이너 재시작 후)
/sbin/init &
```

> **Docker에서 systemd 주의사항**
> 일반 Docker 컨테이너는 PID 1이 systemd가 아니므로 `systemctl`이 동작하지 않는다.
> `--privileged` 옵션으로 실행하거나, 실습 환경에 따라 `service` 명령어로 대체한다.
>
> ```bash
> # systemctl 대신 service 명령어로 대체 가능
> service nginx start     ↔  systemctl start nginx
> service nginx status    ↔  systemctl status nginx
> ```

---

## 학습 목표

1. systemctl 명령어로 서비스를 시작/중지/재시작할 수 있다.
2. 백엔드 앱을 systemd 서비스로 등록하고 자동 재시작을 설정할 수 있다.
3. .env 파일을 보안적으로 관리하는 방법을 이해한다.

---

## 모듈 03. systemctl 기본 명령어 (10분)

### 3-1. 서비스 제어

```bash
# 서비스 시작 / 중지 / 재시작
systemctl start nginx
systemctl stop nginx
systemctl restart nginx

# 설정 변경 후 무중단 재로드 (nginx, apache 등 지원)
systemctl reload nginx

# 상태 확인
systemctl status nginx
```

**systemctl status 출력 읽는 법**

```
● nginx.service - A high performance web server
     Loaded: loaded (/lib/systemd/system/nginx.service; enabled)
     Active: active (running) since Mon 2026-04-13 09:00:00 UTC
    Process: 1234 ExecStart=/usr/sbin/nginx
   Main PID: 1234 (nginx)

Apr 13 09:00:01 server nginx[1234]: nginx: starting...
```

| 항목 | 의미 |
|------|------|
| `Active: active (running)` | 정상 실행 중 |
| `Active: inactive (dead)` | 중지됨 |
| `Active: failed` | 오류로 종료됨 |
| `Loaded: enabled` | 부팅 시 자동 시작 설정됨 |
| `Loaded: disabled` | 부팅 시 자동 시작 안 함 |

---

### 3-2. 부팅 자동 시작 설정

```bash
# 부팅 시 자동 시작 등록
systemctl enable nginx

# 자동 시작 해제
systemctl disable nginx

# 활성화 여부 확인
systemctl is-enabled nginx

# 지금 바로 시작 + 부팅 자동 시작 동시에
systemctl enable --now nginx
```

---

### 3-3. 서비스 목록 확인

```bash
# 실행 중인 서비스 목록
systemctl list-units --type=service --state=running

# 실패한 서비스 확인
systemctl list-units --state=failed

# 특정 서비스 로그 확인
journalctl -u nginx
journalctl -u nginx -f          # 실시간 로그
journalctl -u nginx --since "1 hour ago"
journalctl -u nginx -n 50       # 최근 50줄
```

---

## 모듈 04. 백엔드 앱 서비스 등록 (10분)

### 4-1. systemd 서비스 파일 구조

서비스 파일 위치: `/etc/systemd/system/서비스명.service`

```ini
[Unit]
Description=My Backend App
After=network.target         # 네트워크 시작 후 실행

[Service]
Type=simple
User=appuser                 # 실행할 계정 (root 사용 금지)
WorkingDirectory=/opt/myapp
EnvironmentFile=/opt/myapp/.env   # 환경변수 파일
ExecStart=/usr/bin/java -jar /opt/myapp/app.jar
Restart=always               # 앱이 죽으면 자동 재시작
RestartSec=5                 # 5초 후 재시작

[Install]
WantedBy=multi-user.target
```

**Restart 옵션 설명**

| 값 | 의미 |
|----|------|
| `always` | 항상 재시작 (정상 종료도 포함) |
| `on-failure` | 비정상 종료 시만 재시작 |
| `no` | 재시작 안 함 |

---

### 4-2. .env 파일 보안 관리

백엔드 앱의 DB 비밀번호, API 키 등 민감 정보는 `.env` 파일로 분리해서 관리한다.

```bash
# .env 파일 생성
cat > /opt/myapp/.env << 'EOF'
DB_HOST=localhost
DB_PORT=3306
DB_NAME=mydb
DB_USER=appuser
DB_PASSWORD=secret123
API_KEY=abcdef1234567890
EOF

# 소유자를 앱 실행 계정으로 변경
chown appuser:appuser /opt/myapp/.env

# 소유자만 읽기/쓰기 가능 (다른 계정은 접근 불가)
chmod 600 /opt/myapp/.env

# 퍼미션 확인
ls -la /opt/myapp/.env
# -rw------- 1 appuser appuser  ... .env   ← 정상
# -rw-r--r-- 1 ubuntu  ubuntu   ... .env   ← 위험 (다른 계정도 읽을 수 있음)
```

**systemd에서 .env 사용**

```ini
[Service]
EnvironmentFile=/opt/myapp/.env
# 앱에서 $DB_PASSWORD, $API_KEY 등으로 참조 가능
```

---

## 실습 과제 (20분)

> 복잡한 설정 없이, 범용 웹 서버인 `nginx`를 설치하고 `systemctl`로 제어하는 가장 직관적인 과정을 실습해 봅니다.

### Step 1. Nginx 설치

```bash
# apt 패키지 매니저로 nginx 서비스 설치
apt install -y nginx
```

### Step 2. 서비스 상태 확인 및 제어 테스트

```bash
# 1. 초기 상태 확인 (설치와 동시에 실행됨, Active: active (running) 확인)
systemctl status nginx

# 2. 서비스 중지
systemctl stop nginx

# 3. 중지 상태 확인 (Active: inactive (dead) 확인)
systemctl status nginx

# 4. 서비스 다시 시작
systemctl start nginx
```

### Step 3. 부팅 시 자동 시작(enable) 기능 테스트

```bash
# 현재 활성화 상태 확인 (기본적으로 enabled 상태)
systemctl is-enabled nginx

# 부팅 시 시작 비활성화로 변경
systemctl disable nginx
systemctl is-enabled nginx  # disabled 로 바뀐 것 확인

# 부팅 시 작동하도록 다시 활성화로 복구
systemctl enable nginx
```

### Step 4. 웹 서버 실제 동작 확인 (curl 테스트)

```bash
# localhost 호스트명으로 요청을 보내 정상 응답이 돌아오는지 확인
curl http://localhost
```
*(성공했다면 쭈욱 나오는 HTML 태그 안에 `Welcome to nginx!` 문구가 포함되어 있습니다.)*

---

## [심화] 커스텀 서비스 수동 등록 (직접 만든 앱 올리기)

> `nginx`처럼 `apt` 패키지로 설치하는 프로그램은 `.service` 단위 파일이 자동으로 셋팅되지만, 우리가 직접 만든(Spring Boot, Node.js, Python 등) 백엔드 서버 앱은 수동으로 `systemd`에 등록해야 합니다.

### 1단계. .service 설정 파일 생성
systemd가 바라보는 폴더(`/etc/systemd/system/`)에 에디터로 파일을 생성합니다.

```bash
vim /etc/systemd/system/mybackend.service
```

### 2단계. 실행 스크립트 작성
어떤 프로그램을, 누구의 권한으로, 어떻게 실행시킬지 명시합니다.

```ini
[Unit]
Description=My Custom Backend App      # 서비스 설명
After=network.target                   # 네트워크 구동이 끝난 후에 실행

[Service]
Type=simple
User=ubuntu                            # 실행 계정 (보안상 root 지양)
WorkingDirectory=/home/ubuntu/myapp    # 실행할 폴더 위치
ExecStart=/usr/bin/java -jar /home/ubuntu/myapp/app.jar  # 구동 명령어 (절대경로)
Restart=always                         # 종료/장애 시 묻지도 따지지도 않고 재시작
RestartSec=3                           # 3초 대기 후 재시작

[Install]
WantedBy=multi-user.target             # 시스템 기본 부팅 모드
```

### 3단계. 시스템 데몬 재로드 및 등록 적용
직접 설정 파일을 만들거나 수정했다면, 반드시 `daemon-reload` 명령어로 갱신해 주어야 합니다.

```bash
# 1. systemctl 서비스 목록 새로고침 (필수!)
systemctl daemon-reload

# 2. 직접 만든 서비스 시작 & 부팅 자동시작 켜기
systemctl start mybackend
systemctl enable mybackend

# 3. 최종 구동 상태 확인
systemctl status mybackend
```
*(이제 여러분이 짠 코드도 `nginx` 서버처럼 불시의 장애에도 알아서 다시 켜지는 든든한 백그라운드 서비스가 됩니다!)*

---

## 핵심 정리

| 상황 | 명령어 |
|------|--------|
| 서비스 시작/중지/재시작 | `systemctl start/stop/restart 서비스명` |
| 서비스 상태 확인 | `systemctl status 서비스명` |
| 부팅 자동 시작 등록 | `systemctl enable 서비스명` |
| 서비스 로그 확인 | `journalctl -u 서비스명 -f` |
| 새 서비스 파일 적용 | `systemctl daemon-reload` |
| .env 파일 권한 | `chmod 600 .env` (소유자만 읽기/쓰기) |
| 앱 실행 계정 원칙 | root 대신 전용 계정(`appuser`) 사용 |
| Docker 대체 명령어 | `service 서비스명 start/stop/status` |