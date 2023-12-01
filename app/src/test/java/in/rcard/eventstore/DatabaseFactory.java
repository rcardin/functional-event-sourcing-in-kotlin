package in.rcard.eventstore;

import java.util.Optional;

public class DatabaseFactory {
    public static Database spawn() {
        String version = Optional.ofNullable(System.getenv("EVENTSTORE_DOCKER_TAG_ENV")).orElse("latest");
        boolean secure = Boolean.parseBoolean(Optional.ofNullable(System.getenv("SECURE")).orElse("false"));

        return DockerContainerDatabase
                .builder()
                .version(version)
                .secure(secure)
                .build();
    }

    public static Database spawnPopulatedDatabase() {
        String version = Optional.ofNullable(System.getenv("EVENTSTORE_DOCKER_TAG_ENV")).orElse("latest");

        return DockerContainerDatabase
                .builder()
                .grpcTestDataImage()
                .version(version)
                .build();
    }
}
