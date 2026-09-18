# onti-api 배포용 이미지.
#
# 2단계로 나누는 이유: 빌드에는 JDK와 Gradle이 필요하지만 실행에는 JRE만
# 필요하다. 최종 이미지에 빌드 도구를 남기지 않아야 이미지가 작고, 배포와
# 재시작이 빠르다.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성만 먼저 받아 레이어로 굳혀둔다 — src만 바뀐 커밋에서는 이 레이어를
# 그대로 재사용해 빌드가 훨씬 빨라진다.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
# 테스트는 CI/로컬에서 돌린다. 배포 빌드에서 다시 돌리면 DB가 필요한 테스트
# 때문에 실패할 수 있고, 배포 시간도 늘어난다.
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너에 준 메모리 안에서 힙을 잡게 한다. 이걸 안 주면 JVM이 호스트 전체
# 메모리를 기준으로 힙을 잡아 512MB급 인스턴스에서 OOM으로 죽는다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
