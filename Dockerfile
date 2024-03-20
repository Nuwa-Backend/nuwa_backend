# base-image
FROM openjdk:17-alpine
# 변수 설정 (빌드 파일 경로)
ARG JAR_FILE=build/libs/nuwa-backend-0.0.1-SNAPSHOT.jar
# 환경 변수 설정
ENV MARIADB_URL=${MARIADB_URL} \
MARIADB_USERNAME=${MARIADB_USERNAME} \
MARIADB_PASSWORD=${MARIADB_PASSWORD} \
MARIADB_NAME=${MARIADB_NAME} \
REDIS_USERNAME=${REDIS_USERNAME} \
REDIS_PORT=${REDIS_PORT} \
REDIS_PASSWORD=${REDIS_PASSWORD} \
MONGODB_IP=${MONGODB_IP} \
MONGODB_PORT=${MONGODB_PORT} \
MONGODB_NAME=${MONGODB_NAME} \
MONGODB_USERNAME=${MONGODB_USERNAME} \
MONGODB_PASSWORD=${MONGODB_PASSWORD} \
SENTRY_KEY=${SENTRY_KEY} \
JWT_SECRET=${JWT_SECRET} \
GOOGLE_ID=${GOOGLE_ID} \
GOOGLE_SECRET=${GOOGLE_SECRET} \
GOOGLE_URI=${GOOGLE_URI} \
NAVER_ID=${NAVER_ID} \
NAVER_SECRET=${NAVER_SECRET} \
NAVER_URI=${NAVER_URI} \
KAKAO_ID=${KAKAO_ID} \
KAKAO_URI=${KAKAO_URI} \
AWS_ACCESS_KEY=${AWS_ACCESS_KEY} \
AWS_SECRET_KEY=${AWS_SECRET_KEY} \
AWS_BUCKET=${AWS_BUCKET} \
GMAIL_ADDRESS=${GMAIL_ADDRESS} \
GMAIL_PASSWORD=${GMAIL_PASSWORD} \
ADMIN_URL=${ADMIN_URL} \
ADMIN_USERNAME=${ADMIN_USERNAME} \
ADMIN_PASSWORD=${ADMIN_PASSWORD} \
SERVICE_URL=${SERVICE_URL}
# 빌드 파일 컨테이너로 복사
COPY ${JAR_FILE} nuwa.jar
# jar 파일 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "/nuwa.jar"]