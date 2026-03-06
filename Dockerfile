FROM ubuntu:latest
LABEL authors="ppaan"

ENTRYPOINT ["top", "-b"]