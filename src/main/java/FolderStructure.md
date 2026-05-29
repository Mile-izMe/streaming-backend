src/main/java/com/melody/melody_stream/
│
├── MelodyStreamApplication.java
│
├── config/                 // Cấu hình Global
│   ├── MinioConfig.java
│   ├── RabbitConfig.java
│   ├── SwaggerConfig.java
│   ├── SecurityConfig.java
│   └── JacksonConfig.java
│
├── core/                   // Các thành phần dùng chung toàn dự án
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   └── NotFoundException.java
│   └── util/
│       ├── JsonUtil.java
│       ├── FileUtil.java
│       └── DateTimeUtil.java
│
├── infrastructure/         // Tầng giao tiếp với external services (Adapter Out)
│   ├── minio/
│   │   └── MinioService.java
│   └── ffmpeg/
│       └── FfmpegService.java
│
└── modules/                // TẦNG NGHIỆP VỤ (Chia theo Feature/Domain)
│
├── auth/               // --- MODULE AUTH ---
│   ├── AuthController.java
│   └── ...
│
├── song/               // --- MODULE SONG ---
│   ├── dto/
│   │   ├── SongUploadRequest.java
│   │   └── SongResponse.java
│   ├── entity/
│   │   └── Song.java
│   ├── repository/
│   │   └── SongRepository.java
│   ├── service/
│   │   └── SongService.java
│   └── controller/
│       └── SongController.java
│
├── job/                // --- MODULE JOB ---
│   ├── entity/
│   │   └── Job.java
│   └── ... (repository, service, controller tương tự)
│
└── processmusic/       // --- MODULE BACKGROUND WORKER ---
├── ProcessMusicOrchestrator.java
├── ProcessMusicPublisher.java
├── ProcessMusicListener.java
├── message/
│   └── ProcessMusicMessage.java
└── step/
├── DownloadStep.java
├── TranscodeStep.java
├── UploadHlsStep.java
└── FinalizeStep.java
