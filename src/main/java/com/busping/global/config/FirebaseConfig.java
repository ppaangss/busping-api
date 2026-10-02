package com.busping.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    /**
     * 자격증명 파일이 없으면 FCM만 비활성 상태로 부팅한다 - 로컬 개발 환경 허용
     */
    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        File credentialsFile = new File(credentialsPath);
        // isFile - 경로가 없거나 디렉터리면 스킵 (도커 바인드 마운트가 없는 파일을 빈 디렉터리로 만드는 경우 방어)
        if (!credentialsFile.isFile()) {
            log.warn("[FCM] Firebase 자격증명 파일 없음 (path={}) - FCM 발송 비활성 상태로 부팅", credentialsPath);
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }
}
