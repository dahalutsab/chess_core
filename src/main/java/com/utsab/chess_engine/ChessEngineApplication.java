package com.utsab.chess_engine;

import com.utsab.chess_engine.config.RSAKeyRecord;
import com.utsab.chess_engine.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(RSAKeyRecord.class)
@EnableAsync
public class ChessEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChessEngineApplication.class, args);
        Core.UCI();
	}

}
