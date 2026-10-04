#!/usr/bin/env bash
# 앱 배포: 로컬 bootJar 빌드 -> 앱 서버 1대에 scp -> systemd 재시작
# 사용: ./deploy.sh   (terraform apply 완료 상태에서)
set -euo pipefail
cd "$(dirname "$0")"

echo ">>> terraform output 읽기"
APP_IP=$(terraform output -raw app_public_ip)
REDIS_IP=$(terraform output -raw redis_private_ip)
DB_IP=$(terraform output -raw db_private_ip)
DB_PW=$(terraform output -raw db_password)
PEM=$(terraform output -raw ssh_key_path)
MOCK_PRIV=$(terraform output -raw mock_private_ip)
MOCK_PUB=$(terraform output -raw mock_public_ip)

echo ">>> jar 빌드"
(cd ../.. && ./gradlew bootJar -q)
JAR=$(ls -t ../../build/libs/*.jar | grep -v plain | head -1)
echo "    $JAR"

SSH_OPTS=(-i "$PEM" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 -o LogLevel=ERROR)

# ---- mock TAGO: extension 빌드 + 매핑·jar 배포 + 재시작 ----
# 부팅 시엔 jar가 없어 서비스가 enable만 된 상태 - 여기서 처음 뜬다
echo ">>> mock TAGO extension 빌드"
(cd ../../fake/extension && ./build.sh)

echo ">>> mock TAGO 매핑·extension 배포: $MOCK_PUB"
scp "${SSH_OPTS[@]}" ../../fake/tago/mappings/*.json "ec2-user@$MOCK_PUB:/home/ec2-user/mock/mappings/"
scp "${SSH_OPTS[@]}" ../../fake/extension/tago-world.jar "ec2-user@$MOCK_PUB:/home/ec2-user/mock/"
ssh "${SSH_OPTS[@]}" "ec2-user@$MOCK_PUB" "sudo systemctl restart mock-tago"

mock_ok=""
for i in $(seq 1 20); do
  if ssh "${SSH_OPTS[@]}" "ec2-user@$MOCK_PUB" \
    'curl -sf "http://localhost:8081/getSttnAcctoArvlPrearngeInfoList?_type=json"' >/dev/null 2>&1; then
    echo "    mock TAGO OK"
    mock_ok=1
    break
  fi
  sleep 3
done
if [ -z "$mock_ok" ]; then
  echo "    mock TAGO 응답 없음 - 로그: ssh -i $PEM ec2-user@$MOCK_PUB 'journalctl -u mock-tago -n 50'"
  exit 1
fi

echo ">>> 배포: $APP_IP"

scp "${SSH_OPTS[@]}" "$JAR" "ec2-user@$APP_IP:/home/ec2-user/app.jar.new"

# 환경변수 파일 (DB 비밀번호 포함 - 600)
ssh "${SSH_OPTS[@]}" "ec2-user@$APP_IP" "sudo tee /etc/app.env > /dev/null && sudo chmod 600 /etc/app.env" <<ENV
SPRING_PROFILES_ACTIVE=loadtest
DB_URL=jdbc:mysql://$DB_IP:3306/ppaangss_test?serverTimezone=Asia/Seoul
DB_USERNAME=ppaangss
DB_PASSWORD=$DB_PW
REDIS_HOST=$REDIS_IP
TAGO_ARRIVAL_BASE_URL=http://$MOCK_PRIV:8081
ENV

ssh "${SSH_OPTS[@]}" "ec2-user@$APP_IP" "sudo tee /etc/systemd/system/app.service > /dev/null" <<'UNIT'
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

ssh "${SSH_OPTS[@]}" "ec2-user@$APP_IP" '
  mv /home/ec2-user/app.jar.new /home/ec2-user/app.jar
  sudo systemctl daemon-reload
  sudo systemctl enable app >/dev/null 2>&1
  sudo systemctl restart app
'

# 부팅 대기 - 첫 apply 직후엔 도커 mysql이 아직 이미지 받는 중일 수 있다
# 앱은 DB 연결 실패 시 systemd(Restart=always)가 3초마다 재시도하므로 기다리면 스스로 붙는다
ok=""
for i in $(seq 1 40); do
  if ssh "${SSH_OPTS[@]}" "ec2-user@$APP_IP" 'curl -sf http://localhost:8080/actuator/health' >/dev/null 2>&1; then
    echo "    health OK"
    ok=1
    break
  fi
  sleep 3
done
if [ -z "$ok" ]; then
  echo "    health check 실패 - 로그 확인: ssh -i $PEM ec2-user@$APP_IP 'journalctl -u app -n 50'"
  exit 1
fi

echo ">>> 완료: http://$APP_IP:8080 (부하발생기에서는 프라이빗 IP 사용)"
