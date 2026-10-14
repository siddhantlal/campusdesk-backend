package dev.campusdesk.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapRunner implements ApplicationRunner {
    private final BootstrapService service;
    private final boolean adminEnabled,demoEnabled;
    private final String name,email,password,demoPassword;
    public BootstrapRunner(BootstrapService service,@Value("${app.bootstrap.enabled}") boolean adminEnabled,
            @Value("${app.bootstrap.name}") String name,@Value("${app.bootstrap.email}") String email,@Value("${app.bootstrap.password}") String password,
            @Value("${app.demo.enabled}") boolean demoEnabled,@Value("${app.demo.password}") String demoPassword) {
        this.service=service; this.adminEnabled=adminEnabled; this.name=name; this.email=email; this.password=password;
        this.demoEnabled=demoEnabled; this.demoPassword=demoPassword;
    }
    @Override public void run(ApplicationArguments args) {
        if (adminEnabled) service.admin(name,email,password);
        if (demoEnabled) service.demo(demoPassword);
    }
}
