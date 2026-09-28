#!/usr/bin/env bash
# 앱 배포: 로컬 bootJar 빌드 -> 앱 서버 2대에 scp -> systemd 재시작 (순차)
# 사용: ./deploy.sh   (terraform apply 완료 상태에서)
set -euo pipefail
cd "$(dirname "$0")"

echo ">>> terraform output 읽기"
APP_IPS=$(terraform output -json app_public_ips | jq -r '.[]')
REDIS_IP=$(terraform output -raw redis_private_ip)
RDS_HOST=$(terraform output -raw rds_endpoint)
DB_PW=$(terraform output -raw db_password)
PEM=$(terraform output -raw ssh_key_path)
ALB_DNS=$(terraform output -raw alb_dns_name)

echo ">>> jar 빌드"
(cd .. && ./gradlew bootJar -q)
JAR=$(ls -t ../build/libs/*.jar | grep -v plain | head -1)
echo "    $JAR"

SSH_OPTS=(-i "$PEM" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 -o LogLevel=ERROR)

for IP in $APP_IPS; do
  echo ">>> 배포: $IP"

  scp "${SSH_OPTS[@]}" "$JAR" "ec2-user@$IP:/home/ec2-user/app.jar.new"

  # 환경변수 파일 (DB 비밀번호 포함 - 600)
  ssh "${SSH_OPTS[@]}" "ec2-user@$IP" "sudo tee /etc/app.env > /dev/null && sudo chmod 600 /etc/app.env" <<ENV
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:mysql://$RDS_HOST:3306/ppaangss_test?serverTimezone=Asia/Seoul
DB_USERNAME=ppaangss
DB_PASSWORD=$DB_PW
REDIS_HOST=$REDIS_IP
ENV

  ssh "${SSH_OPTS[@]}" "ec2-user@$IP" "sudo tee /etc/systemd/system/app.service > /dev/null" <<'UNIT'
[Unit]
Description=app
After=network.target

[Service]
User=ec2-user
EnvironmentFile=/etc/app.env
ExecStart=/usr/bin/java -jar /home/ec2-user/app.jar
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
UNIT

  ssh "${SSH_OPTS[@]}" "ec2-user@$IP" '
    mv /home/ec2-user/app.jar.new /home/ec2-user/app.jar
    sudo systemctl daemon-reload
    sudo systemctl enable app >/dev/null 2>&1
    sudo systemctl restart app
  '

  # 부팅 대기 - 로컬 헬스체크 (8080은 ALB에만 열려 있어 SSH 경유로 확인)
  ok=""
  for i in $(seq 1 40); do
    if ssh "${SSH_OPTS[@]}" "ec2-user@$IP" 'curl -sf http://localhost:8080/actuator/health' >/dev/null 2>&1; then
      echo "    health OK"
      ok=1
      break
    fi
    sleep 3
  done
  if [ -z "$ok" ]; then
    echo "    health check 실패 - 로그 확인: ssh -i $PEM ec2-user@$IP 'journalctl -u app -n 50'"
    exit 1
  fi
done

echo ">>> 완료: http://$ALB_DNS"
