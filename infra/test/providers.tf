provider "aws" {
  region  = "ap-northeast-2"
  profile = "myisb"

  # 공용 계정 - 모든 리소스에 공통 태그 (개인 프로젝트명 노출 금지)
  default_tags {
    tags = {
      project    = "ppaangss-test"
      owner      = "ppaangss"
      managed_by = "terraform"
    }
  }
}
