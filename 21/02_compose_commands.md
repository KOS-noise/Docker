# 세션 2 — Compose 핵심 명령어 & 네트워크·볼륨


---

## 2-1. 네트워크 — 서비스 간 통신

Compose 실행 시 **자동으로 전용 네트워크 생성**.
같은 Compose 파일 내 서비스끼리는 **서비스명으로 통신** 가능.

```yaml
# 기본 네트워크 (자동 생성)
services:
  app:
    image: myapp:1.0.0
  db:
    image: mysql:8.0
# → app에서 'db' 라는 호스트명으로 MySQL 접속 가능
```

```yaml
# 커스텀 네트워크 직접 정의
services:
  app:
    networks:
      - backend
  db:
    networks:
      - backend

networks:
  backend:
    driver: bridge
```

```bash
# 생성된 네트워크 확인
docker network ls
docker network inspect spring-mysql_default
```

---

### 💡 왜 IP가 아니라 '서비스명'이어야 할까? (MSA/Cloud Native 관점)

현업에서 마이크로서비스 아키텍처(MSA)나 Kubernetes(EKS 등)를 사용하면 이 개념은 선택이 아닌 **필수적임**.


1.  **서비스 디스커버리(Service Discovery)**: "이름만으로 현재 가동 중인 컨테이너를 탐색한다"는 개념임. Docker Compose의 네트워크는 이러한 자동화 기술의 가장 기본적인 형태임.
2.  **컨테이너는 가변적(Ephemeral)임**: 클라우드 환경에서 컨테이너는 빈번하게 소멸하고 재생성됨. 이때마다 IP 주소가 변경되므로, 코드에 IP를 명시할 경우 지속적인 수정이 필요함.
3.  **확장성(Scalability)**: 특정 서비스의 컨테이너 인스턴스가 증가하더라도, 호출측은 서비스명 하나만으로 통신 가능함. 인프라의 복잡성을 서비스 명칭 뒤로 은닉하는 **추상화**의 핵심임.

> **강사 코멘트**: "이 '서비스명 통신' 개념이 추후 Kubernetes의 'Service' 및 'DNS' 개념으로 직결됨"을 강조하는 가이드 권장.

---

## 2-2. 볼륨 — 데이터 영속성

컨테이너는 소멸 시 내부 데이터를 모두 유실함. 데이터의 영구 저장을 위해 **호스트 시스템의 저장 공간과 연결**하는 과정이 필수적임.

### 1-1. 두 가지 연결 방식 (Volume vs Bind Mount)

```yaml
services:
  db:
    image: mysql:8.0
    volumes:
      - db-data:/var/lib/mysql                           # 1. Named Volume
      - ./my-config/my.cnf:/etc/mysql/conf.d/my.cnf      # 2. Bind Mount

# Named Volume 사용 시 최하단에 반드시 정의 필요
volumes:
  db-data:
```

---

### 1-2. 기술적 차이점 비교

| 항목 | Named Volume (Docker 관리형) | Bind Mount (호스트 직접 연결형) |
| :--- | :--- | :--- |
| **저장 위치** | Docker 전용 스토리지 영역 (`/var/lib/docker/volumes`) | 사용자가 지정한 호스트 경로 (예: `./data`, `C:/config`) |
| **설정 방법** | 이름으로 지정 (예: `db-data`) | 경로로 지정 (예: `./init.sql`, `/home/user/logs`) |
| **특징** | Docker가 관리하여 안정적이고 성능이 높음 | 호스트 폴더와 컨테이너가 실시간 동기화됨 |
| **추가 설정** | 파일 하단 `volumes:` 섹션에 이름 등록 필수 | 별도 등록 없음. 경로만 정확하면 즉시 사용 가능 |

---

### 1-3. 언제 어떤 것을 사용해야 하는가?

*   **Named Volume (추천 — 데이터 보호 용도)**
    *   **사용 사례**: 데이터베이스(MySQL, PostgreSQL)의 실제 데이터 저장소.
    *   **장점**: 컨테이너나 이미지를 삭제해도 데이터가 Docker 관리 하에 안전하게 보존됨. 백업 및 마이그레이션이 용이함.
*   **Bind Mount (추천 — 설정 및 개발 용도)**
    *   **사용 사례**: 소스 코드(Hot Reload용), 설정 파일(`.conf`, `.yml`), 로그 파일(실시간 확인용).
    *   **장점**: 호스트에서 파일을 수정하면 컨테이너에 즉시 반영되어 개발 생산성이 높음.

> **⚠️ 주의 — 구문 구분법**: 
> - **경로로 시작 (`/` 또는 `./`)**하면 **Bind Mount**임. 
> - **이름으로 시작**하면 **Named Volume**이며, 파일 하단에 별도 선언이 없으면 에러가 발생함.

```bash
# 볼륨 목록 및 상세 정보 확인 명령어
docker volume ls           # 전체 볼륨 리스트 조회
docker volume inspect [명칭] # 볼륨이 호스트의 실제 어디에 저장되어 있는지 확인
```

---

## 2-3. 환경 변수 관리 — .env 파일

비밀번호 등 민감 정보를 `docker-compose.yml` 에 직접 기록하지 않고 `.env` 파일로 분리 관리함.

```bash
# .env 파일
DB_ROOT_PASSWORD=rootpass
DB_NAME=mydb
DB_USER=myuser
DB_PASSWORD=mypass
APP_PORT=8080
```

```yaml
# docker-compose.yml 에서 참조
services:
  app:
    ports:
      - "${APP_PORT}:8080"
  db:
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PASSWORD}
```

```bash
# .env 파일 적용 확인
docker compose config    # 변수 치환된 최종 설정 출력
```

> `.env` 파일은 **반드시 .gitignore 에 추가**해야 함. 커밋 시 비밀번호 노출 위험 존재.

---

## 2-4. 실습 — 따라하기

#### **0. 실습 전 준비 사항**
*   **사용 폴더**: 제공된 `exampleCode` 폴더를 기본으로 사용함.
*   **폴더 관리**: 실습 결과물을 분리하고 싶다면 `exampleCode` 하위에 `session1`, `session2` 등의 폴더를 생성하여 `docker-compose.yml` 파일을 복사해 사용하는 방식 권장함.
*   **상태 초기화**: 새로운 실습 진행을 위해 기존에 실행 중인 컨테이너를 종료함.
    ```bash
    # 폴더 이동 (예시)
    cd ./exampleCode
    # 서비스 종료
    docker compose down
    ```

---

### 실습 1 — 특정 서비스만 재시작 (명령어 실습)
> **안내**: 기존 `docker-compose.yml` 파일을 수정하지 않고, 터미널에서 명령어만 입력하여 동작을 확인하는 과정임.

```bash
# 전체 스택 실행
docker compose up -d

# app 서비스만 재시작
docker compose restart app

# 로그에서 재시작 확인
docker compose logs --tail 20 app

# db는 재시작되지 않음 확인
docker compose ps
```

---

### 실습 2 — 볼륨 데이터 영속성 확인 (명령어 실습)
> **안내**: DB에 데이터를 입력한 후, 컨테이너를 삭제(down)하고 재생성(up)해도 데이터가 보존되는지 확인함.

```bash
# DB에 테이블 생성
docker compose exec db mysql -u root -prootpass mydb -e "
  CREATE TABLE users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50));
  INSERT INTO users (name) VALUES ('Alice'), ('Bob');
"

# 데이터 확인
docker compose exec db mysql -u root -prootpass mydb -e "SELECT * FROM users;"

# 컨테이너 삭제 (볼륨은 유지)
docker compose down

# 다시 실행
docker compose up -d

# 데이터가 남아있는지 확인
docker compose exec db mysql -u root -prootpass mydb -e "SELECT * FROM users;"
```

---

### 실습 3 — .env 파일로 환경 변수 분리 (파일 수정 실습)
> **안내**: 기존 `docker-compose.yml` 파일을 열어 내용을 **수정**하고, 새로운 `.env` 파일을 생성하여 연동하는 과정임.

```bash
# .env 파일 생성
cat > .env << 'EOF'
DB_ROOT_PASSWORD=securepass
DB_NAME=mydb
DB_USER=myuser
DB_PASSWORD=mypass
APP_PORT=8080
EOF

# docker-compose.yml 수정 (변수 참조로 변경)
# ${DB_ROOT_PASSWORD}, ${APP_PORT} 등으로 교체

# 최종 설정 확인
docker compose config

# 실행
docker compose up -d
docker compose ps
```

---

## 2-5. 응용 실습 문제

---

### 문제 1 — 볼륨 확인 및 데이터 유지

DB에 데이터를 넣고, `docker compose down` 후 `docker compose up -d` 로 재실행해도 데이터가 유지되는지 확인하세요.

조건:
- `docker compose exec db mysql ...` 로 테이블 생성 & 데이터 삽입
- `docker compose down` (볼륨 삭제 없이)
- `docker compose up -d` 후 데이터 확인

> 힌트: `down -v` 하면 볼륨까지 삭제되어 데이터가 사라짐.

---

### 문제 2 — app 서비스만 재시작

app 서비스만 재시작하고 db는 그대로 유지되는지 확인하세요.

조건:
- `docker compose restart app`
- `docker compose logs -f app` 으로 재시작 로그 확인
- `docker compose ps` 에서 db의 `STATUS` 에 재시작 흔적이 없는지 확인

---

### 문제 3 — 컨테이너 내부에서 서비스 간 통신 확인

app 컨테이너 내부에서 `db` 호스트명으로 통신이 되는지 확인하세요.

조건:
- `docker compose exec app sh` 로 내부 접속
- `ping db` 또는 `nslookup db` 로 db 호스트명 해석 확인
- 결과에서 `db` 가 내부 IP로 해석되는지 확인

---

## 정리

- `up -d` / `down` / `exec` / `logs` / `restart` 가 핵심 명령어임.
- Named Volume = DB 데이터 영속성 기능, `down -v` 수행 시 삭제됨.
- 서비스명이 네트워크 내 호스트명으로 동작함.
- `.env` 파일로 민감 정보 분리 → `.gitignore` 등록 필수임.