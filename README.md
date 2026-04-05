# ChessWeb - Huong dan chay nhanh bang Docker Compose

Muc tieu: chi can chay `docker compose` de dung web, khong can cai/running MySQL thu cong.

## 1. Clone source

```bash
git clone <LINK_REPO_GITHUB>
cd <TEN_THU_MUC_PROJECT>
```

Vi du:

```bash
git clone https://github.com/<username>/<repo>.git
cd ChessMaster_CMT
```

## 2. Chay project bang Docker Compose

Tai thu muc goc project (noi co file `docker-compose.yml`), chay:

```bash
docker compose up -d --build
```

Lenh nay se:
- Build image cho ung dung Spring Boot
- Khoi tao MySQL container
- Tu dong chay script `database_setup.sql`
- Chay web app tren cong `8080`

## 3. Truy cap web

- Trang chinh: `http://localhost:8080/`
- Dang nhap: `http://localhost:8080/Login.html`
- Dang ky: `http://localhost:8080/Register.html`
- Dashboard: `http://localhost:8080/Dashboard.html`

## 4. Cac lenh Docker Compose thuong dung

Xem trang thai container:

```bash
docker compose ps
```

Xem log:

```bash
docker compose logs -f
```

Dung he thong:

```bash
docker compose down
```

Dung va xoa ca volume database (reset du lieu):

```bash
docker compose down -v
```

## 5. Neu sua code va muon chay lai

```bash
docker compose up -d --build
```

## Ghi chu

- Neu may chua bat Docker Desktop, hay bat Docker truoc khi chay lenh.
- Toan bo DB da duoc dong goi trong `docker-compose.yml`, khong can cai MySQL rieng.
