package com.melody.melody_stream.modules.processmusic.types;

import com.melody.melody_stream.modules.processmusic.ProcessMusicContext;

public interface ProcessMusicStep {
    int stepIndex();
    String stepName();
    void process(ProcessMusicContext context);
}
