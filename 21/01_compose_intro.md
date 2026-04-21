# 세션 1 — Docker Compose 개념 & Spring Boot + MySQL 구성

---

## 1-1. Docker Compose란?

### 핵심 개념

**Docker Compose는 "멀티 컨테이너 애플리케이션의 설계도"다.**

컨테이너 하나만으로 돌아가는 서비스는 거의 없다.
웹 서버, 앱 서버, DB, 캐시, 메시지 큐… 실제 서비스는 여러 컨테이너가 서로 연결되어야 한다.

Docker Compose는 이 **"여러 컨테이너가 어떻게 구성되고 연결되는지"를 YAML 파일 하나에 선언**함.
선언한 내용 그대로 `docker compose up` 명령으로 전체 서비스를 동시 실행함.

---

### 왜 필요한가?

어제 배운 `docker run` 명령어로 단일 컨테이너 기동 가능함.
실제 서비스는 **앱 + DB + 캐시 + 웹서버** 등 여러 컨테이너가 동시 동작함.

매번 `docker run` 을 여러 번 입력하면:

- 옵션 누락 실수
- 컨테이너 간 네트워크 수동 연결
- 시작 순서 직접 관리
- 팀원에게 "이렇게 실행하세요" 를 말로 전달해야 함
- 환경마다 옵션이 달라져 재현이 안 됨

**Compose 가 해결하는 것:**


| 문제                    | Compose 해결 방법                     |
| --------------------- | --------------------------------- |
| 긴 `docker run` 명령어 반복 | yml 파일에 한 번만 정의                   |
| 컨테이너 간 네트워크 수동 연결     | 같은 Compose 파일 내 서비스는 자동으로 같은 네트워크 |
| 시작 순서 관리              | `depends_on` 으로 선언                |
| 환경 재현 문제              | 파일을 공유하면 누구나 동일한 환경 실행            |


---

### 언제 쓰는가?

**쓰기 좋은 상황:**

- 로컬 개발 환경 — 앱 + DB + Redis 를 한 번에 올리고 내릴 때
- CI 파이프라인 — 테스트 실행 전 DB 컨테이너를 자동으로 준비할 때
- 소규모 배포 — 단일 서버에서 여러 서비스를 함께 운영할 때
- 팀 온보딩 — 새 팀원이 `docker compose up` 하나로 개발 환경을 바로 갖출 때

**쓰기 어색한 상황:**

- 수십~수백 대 서버에 걸친 대규모 배포 → Kubernetes 영역
- 무중단 롤링 업데이트, 오토스케일링이 필요한 프로덕션 → Kubernetes 영역

> 한 줄 요약: **"로컬 개발·소규모 배포"는 Compose, "대규모 클러스터 운영"은 Kubernetes**

---

→ Docker Compose = **여러 컨테이너를 하나의 파일로 정의하고 한 번에 실행**

```
docker-compose.yml 파일 하나
    ↓
docker compose up -d
    ↓
앱 + DB + 기타 서비스 전부 한 번에 실행
```

---

## 1-2. docker-compose.yml 기본 구조

```yaml
version: '3.8'           # Compose 파일 버전

services:                # 실행할 컨테이너 목록
  app:                   # 서비스 이름 (컨테이너 호스트명으로도 사용)
    image: myapp:1.0.0   # 사용할 이미지
    ports:
      - "8080:8080"      # 호스트:컨테이너 포트 매핑
    environment:
      - DB_HOST=db        # 환경 변수

  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
    volumes:
      - db-data:/var/lib/mysql   # 볼륨 마운트

volumes:                 # 볼륨 정의
  db-data:
```

---

## 1-2-1. 핵심 명령어 (Core Commands)


| 명령어                              | 설명                      |
| -------------------------------- | ----------------------- |
| `docker compose up -d`           | 전체 스택 백그라운드 실행          |
| `docker compose down`            | 컨테이너 + 네트워크 삭제          |
| `docker compose down -v`         | 컨테이너 + 네트워크 + 볼륨 삭제     |
| `docker compose ps`              | 서비스 상태 확인               |
| `docker compose logs -f [서비스]`   | 로그 스트리밍                 |
| `docker compose exec [서비스] [명령]` | 실행 중 서비스 내부 명령          |
| `docker compose restart [서비스]`   | 특정 서비스만 재시작             |
| `docker compose build`           | 이미지 빌드 (build 지시어 있을 때) |
| `docker compose config`          | 최종 설정 파일 출력 (검증용)       |


---

## 1-2-2. image vs build — 이미지를 가져올 것인가, 만들 것인가?

서비스 정의 시 가장 중요한 선택 지점임.

- `**image**`: 이미 만들어진 이미지를 가져다 씀 (예: MySQL, Redis, Nginx)
  ```yaml
  db:
    image: mysql:8.0
  ```
- `**build**`: 내 소스 코드를 바탕으로 **새로운 이미지를 만듦** (예: 내가 개발한 Spring Boot 앱)
  - 이때 반드시 해당 경로에 `**Dockerfile`**이 존재해야 함!

```yaml
services:
  app:
    build:
      context: ./backend    # Dockerfile이 있는 폴더 위치
      dockerfile: Dockerfile # 사용할 파일명 (기본값)
    ports:
      - "8080:8080"
```

```bash
# 이미지 빌드 후 실행 (코드가 수정되었을 때 필수)
docker compose up -d --build

# 이미지만 빌드 (실행은 하지 않음)
docker compose build
```

> **💡 팁**: 개발 중인 애플리케이션은 `build`를 사용하고, MySQL 처럼 검증된 공개 소프트웨어는 `image`를 사용하는 것이 일반적임.

---

## 1-3. Spring Boot + MySQL 구성

### 전체 구조

```
┌─────────────────────────────────────────────┐
│              Docker Network                 │
│                                             │
│  ┌─────────────┐        ┌───────────────┐   │
│  │  app        │ ──────►│  db           │   │
│  │ Spring Boot │  3306  │  MySQL 8.0    │   │
│  │ :8080       │        │               │   │
│  └─────────────┘        └───────────────┘   │
│         │                       │           │
└─────────┼───────────────────────┼───────────┘
          │                       │
     localhost:8080           db-data (볼륨)
```

- app 서비스 → `DB_HOST=db` 환경 변수로 db 서비스에 접속
- 서비스명(`db`)이 컨테이너 내부 호스트명으로 동작
- db 데이터는 볼륨에 저장 → 컨테이너 재시작해도 유지

### docker-compose.yml 완성본

```yaml
version: '3.8'

services:
  app:
    image: myapp:1.0.0
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb
      - SPRING_DATASOURCE_USERNAME=myuser
      - SPRING_DATASOURCE_PASSWORD=mypass
    depends_on:
      - db

  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: mydb
      MYSQL_USER: myuser
      MYSQL_PASSWORD: mypass
    volumes:
      - db-data:/var/lib/mysql
    ports:
      - "3306:3306"

volumes:
  db-data:
```

---

## 1-4. depends_on — 시작 순서 제어

```yaml
services:
  app:
    depends_on:
      - db       # db 컨테이너가 먼저 시작된 후 app 시작
```

**주의:** `depends_on`은 컨테이너 **시작 순서**만 보장함. DB가 실제 쿼리를 처리할 준비가 되었는지는 보장하지 않음.

```yaml
# healthcheck 로 실제 준비 상태 확인
services:
  db:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    depends_on:
      db:
        condition: service_healthy   # db가 healthy 상태일 때만 app 시작
```

---

## 1-5. 실습 — 따라하기

### 실습 1 — docker-compose.yml 파일 확인 & 실행

```bash
# 파일 내용 확인
cat docker-compose.yml

# 전체 스택 실행
docker compose up -d
```

출력에서 확인할 것:

```
[+] Running 3/3
 ✔ Network spring-mysql_default  Created
 ✔ Container spring-mysql-db-1   Started
 ✔ Container spring-mysql-app-1  Started
```

```bash
# 서비스 상태 확인
docker compose ps

# NAME                    STATUS          PORTS
# spring-mysql-app-1      running         0.0.0.0:8080->8080/tcp
# spring-mysql-db-1       running         0.0.0.0:3306->3306/tcp
```

```bash
# 브라우저에서 http://localhost:8080 접속 확인
```

---

### 실습 2 — 로그 확인 & 서비스 분리 조회

```bash
# 전체 서비스 로그
docker compose logs

# app 서비스만 로그 (실시간)
docker compose logs -f app

# db 서비스만 로그
docker compose logs db

# 마지막 20줄만
docker compose logs --tail 20 app
```

```bash
# 각 컨테이너 자원 사용량
docker compose top
```

---

### 실습 3 — DB 접속 확인

```bash
# db 컨테이너 내부 MySQL 접속
docker compose exec db mysql -u root -prootpass

# MySQL 프롬프트에서
SHOW DATABASES;
USE mydb;
SHOW TABLES;
EXIT;
```

```bash
# app 컨테이너 내부에서 db 호스트명으로 통신 확인
docker compose exec app sh
ping db     # db라는 호스트명으로 통신 가능
exit
```

```bash
# 전체 정리
docker compose down -v
```

---

## 1-6. 응용 실습 문제

---

### 문제 1 — 단일 명령으로 전체 스택 실행

`spring-mysql` 폴더에서 전체 스택을 실행하고 아래를 확인하세요.

조건:

- `docker compose up -d` 로 실행
- `docker compose ps` 로 두 서비스 모두 `running` 상태 확인
- 브라우저에서 `http://localhost:8080` 접속 확인
- `docker compose down` 으로 정리

---

### 문제 2 — 환경 변수로 DB 비밀번호 변경

`docker-compose.yml` 의 MySQL 비밀번호를 변경하고 재실행하세요.

조건:

- `MYSQL_ROOT_PASSWORD` 를 `newpassword123` 으로 변경
- `docker compose down -v` (볼륨까지 삭제) 후 `docker compose up -d`
- `docker compose exec db mysql -u root -pnewpassword123` 로 접속 확인

> 힌트: 볼륨을 삭제하지 않으면 기존 비밀번호가 유지됨.

---

### 문제 3 — depends_on 제거 후 순서 문제 확인

`docker-compose.yml` 에서 app 서비스의 `depends_on` 을 제거하고 어떤 일이 생기는지 확인하세요.

조건:

- `depends_on` 제거 후 `docker compose up -d`
- `docker compose logs app` 에서 DB 연결 실패 오류 확인
- 다시 `depends_on` 복원 후 재실행

---

## 정리

- Docker Compose = 여러 컨테이너를 yml 파일 하나로 정의 및 동시 실행함.
- 서비스명이 컨테이너 내부 호스트명으로 동작함. (`DB_HOST=db`)
- `depends_on` = 시작 순서 보장하며, 실제 DB 가동 상태는 healthcheck로 확인 필수임.
- 볼륨을 통한 DB 데이터 영속성 확보함.

