package com.melody.melody_stream.modules.processmusic;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessMusicContext {
    private String jobId;
    private String songId;
    private String userId;
    private String objectKey; // file in Minio
    private String localFilePath;
    private String hlsOutputDir;
    private String masterPlaylistKey;
}
