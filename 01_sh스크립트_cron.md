# 1교시. sh 스크립트 + cron 자동화
> 백엔드 개발자를 위한 리눅스 실무 | 70분 | 이론 45분 + 실습 25분

---

## 실습 환경

```bash
# Windows에서 Docker Ubuntu 컨테이너 접속
docker run -it --name linux-lab ubuntu:22.04 /bin/bash

# 컨테이너 재접속 시
docker start linux-lab && docker exec -it linux-lab /bin/bash

# 필수 패키지 설치 (최초 1회)
apt update && apt install -y cron vim

apt install -y locales
locale-gen ko_KR.UTF-8
update-locale LANG=ko_KR.UTF-8
```



## Part 1. sh 스크립트 기초 문법

### 1-1. 스크립트 파일 만들기 ⭐

**shebang** — 첫 줄에 반드시 작성, 이 파일을 어떤 인터프리터로 실행할지 선언

```bash
#!/bin/bash
```

```bash
# 파일 작성
vim hello.sh

# 실행 권한 부여
chmod +x hello.sh

# 실행 방법 3가지
./hello.sh           # 현재 디렉토리에서 직접 실행
bash hello.sh        # bash 명령어로 실행 (권한 없어도 됨)
/bin/bash hello.sh   # 절대 경로로 실행
```

> `./hello.sh` 실행 시 `Permission denied` 가 나오면 `chmod +x` 를 안 한 것이다.

---

### 1-2. vim 기본 사용법 ⭐

vim은 터미널에서 파일을 편집할 때 사용하는 에디터.  
**모드** 개념이 핵심이다 — 열자마자 타이핑이 안 되는 게 정상이다.

#### 모드 전환

```
┌─────────────────────────────────────────┐
│            Normal Mode (기본)            │
│  파일을 열면 항상 이 모드로 시작         │
│  키보드 입력 = 명령어 (타이핑 안 됨)     │
└────────────┬──────────┬─────────────────┘
             │          │
           i, a, o      ESC
             │          │
┌────────────▼──────────┘
│         Insert Mode                     │
│  실제로 글자를 입력하는 모드             │
│  화면 하단에 -- INSERT -- 표시됨         │
└─────────────────────────────────────────┘
```

| 키 | Normal → Insert | 설명 |
|----|-----------------|------|
| `i` | 커서 앞에서 입력 시작 | 가장 많이 씀 |
| `a` | 커서 뒤에서 입력 시작 | |
| `o` | 현재 줄 아래 새 줄 추가 후 입력 | |
| `ESC` | Insert → Normal 복귀 | |

---

#### 저장 & 종료 ⭐

Normal 모드에서 `:` 를 누르면 하단에 명령어 입력창이 열린다.

| 명령어 | 동작 |
|--------|------|
| `:w` | 저장 (종료 안 함) |
| `:q` | 종료 (변경사항 없을 때) |
| `:wq` | 저장 후 종료 |
| `:wq!` | 강제 저장 후 종료 |
| `:q!` | 저장 안 하고 강제 종료 (수정 내용 버림) |
| `:x` | `:wq` 와 동일 |

> 가장 많이 쓰는 것: **`:wq`** (저장+종료) / **`:q!`** (저장 없이 그냥 나가기)

---

#### 기본 이동 (Normal 모드)

| 키 | 동작 |
|----|------|
| `h` `j` `k` `l` | ← ↓ ↑ → (방향키 대신) |
| `gg` | 파일 맨 위로 |
| `G` | 파일 맨 아래로 |
| `0` | 줄 맨 앞으로 |
| `$` | 줄 맨 끝으로 |

---

#### 편집 (Normal 모드)

| 키 | 동작 |
|----|------|
| `dd` | 현재 줄 삭제 (잘라내기) |
| `yy` | 현재 줄 복사 |
| `p` | 붙여넣기 |
| `u` | 실행 취소 (undo) |
| `Ctrl + r` | 다시 실행 (redo) |
| `/검색어` | 아래 방향으로 검색 |
| `?검색어` | 위 방향으로 검색 |
| `n` | 다음 검색 결과로 이동 |
| `N` | 이전 검색 결과로 이동 |

**검색 예시**

```
/LOG_DIR   →  Enter   첫 번째 결과로 이동
n                     다음 결과
N                     이전 결과
```

> 검색 후 하이라이트를 끄려면 `:noh` 입력

---

#### vim으로 파일 열기/저장 흐름

```bash
vim hello.sh        # 파일 열기 (없으면 새로 생성)
```

```
1. vim 실행 → Normal 모드로 시작
2. i 누르기 → Insert 모드 진입 (-- INSERT -- 표시)
3. 내용 입력
4. ESC 누르기 → Normal 모드 복귀
5. :wq 입력 후 Enter → 저장 & 종료
```

---

### 1-3. 출력 — echo & printf


#### echo ⭐

```bash
#!/bin/bash

echo "Hello, World!"          # 기본 출력 (자동 줄바꿈)
echo '작은따옴표도 가능'        # 작은따옴표: 변수 해석 안 함
echo ""                        # 빈 줄 출력
echo -n "줄바꿈 없이 출력"     # -n: 줄바꿈 제거
echo -e "줄\n바꿈\t탭"         # -e: 이스케이프 문자 해석
```

**큰따옴표 vs 작은따옴표**

```bash
NAME="홍길동"

echo "$NAME 님 안녕하세요"    # 출력: 홍길동 님 안녕하세요
echo '$NAME 님 안녕하세요'    # 출력: $NAME 님 안녕하세요  ← 변수 해석 안 됨
```

#### printf — 형식 지정 출력

```bash
#!/bin/bash

# printf는 형식(format)을 지정할 수 있어서 로그에 유용
printf "이름: %s\n" "홍길동"
printf "나이: %d\n" 30
printf "점수: %.2f\n" 98.5678    # 소수점 2자리

# 정렬
printf "%-10s %5d\n" "apple"  100   # 왼쪽 정렬 10자 / 오른쪽 정렬 5자
printf "%-10s %5d\n" "banana" 50

# 출력:
# apple       100
# banana       50
```

---

### 1-4. 변수 ⭐

#### 변수 선언과 사용

```bash
#!/bin/bash

# 선언: = 양쪽에 공백 없음 (공백 있으면 에러)
NAME="홍길동"         # 문자열
AGE=30               # 숫자도 따옴표 없이 선언 가능
IS_ADMIN=true        # 불리언 (bash에는 진짜 boolean 없음, 문자열로 처리)

# 사용: $ 붙이기
echo $NAME
echo $AGE

# 중괄호: 변수 이름 경계를 명확히 할 때
echo "${NAME}님의 나이는 ${AGE}살입니다."

# 잘못된 예 (중괄호 없으면 변수명이 이어져서 오인식)
echo "$NAMEtest"     # $NAMEtest 를 변수로 인식 → 빈값
echo "${NAME}test"   # 홍길동test  ← 올바른 방식
```

#### 명령어 실행 결과를 변수에 저장 ⭐

```bash
#!/bin/bash

# $() 형식 권장 (가독성 좋음)
TODAY=$(date +%Y-%m-%d)
HOSTNAME=$(hostname)
FILE_COUNT=$(ls /var/log | wc -l)

echo "오늘 날짜: $TODAY"
echo "서버 이름: $HOSTNAME"
echo "로그 파일 수: $FILE_COUNT"

# 백틱 형식 (구형 방식, 중첩 사용이 어려움)
TODAY=`date +%Y-%m-%d`
```

#### 변수 기본값 설정

```bash
#!/bin/bash

# 변수가 비어 있으면 기본값 사용
ENV=${APP_ENV:-"development"}
PORT=${APP_PORT:-8080}

echo "환경: $ENV"
echo "포트: $PORT"

# APP_ENV 가 설정된 경우: 그 값 사용
# APP_ENV 가 없는 경우: "development" 사용
```

#### readonly — 상수 선언

```bash
#!/bin/bash

readonly MAX_RETRY=3
readonly LOG_DIR="/var/log/myapp"

echo "최대 재시도: $MAX_RETRY"

MAX_RETRY=5   # 에러 발생: readonly variable
```

#### unset — 변수 삭제

```bash
NAME="홍길동"
echo $NAME     # 홍길동

unset NAME
echo $NAME     # (빈값)
```

---

### 1-5. 특수 변수

스크립트 실행 시 자동으로 설정되는 변수들.

```bash
#!/bin/bash

echo "스크립트 파일명: $0"
echo "첫 번째 인수:    $1"
echo "두 번째 인수:    $2"
echo "전체 인수 목록:  $@"
echo "인수 개수:       $#"
echo "이전 명령 종료코드: $?"
echo "현재 프로세스 PID:  $$"
```

```bash
# 실행 예시
./myscript.sh hello world

# 출력:
# 스크립트 파일명: ./myscript.sh
# 첫 번째 인수:    hello
# 두 번째 인수:    world
# 전체 인수 목록:  hello world
# 인수 개수:       2
```

---

### 1-6. 연산

#### 정수 연산

```bash
#!/bin/bash

A=10
B=3

# $(( )) 산술 연산
echo $((A + B))    # 13
echo $((A - B))    # 7
echo $((A * B))    # 30
echo $((A / B))    # 3  (정수 나눗셈)
echo $((A % B))    # 1  (나머지)
echo $((A ** B))   # 1000 (거듭제곱)

# 변수에 연산 결과 저장
RESULT=$((A * B + 5))
echo $RESULT   # 35

# 증감
COUNT=0
COUNT=$((COUNT + 1))   # 증가
COUNT=$((COUNT - 1))   # 감소
((COUNT++))            # 단축 표현
((COUNT--))
```

#### 문자열 연산

```bash
#!/bin/bash

STR="Hello, World!"

# 문자열 길이
echo ${#STR}            # 13

# 부분 문자열: ${변수:시작:길이}
echo ${STR:0:5}         # Hello
echo ${STR:7:5}         # World

# 문자열 치환: ${변수/찾을것/바꿀것}
echo ${STR/World/Linux} # Hello, Linux!

# 대소문자 변환
LOWER=${STR,,}          # 전체 소문자
UPPER=${STR^^}          # 전체 대문자
echo $LOWER             # hello, world!
echo $UPPER             # HELLO, WORLD!

# 앞/뒤 문자열 제거
FILE="backup_2026-04-16.tar.gz"
echo ${FILE%.tar.gz}    # backup_2026-04-16  (뒤에서 .tar.gz 제거)
echo ${FILE#backup_}    # 2026-04-16.tar.gz  (앞에서 backup_ 제거)
```

---

### 1-7. 사용자 입력 — read

```bash
#!/bin/bash

# 기본 입력
echo -n "이름을 입력하세요: "
read NAME
echo "안녕하세요, $NAME 님!"

# -p 옵션으로 프롬프트 한 줄에
read -p "나이를 입력하세요: " AGE
echo "나이: $AGE"

# -s 옵션: 입력 내용 숨김 (비밀번호)
read -s -p "비밀번호: " PASSWORD
echo ""   # 줄바꿈
echo "입력 완료"

# -t 옵션: 타임아웃 (초)
read -t 5 -p "5초 안에 입력하세요: " INPUT
if [ $? -ne 0 ]; then
    echo "시간 초과"
fi
```

---

### 1-8. 조건문 ⭐

#### if / elif / else

```bash
#!/bin/bash

SCORE=85

if [ $SCORE -ge 90 ]; then
    echo "A"
elif [ $SCORE -ge 80 ]; then
    echo "B"
elif [ $SCORE -ge 70 ]; then
    echo "C"
else
    echo "F"
fi
```

#### 자주 쓰는 조건 표현

**숫자 비교**

| 표현 | 의미 | 예시 |
|------|------|------|
| `-eq` | 같다 (equal) | `[ $A -eq $B ]` |
| `-ne` | 다르다 (not equal) | `[ $A -ne $B ]` |
| `-gt` | 크다 (greater than) | `[ $A -gt 10 ]` |
| `-lt` | 작다 (less than) | `[ $A -lt 10 ]` |
| `-ge` | 크거나 같다 | `[ $A -ge 10 ]` |
| `-le` | 작거나 같다 | `[ $A -le 10 ]` |

**문자열 비교**

| 표현 | 의미 |
|------|------|
| `[ "$A" == "$B" ]` | 문자열이 같다 |
| `[ "$A" != "$B" ]` | 문자열이 다르다 |
| `[ -z "$A" ]` | 변수가 비어 있다 ⭐ |
| `[ -n "$A" ]` | 변수에 값이 있다 |

**파일/디렉토리 확인** ⭐

| 표현 | 의미 |
|------|------|
| `[ -f 파일 ]` | 파일이 존재함 |
| `[ -d 경로 ]` | 디렉토리가 존재함 |
| `[ ! -f 파일 ]` | 파일이 존재하지 않음 |
| `[ -r 파일 ]` | 읽기 권한 있음 |
| `[ -w 파일 ]` | 쓰기 권한 있음 |
| `[ -x 파일 ]` | 실행 권한 있음 |
| `[ -s 파일 ]` | 파일이 존재하고 크기가 0이 아님 |

**조건 조합**

```bash
# AND: 두 조건 모두 참
if [ -f "$FILE" ] && [ -r "$FILE" ]; then
    echo "파일 존재하고 읽기 가능"
fi

# OR: 하나라도 참
if [ "$ENV" == "dev" ] || [ "$ENV" == "test" ]; then
    echo "개발/테스트 환경"
fi
```

#### case — 다중 분기

```bash
#!/bin/bash

read -p "명령어 입력 (start/stop/status): " CMD

case $CMD in
    start)
        echo "서비스 시작"
        ;;
    stop)
        echo "서비스 중지"
        ;;
    restart)
        echo "서비스 재시작"
        ;;
    status)
        echo "서비스 상태 확인"
        ;;
    *)
        echo "알 수 없는 명령어: $CMD"
        exit 1
        ;;
esac
```

---

### 1-9. 반복문 ⭐

#### for 문

```bash
#!/bin/bash

# 목록 반복
for FRUIT in apple banana cherry; do
    echo "과일: $FRUIT"
done

# 숫자 범위
for i in {1..5}; do
    echo "$i 번째"
done

# 증감 지정: {시작..끝..증감}
for i in {0..10..2}; do   # 0, 2, 4, 6, 8, 10
    echo $i
done

# C 스타일
for ((i=0; i<5; i++)); do
    echo $i
done

# 파일 목록 순회 ⭐
for FILE in /var/log/myapp/*.log; do
    echo "파일 크기: $(du -sh $FILE)"
done

# 배열 순회
SERVERS=("web01" "web02" "db01")
for SERVER in "${SERVERS[@]}"; do
    echo "서버: $SERVER"
done
```

#### while 문

```bash
#!/bin/bash

# 조건이 참인 동안 반복
COUNT=1
while [ $COUNT -le 5 ]; do
    echo "$COUNT 번째 실행"
    COUNT=$((COUNT + 1))
done

# 파일을 한 줄씩 읽기 (로그 처리에 유용)
while IFS= read -r LINE; do
    echo "줄: $LINE"
done < /var/log/myapp/app.log

# 무한 루프 (서비스 모니터링 등)
while true; do
    echo "헬스체크: $(date)"
    sleep 60
done
```

#### break / continue

```bash
#!/bin/bash

for i in {1..10}; do
    if [ $i -eq 5 ]; then
        echo "5에서 중단"
        break        # 루프 완전 종료
    fi
    echo $i
done

echo "---"

for i in {1..10}; do
    if [ $((i % 2)) -eq 0 ]; then
        continue     # 짝수는 건너뛰기
    fi
    echo $i          # 홀수만 출력
done
```

---

### 1-10. 함수 

```bash
#!/bin/bash

# 함수 정의
greet() {
    echo "안녕하세요, $1 님!"   # $1: 함수에 전달된 첫 번째 인수
}

# 함수 호출
greet "홍길동"
greet "김철수"

# 반환값: return은 종료 코드(0~255)만 반환
# 문자열 반환은 echo로 출력 후 $() 로 캡처
get_date() {
    echo $(date +%Y-%m-%d)
}

TODAY=$(get_date)
echo "오늘: $TODAY"

# 실용 예시: 타임스탬프 로그 함수 
log() {
    local LEVEL=$1    # local: 함수 내부 변수 (함수 밖에서 접근 불가)
    local MSG=$2
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$LEVEL] $MSG"
}

log "INFO"  "서버 시작됨"
log "WARN"  "디스크 사용량 80% 초과"
log "ERROR" "DB 연결 실패"
```

---

### 1-11. 종료 코드

```bash
#!/bin/bash

# 명령어 실행 후 종료 코드 확인
ls /tmp
echo "종료 코드: $?"    # 0 = 성공

ls /존재하지않는경로
echo "종료 코드: $?"    # 0이 아님 = 실패

# 스크립트에서 명시적 종료
exit 0    # 정상 종료
exit 1    # 에러 종료 (관례: 1 이상은 에러)

# 실용 패턴: 명령어 실패 시 스크립트 중단
cp /src/app.jar /opt/myapp/ || exit 1

# 스크립트 상단에 추가 — 명령어 실패 시 자동 종료 (배포/정리 스크립트에 권장)
set -e
set -u    # 선언되지 않은 변수 사용 시 에러
```

---

### 1-12. 리다이렉션 & 파이프 ⭐

```bash
#!/bin/bash

# 출력을 파일로 저장
echo "로그 메시지" > /tmp/output.txt      # 덮어쓰기
echo "추가 메시지" >> /tmp/output.txt     # 이어쓰기 (append) ⭐

# 에러 출력 분리
ls /없는경로 2>/tmp/error.txt             # 에러만 파일로
ls /없는경로 > /tmp/out.txt 2>&1          # 정상 + 에러 모두 파일로
ls /없는경로 > /dev/null 2>&1            # 출력 완전 무시

# 파이프: 앞 명령어 출력을 다음 명령어 입력으로
ps aux | grep java                        # java 프로세스 필터
cat /var/log/nginx/access.log | grep 500 | wc -l   # 500 에러 개수
```

---

### 1-13. 실용 패턴 — 디스크 용량 확인 ⭐

백엔드 서버에서 **로그가 쌓여 디스크가 꽉 차는** 상황은 흔히 발생한다.

```bash
# 전체 디스크 사용량 확인
df -h

# 특정 디렉토리 용량 확인
du -sh /var/log/*

# 용량 큰 순으로 정렬
du -sh /var/log/* | sort -rh | head -10
```

```
df -h 출력 예시:
Filesystem      Size  Used Avail Use% Mounted on
/dev/sda1        50G   38G   12G  76% /
tmpfs           2.0G     0  2.0G   0% /dev/shm
```

| 명령어 | 의미 |
|--------|------|
| `df -h` | 마운트된 전체 파티션 용량 (human-readable) |
| `du -sh 경로` | 특정 경로 총 용량 요약 |
| `du -sh 경로/*` | 하위 항목별 용량 |

---

## Part 2. cron 자동화 ⭐

### 2-1. crontab 기본 문법

```
*  *  *  *  *  실행할_명령어
│  │  │  │  │
│  │  │  │  └── 요일 (0=일요일 ~ 6=토요일)
│  │  │  └───── 월 (1~12)
│  │  └──────── 일 (1~31)
│  └─────────── 시 (0~23)
└────────────── 분 (0~59)
```

**예시**

```
# 매일 새벽 2시에 실행
0 2 * * * /home/ubuntu/cleanup.sh

# 매주 월요일 오전 9시에 실행
0 9 * * 1 /home/ubuntu/weekly_report.sh

# 10분마다 실행
*/10 * * * * /home/ubuntu/health_check.sh

# 매시 0분, 30분에 실행
0,30 * * * * /home/ubuntu/sync.sh

# 평일(월~금) 오전 9시~18시 매 시간 실행
0 9-18 * * 1-5 /home/ubuntu/check.sh
```

### 2-2. crontab 관리 명령어

```bash
crontab -e      # cron 작업 편집 (등록/수정)
crontab -l      # 등록된 cron 목록 조회
crontab -r      # 전체 cron 삭제 (주의)
```

### 2-3. cron 실행 확인

```bash
# cron 서비스 시작 (Docker 환경에서는 수동 시작 필요)
service cron start
service cron status

# cron 실행 로그 확인
tail -f /var/log/syslog | grep CRON
```

> **Docker 주의사항**: Docker 컨테이너는 기본적으로 cron 데몬이 꺼져 있다.
> `service cron start` 로 먼저 시작해야 등록한 cron이 동작한다.

### 2-4. cron 스크립트 작성 시 주의사항 ⭐

```bash
# cron은 환경변수가 거의 없는 최소 환경에서 실행된다
# PATH가 다르기 때문에 명령어를 절대 경로로 작성해야 안전

# 나쁜 예 (cron에서 실패할 수 있음)
* * * * * cleanup.sh

# 좋은 예 (절대 경로 사용)
* * * * * /home/ubuntu/cleanup.sh

# 실행 결과를 로그로 남기기
0 2 * * * /home/ubuntu/cleanup.sh >> /var/log/cron_cleanup.log 2>&1
```



### 실습 1. cron으로 로그 파일 자동 생성

#### Step 1. 로그 생성 스크립트 작성

```bash
mkdir -p /var/log/myapp

vim /generate_log.sh
```

```bash
#!/bin/bash

LOG_DIR="/var/log/myapp"
FILENAME="app_$(date +%Y%m%d_%H%M%S).log"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] INFO  Server running OK" > $LOG_DIR/$FILENAME
echo "[$(date '+%Y-%m-%d %H:%M:%S')] DEBUG Request processed"  >> $LOG_DIR/$FILENAME
```

```bash
chmod +x ./generate_log.sh

# 직접 실행해서 동작 확인
generate_log.sh
ls /var/log/myapp/
cat /var/log/myapp/*.log
```

#### Step 2. cron에 등록 (1분마다 실행)

```bash
service cron start

crontab -e
```

아래 내용 추가 후 저장:

```
* * * * * /generate_log.sh
```

```bash
crontab -l   # 등록 확인
```

#### Step 3. 파일이 쌓이는지 확인

```bash
# 10초마다 화면 갱신 — 파일이 1분마다 추가되는 것 확인
watch -n 10 'ls -la /var/log/myapp/'
# Ctrl+C 로 종료
```

---

### 실습 2. cleanup_log.sh 작성 및 실행

#### Step 1. 오래된 로그 파일 추가 생성

```bash
# 실제 내용이 있는 오래된 로그 파일 생성
echo "[2026-04-06 10:23:11] ERROR DB connection failed"  > /var/log/myapp/app_20260406_020000.log
echo "[2026-04-08 09:11:44] INFO  Server started"        > /var/log/myapp/app_20260408_020000.log

# 날짜를 오래된 것처럼 조작
touch -d "10 days ago" /var/log/myapp/app_20260406_020000.log
touch -d "8 days ago"  /var/log/myapp/app_20260408_020000.log

# 파일 상태 확인 (날짜 포함)
ls -la /var/log/myapp/
```

#### Step 2. cleanup_log.sh 작성

```bash
vim /home/ubuntu/cleanup_log.sh
```

```bash
#!/bin/bash
set -e

# ── 설정 ────────────────────────────────────
LOG_DIR="/var/log/myapp"
MAX_DAYS=7
HISTORY="/var/log/cleanup_history.log"

# ── 함수 ────────────────────────────────────
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> $HISTORY
}

# ── 실행 ────────────────────────────────────
log "=== 정리 시작 ==="

# 디렉토리 확인
if [ ! -d "$LOG_DIR" ]; then
    log "ERROR: 디렉토리 없음 $LOG_DIR"
    exit 1
fi

# 정리 전 용량
BEFORE=$(du -sh $LOG_DIR | cut -f1)
log "정리 전 용량: $BEFORE"

# 7일 이상 된 파일 탐색
DELETED=$(find $LOG_DIR -type f -name "*.log" -mtime +$MAX_DAYS)

if [ -z "$DELETED" ]; then
    log "삭제할 파일 없음"
else
    find $LOG_DIR -type f -name "*.log" -mtime +$MAX_DAYS -delete
    log "삭제된 파일:"
    echo "$DELETED" >> $HISTORY
fi

# 정리 후 용량
AFTER=$(du -sh $LOG_DIR | cut -f1)
log "정리 후 용량: $AFTER"
log "=== 완료 ==="
```

#### Step 3. 실행 권한 부여 및 실행

```bash
chmod +x /home/ubuntu/cleanup_log.sh

# 실행 전 파일 목록
echo "=== 실행 전 ==="
ls -la /var/log/myapp/

# 스크립트 실행
/home/ubuntu/cleanup_log.sh

# 실행 후 파일 목록
echo "=== 실행 후 ==="
ls -la /var/log/myapp/

# 정리 이력 확인
cat /var/log/cleanup_history.log
```

**확인 포인트**
- `app_20260406_020000.log` (10일 전), `app_20260408_020000.log` (8일 전) → 삭제됨
- 최근 파일 (cron이 1분마다 만든 것) → 남아있음

---

### 실습 3. cleanup_log.sh를 cron에 등록

```bash
crontab -e
```

기존 내용에 아래 줄 추가:

```
# 매일 새벽 2시에 로그 정리
0 2 * * * /home/ubuntu/cleanup_log.sh >> /var/log/cron_cleanup.log 2>&1
```

```bash
# 최종 cron 목록 확인
crontab -l

# 결과:
# * * * * * /home/ubuntu/generate_log.sh
# 0 2 * * * /home/ubuntu/cleanup_log.sh >> /var/log/cron_cleanup.log 2>&1
```

```bash
# cron 실행 로그 확인
tail -f /var/log/syslog | grep CRON
```

---

## 핵심 정리

### sh 문법 요약

| 항목 | 문법 | 예시 |
|------|------|------|
| 변수 선언 ⭐ | `NAME=값` (공백 없음) | `PORT=8080` |
| 변수 사용 ⭐ | `$NAME` / `${NAME}` | `echo $PORT` |
| 명령어 결과 저장 ⭐ | `VAR=$(명령어)` | `TODAY=$(date +%Y-%m-%d)` |
| 기본값 설정 | `${VAR:-기본값}` | `${ENV:-development}` |
| 문자열 출력 ⭐ | `echo "..."` | `echo "Hello $NAME"` |
| 형식 출력 | `printf "형식" 값` | `printf "%s: %d\n" "점수" 90` |
| 정수 연산 | `$((식))` | `$((A + B))` |
| 문자열 길이 | `${#VAR}` | `${#NAME}` |
| 조건문 ⭐ | `if [ 조건 ]; then ... fi` | `if [ -f "$FILE" ]` |
| for 반복 ⭐ | `for VAR in 목록; do ... done` | `for FILE in *.log` |
| while 반복 | `while [ 조건 ]; do ... done` | `while [ $N -lt 10 ]` |
| 함수 정의 ⭐ | `함수명() { ... }` | `log() { echo $1 >> file; }` |
| 종료 코드 ⭐ | `exit 0` / `exit 1` | `exit 1` (에러) |
| 파일 이어쓰기 ⭐ | `>>` | `echo "msg" >> log.txt` |

### cron 요약

| 항목 | 내용 |
|------|------|
| 등록/수정 ⭐ | `crontab -e` |
| 목록 확인 ⭐ | `crontab -l` |
| 문법 ⭐ | `분 시 일 월 요일 명령어` |
| 절대 경로 ⭐ | cron 환경에서는 명령어를 절대 경로로 작성 |
| 로그 남기기 ⭐ | 명령어 뒤에 `>> 로그파일 2>&1` 추가 |
| Docker 주의 ⭐ | `service cron start` 먼저 실행 필수 |