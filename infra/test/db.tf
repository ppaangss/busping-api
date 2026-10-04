# ---- DB 1대 - 도커 MySQL (RDS 대용) ----
# 테스트 환경엔 지킬 데이터가 없다(시드는 스크립트로 재생성) - RDS의 백업·관리 기능은 쓸 일이 없는 비용
# t4g.micro는 운영 후보 RDS(db.t4g.micro)와 같은 하드웨어 급이라 성능 등가성이 유지된다

resource "random_password" "db" {
  length  = 24
  special = false # JDBC URL·셸 인용 문제 회피
}

resource "aws_instance" "db" {
  ami                    = data.aws_ami.al2023_arm.id
  instance_type          = "t4g.micro"
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.db.id]
  key_name               = aws_key_pair.main.key_name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y docker
    systemctl enable --now docker
    # restart always - 컨테이너가 혼자 죽으면 셀프 복구 (일회용 환경의 유일한 보험)
    docker run -d --name mysql --restart always \
      -e MYSQL_ROOT_PASSWORD='${random_password.db.result}' \
      -e MYSQL_DATABASE=ppaangss_test \
      -e MYSQL_USER=ppaangss \
      -e MYSQL_PASSWORD='${random_password.db.result}' \
      -p 3306:3306 mysql:8
  EOF

  tags = { Name = "${var.name_prefix}-db" }
}
