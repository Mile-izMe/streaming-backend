package com.melody.melody_stream.modules.processmusic.steps;

import com.melody.melody_stream.infrastructure.ffmpeg.service.FfmpegService;
import com.melody.melody_stream.modules.processmusic.ProcessMusicContext;
import com.melody.melody_stream.modules.processmusic.types.ProcessMusicStep;
import com.melody.melody_stream.modules.song.entity.Song;
import com.melody.melody_stream.modules.song.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeStep implements ProcessMusicStep  {

    private final FfmpegService ffmpegService;
    private final SongRepository songRepository;

    @Override
    public int stepIndex() {
        return 1;
    }

    @Override
    public String stepName() {
        return "ANALYZE";
    }

    @Override
    @Transactional
    public void process(ProcessMusicContext context) {
        String localTempPath = context.getLocalFilePath();

        if (localTempPath == null || localTempPath.trim().isEmpty()) {
            throw new IllegalArgumentException("localTempPath is required for analysis");
        }

        try {
            Double duration = ffmpegService.getAudioDuration(localTempPath);

            if (duration == null || duration <= 0) {
                throw new RuntimeException("Could not extract duration from file");
            }

            String songId = context.getSongId();

            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new RuntimeException("Song not found with ID: " + songId));

            song.setDuration((int) Math.round(duration));
            songRepository.save(song);

            log.info("Analyze step completed successfully for song ID: {}", songId);
        } catch (Exception error) {
            log.error("Analyze step failed", error);
            throw new RuntimeException("Analyze step failed: " + error.getMessage(), error);
        }
    }
}
