# 세션 3 — 핵심 명령어 실습


---

## 실습 목표

nginx 컨테이너를 직접 띄우고, 로그 확인, 내부 접속, 정리까지
**전체 흐름을 손으로 익히는 것**이 목표입니다.

---

## 3-1. docker pull — 이미지 내려받기

```bash
docker pull nginx:alpine
```

---

## 3-2. docker run — 컨테이너 실행

```bash
docker run -d -p 8080:80 --name my-nginx nginx:alpine
```

### 옵션 설명

| 옵션 | 의미 |
|------|------|
| `-d` | detach — 백그라운드 실행 |
| `-p 8080:80` | 호스트:8080 → 컨테이너:80 포트 포워딩 |
| `--name my-nginx` | 컨테이너 이름 지정 (없으면 랜덤) |
| `nginx:alpine` | 사용할 이미지:태그 |

> 브라우저에서 `http://localhost:8080` 접속 → nginx 기본 페이지 확인

### 자주 쓰는 run 옵션 모음

```bash
# 환경 변수 직접 전달
docker run -e MY_VAR=hello nginx:alpine

# .env 파일로 여러 환경 변수 한 번에 전달
docker run --env-file .env nginx:alpine

# 볼륨 마운트
docker run -v /host/path:/container/path nginx:alpine

# 메모리 제한
docker run --memory=512m nginx:alpine

# CPU 제한
docker run --cpus=1.5 nginx:alpine

# 자동 삭제 (종료 시)
docker run --rm nginx:alpine echo "done"

# 포그라운드 + 대화형
docker run -it ubuntu:22.04 bash
```

---

## 3-3. docker logs — 로그 확인

```bash
# 전체 로그 출력
docker logs my-nginx

# 실시간 스트리밍 (-f = follow)
docker logs -f my-nginx

# 마지막 50줄만
docker logs --tail 50 my-nginx

# 타임스탬프 포함
docker logs -t my-nginx

# 특정 시간 이후 로그
docker logs --since 5m my-nginx
```

> 브라우저에서 `http://localhost:8080` 새로고침 후 로그 변화 확인

---

## 3-4. docker exec — 실행 중인 컨테이너 내부 접속

```bash
# sh 쉘로 접속 (alpine은 bash 없음)
docker exec -it my-nginx sh

# 쉘 없이 단일 명령 실행
docker exec my-nginx cat /etc/hosts
docker exec my-nginx nginx -v
```

> **💡 리눅스 쉘(Shell) 상식: sh vs bash**
> - **쉘(Shell)**: 사용자가 입력한 명령어를 컴퓨터에게 전달해 주는 **'통역사'**임.
> - **`sh` (기본형)**: 기능은 적지만 가벼움. **alpine** 같은 초경량 이미지에 들어있음.
> - **`bash` (고급형)**: 자동 완성 등 편의 기능이 많음. **ubuntu, latest** 등 일반 이미지에 들어있음.
> - **결론**: `alpine` 이미지는 다이어트를 위해 `bash`를 빼버렸으므로 반드시 `sh`라고 불러야 함!

### 옵션 설명

| 옵션 | 의미 |
|------|------|
| `-i` | interactive — stdin 유지 |
| `-t` | tty — 터미널 할당 |
| `-it` | 대화형 터미널 (보통 함께 사용) |


---

## 3-5. 실습 — 따라하기

### 실습 1 — nginx 띄우고 로그로 접속 흔적 확인

```bash
# 컨테이너 실행
docker run -d -p 8080:80 --name my-nginx nginx:alpine

# 실행 확인
docker ps

# 로그 확인 — 접속 기록이 찍히는지 확인
docker logs my-nginx

# 실시간 로그 스트리밍 상태로 브라우저 새로고침
docker logs -f my-nginx
# 로그가 실시간으로 추가되는 것 확인 후 Ctrl+C
```

출력에서 확인할 것:
- 브라우저 새로고침마다 `GET / HTTP/1.1 200` 로그가 추가됨
- 접속 IP, 시간, 상태코드 포함

```bash
# 정리 (실습 2를 위해 정리하지 않고 넘어가도 좋음)
# docker stop my-nginx
# docker rm my-nginx
```

---

### 실습 2 — exec로 접속하여 실제 로그 파일 찾아가기

실습 1에서 봤던 로그 데이터의 '본거지'를 찾아가서 직접 확인.

```bash
# 1. 컨테이너 내부 접속
docker exec -it my-nginx sh

# 2. nginx 로그가 쌓이는 폴더로 이동
cd /var/log/nginx

# 3. 로그 파일 목록 확인
ls -l

# 4. 직접 로그 파일 내용 출력하여 실습 1의 로그와 대조
cat access.log

# 5. 종료
exit
```

> **💡 원리 깨치기: 왜 docker logs로 보였을까요?**
> - 컨테이너 내부의 `access.log` 파일을 잘 보면 터미널 출력(`stdout`)으로 연결되어 있음.
> - 덕분에 우리는 컨테이너 안에 들어가지 않고도 밖에서 `docker logs`로 실시간 접속 현황을 볼 수 있는 것임!

```bash
# 정리
docker stop my-nginx && docker rm my-nginx
```

---

### 실습 3 — 환경 변수 주입 및 확인

컨테이너 실행 시 `-e` 옵션으로 환경 변수를 전달하고, 내부에서 확인.

```bash
# 환경 변수를 넣어서 실행
docker run -d --name env-test \
  -e APP_ENV=production \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  nginx:alpine

# 컨테이너 내부에서 환경 변수 확인
docker exec env-test env

# 특정 환경 변수만 확인
docker exec env-test sh -c "echo \$APP_ENV"
# production 출력

# inspect로도 확인 가능
docker inspect env-test | grep -A 10 '"Env"'

# 정리
docker stop env-test && docker rm env-test
```

---

## 3-9. 응용 실습 문제

> 모르면 `docker [명령어] --help` 참고.

---

### 문제 1 — 로그 필터링

`nginx:alpine` 컨테이너를 실행하고 브라우저에서 여러 번 접속한 뒤,
**최근 5분 이내 로그만** 타임스탬프와 함께 출력하세요.

조건:
- 컨테이너 이름: `log-test`
- 포트: `8080:80`
- 브라우저에서 최소 5회 이상 접속
- `--since` 와 `-t` 옵션 함께 사용
- 확인 후 컨테이너 정지 및 삭제

---

### 문제 2 — 실행 중인 컨테이너 내부 파일 수정

`nginx:alpine` 컨테이너를 실행하고, 기본 페이지 내용을 직접 수정해보세요.

조건:
- 컨테이너 이름: `edit-test`, 포트: `8080:80`
- `docker exec` 로 컨테이너 내부 접속
- `/usr/share/nginx/html/index.html` 내용을 `Hello Docker!` 로 교체
  ```bash
  echo "Hello Docker!" > /usr/share/nginx/html/index.html
  ```
- 브라우저에서 `http://localhost:8080` 접속 → 변경 내용 확인
- 컨테이너를 `stop → rm → run` 으로 새로 실행 시 원래 페이지로 돌아오는지 확인

> 힌트: 컨테이너 내 파일 변경은 해당 컨테이너에만 적용. 이미지는 변경되지 않음.

---

### 문제 3 — 컨테이너 3개를 한 번에 정리

아래 3개 컨테이너를 실행한 뒤, **명령어 한 줄로** 모두 정지하고 삭제하세요.

```bash
docker run -d --name clean-1 nginx:alpine
docker run -d --name clean-2 nginx:alpine
docker run -d --name clean-3 nginx:alpine
```

조건:
- `docker stop` 한 번에 3개 정지
- `docker rm` 한 번에 3개 삭제
- `docker ps -a` 로 모두 사라진 것 확인

> 힌트: `docker stop` 과 `docker rm` 은 여러 이름을 공백으로 구분해서 한 번에 처리 가능.

---

## 명령어 치트시트

```
pull    이미지 다운로드
run     이미지로 컨테이너 생성 + 실행
ps      컨테이너 목록 조회
logs    컨테이너 로그 출력
exec    실행 중 컨테이너에 명령 실행
stop    컨테이너 정상 종료
kill    컨테이너 강제 종료
start   정지된 컨테이너 재시작
rm      컨테이너 삭제
rmi     이미지 삭제
images  로컬 이미지 목록
```