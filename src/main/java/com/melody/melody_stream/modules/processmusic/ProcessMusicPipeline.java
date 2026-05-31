package com.melody.melody_stream.modules.processmusic;

import com.melody.melody_stream.modules.processmusic.types.ProcessMusicStep;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProcessMusicPipeline {

    private final Map<Integer, ProcessMusicStep> stepMap;

    public ProcessMusicPipeline(List<ProcessMusicStep> steps) {
        this.stepMap = steps.stream()
                .collect(Collectors.toMap(ProcessMusicStep::stepIndex, s -> s));
    }

    public ProcessMusicStep stepByIndex(int index) {
        ProcessMusicStep step = stepMap.get(index);
        if (step == null) throw new IllegalStateException("No step for index=" +index);
        return step;
    }
}
