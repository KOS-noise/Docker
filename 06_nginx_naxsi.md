# 6교시. nginx naxsi — WAF 설정
> 백엔드 개발자를 위한 리눅스 실무 | 50분 | 이론 20분 + 실습 30분

---

## 실습 환경

> **naxsi는 nginx를 소스 컴파일할 때 모듈로 포함해야 한다.**
> 실습은 두 가지 방법 중 선택한다.

**방법 A. naxsi 포함 nginx 직접 빌드 (권장)**

```bash
docker run -it --name naxsi-lab \
  -p 80:80 -p 8080:8080 \
  ubuntu:22.04 /bin/bash

apt update && apt install -y build-essential libpcre3-dev zlib1g-dev \
  libssl-dev libgd-dev git curl python3 vim

# nginx + naxsi 소스 다운로드
cd /tmp
NGINX_VERSION=1.24.0
wget http://nginx.org/download/nginx-${NGINX_VERSION}.tar.gz
tar xzf nginx-${NGINX_VERSION}.tar.gz

git clone https://github.com/nbs-system/naxsi.git

# 빌드
cd nginx-${NGINX_VERSION}
./configure \
  --prefix=/etc/nginx \
  --sbin-path=/usr/sbin/nginx \
  --modules-path=/usr/lib/nginx/modules \
  --conf-path=/etc/nginx/nginx.conf \
  --error-log-path=/var/log/nginx/error.log \
  --http-log-path=/var/log/nginx/access.log \
  --pid-path=/var/run/nginx.pid \
  --add-module=../naxsi/naxsi_src

make && make install
```

**방법 B. 개념 학습 + ModSecurity로 대체 실습**

```bash
# ModSecurity는 apt로 설치 가능한 대안 WAF
apt install -y nginx libnginx-mod-security2
```

---

## 학습 목표

1. WAF(Web Application Firewall)의 개념과 역할을 설명할 수 있다.
2. naxsi의 동작 방식과 규칙 구조를 이해한다.
3. LearningMode로 오탐을 분석하고 WhiteList 규칙을 작성할 수 있다.
4. 보안 헤더를 nginx에 적용할 수 있다.

---

## 모듈 06. WAF 개념 (5분)

### 6-1. WAF란

```
일반 방화벽     : IP / 포트 수준 차단
WAF (Layer 7)  : HTTP 요청 내용(URL, 파라미터, 헤더, 바디)을 검사해서 차단
```

**WAF가 막는 주요 공격**

| 공격 유형 | 예시 |
|-----------|------|
| SQL Injection | `' OR 1=1 --`, `UNION SELECT` |
| XSS | `<script>alert(1)</script>` |
| Path Traversal | `../../etc/passwd` |
| Command Injection | `; rm -rf /` |
| File Inclusion | `?file=http://evil.com/shell.php` |

---

### 6-2. naxsi 동작 방식

```
클라이언트 요청
    ↓
naxsi 규칙 매칭 (점수 계산)
    ↓
점수가 임계값 초과?
    ├── YES → 차단 (403 응답) 또는 LearningMode에서 로그만 기록
    └── NO  → 백엔드 앱으로 전달
```

**두 가지 모드**

| 모드 | 동작 | 용도 |
|------|------|------|
| `LearningMode` | 차단 안 하고 로그만 기록 | 초기 도입 시 오탐 분석 |
| 차단 모드 | 규칙 위반 시 403 반환 | 실제 운영 |

---

## 모듈 07. naxsi 설정 (10분)

### 7-1. 핵심 규칙 파일 구조

```
/etc/nginx/
├── naxsi_core.rules     ← naxsi 기본 규칙 (수정하지 않음)
└── sites-available/
    └── myapp            ← 서비스별 설정 (WhiteList 등 추가)
```

**naxsi_core.rules 예시 (일부)**

```nginx
# SQL Injection 관련 문자 탐지
MainRule "str:select" "msg:sql keyword select" "mz:ARGS|BODY" "s:$SQL:4" id:1000;
MainRule "str:insert" "msg:sql keyword insert" "mz:ARGS|BODY" "s:$SQL:4" id:1001;
MainRule "str:union"  "msg:sql keyword union"  "mz:ARGS|BODY" "s:$SQL:8" id:1002;

# XSS 관련 문자 탐지
MainRule "str:<script" "msg:xss tag script"    "mz:ARGS|BODY" "s:$XSS:8" id:1100;
MainRule "str:javascript" "msg:xss javascript" "mz:ARGS|BODY" "s:$XSS:4" id:1101;
```

**규칙 구성 설명**

| 항목 | 의미 |
|------|------|
| `str:select` | 문자열 "select" 포함 시 매칭 |
| `mz:ARGS` | URL 파라미터에서 검사 |
| `mz:BODY` | 요청 바디에서 검사 |
| `s:$SQL:4` | SQL 점수 +4 추가 |
| `id:1000` | 규칙 ID |

---

### 7-2. 서비스 설정 파일

```nginx
# /etc/nginx/sites-available/myapp

server {
    listen 80;
    server_name _;

    # naxsi 핵심 규칙 로드
    include /etc/nginx/naxsi_core.rules;

    location / {
        # LearningMode: 차단 없이 로그만 기록 (도입 초기 사용)
        LearningMode;

        # 임계값 설정 (SQL 점수 8점 이상이면 위반)
        SecRulesEnabled;
        DeniedUrl "/request_denied";

        CheckRule "$SQL >= 8" BLOCK;
        CheckRule "$XSS >= 8" BLOCK;

        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 차단 시 응답 처리
    location /request_denied {
        return 403 "요청이 차단되었습니다.";
    }
}
```

---

### 7-3. WhiteList — 오탐 예외 처리

정상적인 요청이 차단되는 경우 **WhiteList**로 예외 처리한다.

```nginx
# 특정 URL 경로 전체 예외
BasicRule wl:1000,1001 "mz:URL|/api/search";

# 특정 파라미터 예외 (content 파라미터는 HTML 허용)
BasicRule wl:1100,1101 "mz:BODY|content";

# 특정 헤더 예외
BasicRule wl:1000 "mz:$HEADERS_VAR:authorization";
```

---

### 7-4. LearningMode 로그 분석

```bash
# naxsi가 탐지한 요청 로그 확인
grep "NAXSI_FMT" /var/log/nginx/error.log

# 로그 예시:
# NAXSI_FMT: ip=1.2.3.4&server=myapp&uri=/api/search&learning=1
#            &vers=0.55&total_processed=5&total_blocked=1
#            &block=0&cscore0=$SQL&score0=8&zone0=ARGS&id0=1000
#            &var_name0=keyword
```

**로그 분석 포인트**

| 항목 | 의미 |
|------|------|
| `uri` | 어떤 경로에서 탐지됐는지 |
| `id0` | 어떤 규칙에 매칭됐는지 |
| `zone0` | ARGS / BODY / HEADERS 어디서 탐지됐는지 |
| `var_name0` | 어떤 파라미터가 탐지됐는지 |
| `score0` | 누적 점수 |

---

## 모듈 08. 보안 헤더 (5분)

WAF와 함께 **HTTP 보안 헤더**를 설정하면 클라이언트 수준 공격도 방어할 수 있다.

```nginx
server {
    # ...

    # 클릭재킹 방지
    add_header X-Frame-Options "DENY" always;

    # MIME 스니핑 방지
    add_header X-Content-Type-Options "nosniff" always;

    # XSS 필터 활성화 (구형 브라우저용)
    add_header X-XSS-Protection "1; mode=block" always;

    # HTTPS 강제 (HSTS) - 1년간 HTTPS만 허용
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 레퍼러 정보 제한
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 허용할 리소스 출처 제한 (CSP)
    add_header Content-Security-Policy "default-src 'self'" always;
}
```

**보안 헤더 설정 확인**

```bash
curl -I http://localhost/ | grep -E "X-Frame|X-Content|Strict|X-XSS"
```

---

## 실습 과제 (30분)

> naxsi 빌드가 어려운 경우, 보안 헤더 설정 + ModSecurity 기본 설정 실습으로 대체한다.

### 공통 실습 A. 보안 헤더 설정 및 검증

```bash
# 현재 nginx 설정에 보안 헤더 추가
cat >> /etc/nginx/sites-available/myapp << 'EOF'

    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
EOF

nginx -t && service nginx reload

# 헤더 확인
curl -I http://localhost/
```

### 공통 실습 B. 의심스러운 요청 시뮬레이션

```bash
# SQL Injection 시도 흉내
curl "http://localhost/api/search?q=select+*+from+users"

# XSS 시도 흉내
curl "http://localhost/api/comment?text=<script>alert(1)</script>"

# Path Traversal 시도 흉내
curl "http://localhost/../../etc/passwd"

# nginx 에러 로그 확인
tail -20 /var/log/nginx/error.log
tail -20 /var/log/nginx/access.log
```

### naxsi 설치 성공 시 실습 C. LearningMode 분석

```bash
# LearningMode에서 요청 발생 후 로그 분석
grep "NAXSI_FMT" /var/log/nginx/error.log

# 탐지된 규칙 ID 목록 추출
grep "NAXSI_FMT" /var/log/nginx/error.log \
  | grep -oP 'id\d+=\K[0-9]+' | sort | uniq -c | sort -rn
```

### naxsi 설치 성공 시 실습 D. WhiteList 추가 후 차단 모드 전환

```bash
# LearningMode 제거 후 실제 차단 활성화
# /etc/nginx/sites-available/myapp 에서 LearningMode 줄 삭제/주석 처리

vim /etc/nginx/sites-available/myapp
# LearningMode;  ← 이 줄 주석 처리 (#LearningMode;)

nginx -t && service nginx reload

# 동일한 의심 요청 재시도 → 403 응답 확인
curl "http://localhost/api/search?q=select+*+from+users"
# < HTTP/1.1 403 Forbidden
```

---

## 핵심 정리

| 항목 | 내용 |
|------|------|
| WAF 역할 | HTTP 요청 내용(파라미터/바디)까지 검사해서 공격 차단 |
| naxsi 모드 | `LearningMode` (로그만) → 분석 → 차단 모드 순서로 도입 |
| 오탐 처리 | `BasicRule wl:규칙ID "mz:위치"` 로 WhiteList 등록 |
| 로그 위치 | `/var/log/nginx/error.log` (`NAXSI_FMT` 키워드로 검색) |
| X-Frame-Options | `DENY` — 클릭재킹 방지 |
| X-Content-Type-Options | `nosniff` — MIME 스니핑 방지 |
| HSTS | `Strict-Transport-Security` — HTTPS 강제 |
| 도입 순서 | LearningMode → 로그 분석 → WhiteList 작성 → 차단 모드 전환 |