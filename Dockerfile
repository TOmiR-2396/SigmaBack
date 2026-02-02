# Usa el JAR pre-compilado directamente (sin recompilar)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
