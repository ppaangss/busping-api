# apply 시점의 내 공인 IP - 세션마다 환경을 새로 띄우는 패턴이라 하드코딩 대신 매번 조회
data "http" "my_ip" {
  url = "https://checkip.amazonaws.com"
}

locals {
  my_ip_cidr = "${chomp(data.http.my_ip.response_body)}/32"
}

# ---- 보안그룹 5개 ----
# 전 리소스가 퍼블릭 서브넷에 있으므로 격리는 전부 여기서 담당한다.
# SG 설명(description)은 AWS가 ASCII만 허용해서 영문.

resource "aws_security_group" "app" {
  name        = "${var.name_prefix}-app-sg"
  description = "app server - 8080 from load generator and admin ip, ssh from admin ip"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.name_prefix}-app-sg" }
}

resource "aws_security_group" "redis" {
  name        = "${var.name_prefix}-redis-sg"
  description = "redis - 6379 from app, ssh from admin ip"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.name_prefix}-redis-sg" }
}

resource "aws_security_group" "db" {
  name        = "${var.name_prefix}-db-sg"
  description = "db - 3306 from app and load generator, ssh from admin ip"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.name_prefix}-db-sg" }
}

resource "aws_security_group" "loadgen" {
  name        = "${var.name_prefix}-loadgen-sg"
  description = "load generator - ssh from admin ip"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.name_prefix}-loadgen-sg" }
}

resource "aws_security_group" "mock" {
  name        = "${var.name_prefix}-mock-sg"
  description = "mock external api - 8081 from app, ssh from admin ip"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.name_prefix}-mock-sg" }
}

# ---- 인그레스 규칙 ----

# 앱 8080 <- 부하발생기 (ALB 없이 직접 - 테스트 진입점)
resource "aws_vpc_security_group_ingress_rule" "app_from_loadgen" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.loadgen.id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}

# 앱 8080 <- 내 IP (드라이런·디버깅)
resource "aws_vpc_security_group_ingress_rule" "app_http_admin" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = local.my_ip_cidr
  from_port         = 8080
  to_port           = 8080
  ip_protocol       = "tcp"
}

# 앱 SSH <- 내 IP
resource "aws_vpc_security_group_ingress_rule" "app_ssh_admin" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = local.my_ip_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# Redis 6379 <- 앱만
resource "aws_vpc_security_group_ingress_rule" "redis_from_app" {
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = aws_security_group.app.id
  from_port                    = 6379
  to_port                      = 6379
  ip_protocol                  = "tcp"
}

# Redis SSH <- 내 IP (장애 테스트 때 redis 죽이러 들어갈 문)
resource "aws_vpc_security_group_ingress_rule" "redis_ssh_admin" {
  security_group_id = aws_security_group.redis.id
  cidr_ipv4         = local.my_ip_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# DB 3306 <- 앱
resource "aws_vpc_security_group_ingress_rule" "db_from_app" {
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = aws_security_group.app.id
  from_port                    = 3306
  to_port                      = 3306
  ip_protocol                  = "tcp"
}

# DB 3306 <- 부하발생기 (시드 대량 투입용)
resource "aws_vpc_security_group_ingress_rule" "db_from_loadgen" {
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = aws_security_group.loadgen.id
  from_port                    = 3306
  to_port                      = 3306
  ip_protocol                  = "tcp"
}

# DB SSH <- 내 IP (도커 컨테이너 디버깅)
resource "aws_vpc_security_group_ingress_rule" "db_ssh_admin" {
  security_group_id = aws_security_group.db.id
  cidr_ipv4         = local.my_ip_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# 부하발생기 SSH <- 내 IP
resource "aws_vpc_security_group_ingress_rule" "loadgen_ssh_admin" {
  security_group_id = aws_security_group.loadgen.id
  cidr_ipv4         = local.my_ip_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# mock TAGO 8081 <- 앱만
resource "aws_vpc_security_group_ingress_rule" "mock_from_app" {
  security_group_id            = aws_security_group.mock.id
  referenced_security_group_id = aws_security_group.app.id
  from_port                    = 8081
  to_port                      = 8081
  ip_protocol                  = "tcp"
}

# mock SSH <- 내 IP (매핑 배포·디버깅)
resource "aws_vpc_security_group_ingress_rule" "mock_ssh_admin" {
  security_group_id = aws_security_group.mock.id
  cidr_ipv4         = local.my_ip_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# ---- 이그레스: 전부 전체 허용 (패키지 설치, TAGO/FCM 아웃바운드 등) ----

resource "aws_vpc_security_group_egress_rule" "all" {
  for_each = {
    app     = aws_security_group.app.id
    redis   = aws_security_group.redis.id
    db      = aws_security_group.db.id
    loadgen = aws_security_group.loadgen.id
    mock    = aws_security_group.mock.id
  }

  security_group_id = each.value
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
