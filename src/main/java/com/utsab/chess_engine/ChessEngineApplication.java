package com.utsab.chess_engine;

import com.utsab.chess_engine.config.RSAKeyRecord;
import com.utsab.chess_engine.core.UChessEngineUCI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(RSAKeyRecord.class)
@EnableAsync
@EnableScheduling
public class ChessEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChessEngineApplication.class, args);
        UChessEngineUCI.UCI();
	}

}
