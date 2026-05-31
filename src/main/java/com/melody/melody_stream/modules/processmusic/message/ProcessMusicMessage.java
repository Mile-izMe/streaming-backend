package com.melody.melody_stream.modules.processmusic.message;

public record ProcessMusicMessage(
     String jobId,
     String songId,
     String userId,
     Integer attempt
) {}
