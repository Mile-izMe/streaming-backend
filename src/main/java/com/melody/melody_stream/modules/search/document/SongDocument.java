package com.melody.melody_stream.modules.search.document;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;

@Document(indexName = "songs")
@Setting(settingPath = "elastic/es-settings.json")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SongDocument {

    @Id
    private String id;

    @MultiField(
        // Standard (English & Vietnamese symptom)
        mainField = @Field(type = FieldType.Text, analyzer = "standard"),

        otherFields = {
           // Layer 2: Non Vietnamese symptom
           @InnerField(suffix = "vi", type = FieldType.Text, analyzer = "vi_analyzer"),

           // Layer 3: Use N-gram for Autocomplete
           // Note: searchAnalyzer must be vi_analyzer
           // for query of user not hash when searched.
           @InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "vi_analyzer")
        }

    )
    private String title;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {
               @InnerField(suffix = "vi", type = FieldType.Text, analyzer = "vi_analyzer"),
               @InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "vi_analyzer")
            }
    )
    private String artist;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Keyword)
    private String audioUrl;

    @Field(type = FieldType.Integer)
    private Integer duration;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDate createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDate updatedAt;
}
