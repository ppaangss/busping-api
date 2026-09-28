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

# ---- AMI - Amazon Linux 2023 (앱·부하발생기 x86, redis는 t4g라 arm) ----

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

# ---- 앱 서버 2대 - AZ 분산 (가용성 테스트의 전제) ----

resource "aws_instance" "app" {
  count = 2

  ami                    = data.aws_ami.al2023_x86.id
  instance_type          = "t3.small"
  subnet_id              = aws_subnet.public[count.index].id
  vpc_security_group_ids = [aws_security_group.app.id]
  key_name               = aws_key_pair.main.key_name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y java-17-amazon-corretto-headless
  EOF

  tags = { Name = "${var.name_prefix}-app-${count.index + 1}" }
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

# ALB 80 <- 부하발생기 공인 IP
# (부하발생기는 ALB의 공인 DNS로 요청하므로 SG 참조가 아니라 공인 IP로 허용해야 한다)
resource "aws_vpc_security_group_ingress_rule" "alb_http_loadgen" {
  security_group_id = aws_security_group.alb.id
  cidr_ipv4         = "${aws_instance.loadgen.public_ip}/32"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}
