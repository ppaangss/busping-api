output "alb_dns_name" {
  description = "테스트 진입점 - 여기로 요청을 쏜다"
  value       = aws_lb.main.dns_name
}

output "app_public_ips" {
  description = "앱 서버 공인 IP (SSH·배포용)"
  value       = aws_instance.app[*].public_ip
}

output "app_private_ips" {
  value = aws_instance.app[*].private_ip
}

output "redis_private_ip" {
  description = "앱 설정 REDIS_HOST에 넣을 값"
  value       = aws_instance.redis.private_ip
}

output "redis_public_ip" {
  description = "SSH용 (장애 테스트 때 redis 죽이러 들어갈 문)"
  value       = aws_instance.redis.public_ip
}

output "loadgen_public_ip" {
  value = aws_instance.loadgen.public_ip
}

output "mock_private_ip" {
  description = "앱 설정 TAGO_ARRIVAL_BASE_URL에 넣을 주소 (http://<ip>:8081)"
  value       = aws_instance.mock.private_ip
}

output "mock_public_ip" {
  description = "매핑 배포·SSH용"
  value       = aws_instance.mock.public_ip
}

output "rds_endpoint" {
  description = "앱 설정 DB_URL에 넣을 값"
  value       = aws_db_instance.main.address
}

output "db_password" {
  value     = random_password.db.result
  sensitive = true
}

output "ssh_key_path" {
  value = local_file.ssh_pem.filename
}
