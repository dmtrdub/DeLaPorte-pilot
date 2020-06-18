package my.dub.dlp_pilot;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("DeLaPorte is starting up!");
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        context.registerShutdownHook();
    }
}