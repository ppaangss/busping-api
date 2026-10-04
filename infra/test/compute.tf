# ---- SSH 키페어 - 세션마다 생성, pem은 로컬에만 저장 (gitignore) ----

resource "tls_private_key" "ssh" {
  algorithm = "ED25519"
}

resource "aws_key_pair" "main" {
  key_name   = "${var.name_prefix}-key"
  public_key = tls_private_key.ssh.public_key_openssh
}

resource "local_file" "ssh_pem" {
  content         = tls_private_key.ssh.private_key_openssh
  filename        = "${path.module}/${var.name_prefix}.pem"
  file_permission = "0600"
}

# ---- AMI - Amazon Linux 2023 (앱·부하발생기 x86, redis·db·mock은 t4g라 arm) ----

data "aws_ami" "al2023_x86" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-x86_64"]
  }
}

data "aws_ami" "al2023_arm" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-arm64"]
  }
}

# ---- 앱 서버 1대 - 측정 대상 ----
# 1대인 이유: 측정은 단위 용량(톰캣 200 하나의 한계)을 재는 것 - 운영 대수는 거기서 나눗셈
# 기종은 운영 후보와 동일(t3.small) - 측정의 절대 숫자가 운영 예보가 되려면 기계가 같아야 한다

resource "aws_instance" "app" {
  ami                    = data.aws_ami.al2023_x86.id
  instance_type          = "t3.small"
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.app.id]
  key_name               = aws_key_pair.main.key_name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y java-17-amazon-corretto-headless
  EOF

  tags = { Name = "${var.name_prefix}-app" }
}

# ---- Redis 1대 - 네이티브 설치, 바인드 개방 (접근 통제는 SG가 담당) ----

resource "aws_instance" "redis" {
  ami                    = data.aws_ami.al2023_arm.id
  instance_type          = "t4g.micro"
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.redis.id]
  key_name               = aws_key_pair.main.key_name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y redis6
    sed -i 's/^bind .*/bind 0.0.0.0/' /etc/redis6/redis6.conf
    sed -i 's/^protected-mode yes/protected-mode no/' /etc/redis6/redis6.conf
    systemctl enable --now redis6
  EOF

  tags = { Name = "${var.name_prefix}-redis" }
}

# ---- 부하발생기 1대 - k6 + mysql 클라이언트(시드 투입용) ----

resource "aws_instance" "loadgen" {
  ami                    = data.aws_ami.al2023_x86.id
  instance_type          = "t3.small"
  subnet_id              = aws_subnet.public[1].id
  vpc_security_group_ids = [aws_security_group.loadgen.id]
  key_name               = aws_key_pair.main.key_name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y https://dl.k6.io/rpm/repo.rpm
    dnf install -y k6 mariadb105
  EOF

  tags = { Name = "${var.name_prefix}-loadgen" }
}

# ---- mock 외부 API 1대 - WireMock 단일 프로세스 (mock TAGO :8081, 저널 off) ----
# 매핑 파일과 extension jar(tago-world: 실측 지연·에러율 재현)는 deploy 스크립트가 scp로 전달 후 서비스 시작
# 부팅 시엔 jar가 아직 없어 기동 불가이므로 enable만 하고 start는 deploy가 담당
# container-threads 200: 지연 도입으로 요청당 평균 ~1.4초 스레드 점유 - 기본 10개면 fake가 병목이 된다

resource "aws_instance" "mock" {
  ami                    = data.aws_ami.al2023_arm.id
  instance_type          = "t4g.micro"
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.mock.id]
  key_name               = aws_key_pair.main.key_name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y java-17-amazon-corretto-headless
    mkdir -p /home/ec2-user/mock/mappings
    curl -sfL -o /home/ec2-user/mock/wiremock.jar \
      https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.9.1/wiremock-standalone-3.9.1.jar
    chown -R ec2-user:ec2-user /home/ec2-user/mock
    cat > /etc/systemd/system/mock-tago.service <<'UNIT'
    [Unit]
    Description=mock tago
    After=network.target

    [Service]
    User=ec2-user
    ExecStart=/usr/bin/java -cp /home/ec2-user/mock/wiremock.jar:/home/ec2-user/mock/tago-world.jar wiremock.Run --port 8081 --root-dir /home/ec2-user/mock --no-request-journal --extensions com.busping.faketago.TagoWorld --container-threads 200
    Restart=always
    RestartSec=3

    [Install]
    WantedBy=multi-user.target
    UNIT
    systemctl daemon-reload
    systemctl enable mock-tago
  EOF

  tags = { Name = "${var.name_prefix}-mock" }
}

