terraform {
  required_version = ">= 1.15"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    # apply 시점의 내 공인 IP 조회용 (SSH·ALB 접근 제한)
    http = {
      source  = "hashicorp/http"
      version = "~> 3.4"
    }
    # SSH 키페어 생성·로컬 저장, DB 비밀번호 생성
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}
