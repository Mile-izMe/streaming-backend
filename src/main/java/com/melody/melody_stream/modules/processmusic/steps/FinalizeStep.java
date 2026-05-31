package com.melody.melody_stream.modules.processmusic.steps;

import com.melody.melody_stream.modules.processmusic.ProcessMusicContext;
import com.melody.melody_stream.modules.processmusic.types.ProcessMusicStep;
import com.melody.melody_stream.modules.song.entity.Song;
import com.melody.melody_stream.modules.song.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinalizeStep implements ProcessMusicStep {

    private final SongRepository songRepository;

    @Override
    public int stepIndex() {
        return 4;
    }

    @Override
    public String stepName() {
        return "FINALIZE";
    }

    @Override
    @Transactional
    public void process(ProcessMusicContext context) {
        String songId = context.getSongId();
        String outputDir = context.getHlsOutputDir();

        try {
            log.info("Starting finalize step for Song ID: {}", songId);

            // 1. Create HLS path point to file master playlist
            String hlsUrl = String.format("processed/songs/%d/master.m3u8", songId);

            // 2. Update record Song in database with new HLS URL
            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new RuntimeException("Song not found with ID: " + songId));

            song.setAudioUrl(hlsUrl);
            songRepository.save(song);

            log.info("Database updated successfully. New Audio URL: {}", hlsUrl);

            // 4. CLEANUP
            // Delete temporary files in Disk to avoid full memory in Server
            if (outputDir != null && !outputDir.trim().isEmpty()) {
                Path outputDirPath = Paths.get(outputDir);
                FileSystemUtils.deleteRecursively(outputDirPath);
                log.info("Cleaned up temporary directory: {}", outputDirPath);
            }

            // If save localTempPath (file download initial), delete it also
            String localTempPath = context.getLocalFilePath();
            if (localTempPath != null) {
                FileSystemUtils.deleteRecursively(Paths.get(localTempPath).getParent());
            }

            log.info("Finalize step completed successfully for Song ID: {}", songId);

        } catch (Exception error) {
            log.error("Finalize step failed for Song ID: {}", songId, error);
            throw new RuntimeException("Finalize step failed: " + error.getMessage(), error);
        }
    }
}
