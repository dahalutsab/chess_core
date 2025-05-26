package com.utsab.chess_engine.config.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileConfig {
    @Value("${app.filepath.windows}")
    private String windowsFilePath;

    @Value("${app.filepath.linux}")
    private String linuxFilePath;

    @Value("${app.filepath.mac}")
    private String macFilePath;

    @Bean
    public String getFilePath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return windowsFilePath;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return linuxFilePath;
        } else if (os.contains("mac")) {
            return macFilePath;
        }
        return null;
    }
}
