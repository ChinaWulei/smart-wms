FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/smart-wms-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect -Dspring.jpa.properties.jakarta.persistence.jdbc.url=jdbc:postgresql://postgres:5432/smart_wms -Dspring.datasource.url=jdbc:postgresql://postgres:5432/smart_wms -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres"
ENTRYPOINT ["java", "-jar", "app.jar"]
