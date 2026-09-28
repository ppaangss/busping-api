# 공용 AWS 계정 - AWS에 올라가는 모든 명칭은 이 프리픽스만 사용
variable "name_prefix" {
  description = "모든 AWS 리소스 이름의 프리픽스"
  type        = string
  default     = "ppaangss-test"
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}
